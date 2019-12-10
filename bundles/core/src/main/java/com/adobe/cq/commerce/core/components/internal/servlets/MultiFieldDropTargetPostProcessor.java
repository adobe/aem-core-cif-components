/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

/*******************************************************************************
 * 
 * Copyright (c) 2018 iDA MediaFoundry
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *******************************************************************************/

package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;

import com.google.common.collect.Iterators;

/**
 * This class is originally available at <code>https://github.com/ida-mediafoundry/jetpack-advanced-drop-targets</code><br>
 * <br>
 * This PostProcessor listens to all post servlet actions that contain a property that starts with <code>"./dropTarget->"</code>.
 * It will take the value from the property and add it to the property that follows the drop target prefix.<br>
 * <br>
 * In the following example, it will take the value from "./dropTarget->@books" and add it to the property "books"
 * 
 * <pre>
 * {@literal
 * <cq:dropTargets jcr:primaryType="nt:unstructured">
 *   <books
 *      jcr:primaryType="cq:DropTargetConfig"
 *      ...
 *      propertyName="./dropTarget->@books">
 *   </books>
 * </cq:dropTargets>
 * }
 * </pre>
 * 
 */
@Component(service = { SlingPostProcessor.class })
public class MultiFieldDropTargetPostProcessor implements SlingPostProcessor {

    private static final String DROP_TARGET_PREFIX = "./dropTarget->";
    private static final String COMPOSITE_VARIABLE = "{{COMPOSITE}}";
    private static final String PROPERTY_PREFIX = "@";
    private static final String SLASH = "/";
    private static final String SLING_PROPERTY_PREFIX = "./";

    @Override
    public void process(SlingHttpServletRequest request, List<Modification> modifications) throws Exception {
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();

        for (String key : requestParameterMap.keySet()) {
            if (key.startsWith(DROP_TARGET_PREFIX)) {

                RequestParameter requestParameter = requestParameterMap.getValue(key);
                if (requestParameter != null) {
                    String target = key.replace(DROP_TARGET_PREFIX, StringUtils.EMPTY);
                    String propertyValue = requestParameter.getString();
                    Resource resource = request.getResource();
                    processProperty(resource, target, propertyValue, key);
                    modifications.add(Modification.onModified(resource.getPath()));
                }
            }
        }
    }

    private void processProperty(Resource resource, String target, String propertyValue, String originalKey) throws Exception {
        String[] paths = target.split(SLASH);

        ResourceResolver resourceResolver = resource.getResourceResolver();

        // clean-up the dropTarget property or node
        ModifiableValueMap originalProperties = resource.adaptTo(ModifiableValueMap.class);
        originalProperties.remove(originalKey);

        String dropTargetNodeName = DROP_TARGET_PREFIX.replace(SLING_PROPERTY_PREFIX, StringUtils.EMPTY);
        Resource dropTargetResource = resource.getChild(dropTargetNodeName);
        if (dropTargetResource != null) {
            resourceResolver.delete(dropTargetResource);
        }

        // check all paths and create correct resources and properties
        boolean isArray = true;
        Resource currentResource = resource;
        for (String path : paths) {
            if (path.startsWith(PROPERTY_PREFIX)) {
                // this is the property
                String propertyName = path.replace(PROPERTY_PREFIX, StringUtils.EMPTY);
                ModifiableValueMap properties = currentResource.adaptTo(ModifiableValueMap.class);
                if (isArray) {
                    List<String> childPages = new ArrayList<>(Arrays.asList(properties.get(propertyName, new String[0])));
                    childPages.add(propertyValue);
                    properties.remove(propertyName);
                    properties.put(propertyName, childPages.toArray());
                } else {
                    properties.put(propertyName, propertyValue);
                }
            } else if (path.equals(COMPOSITE_VARIABLE)) {
                // create new subNode
                int count = Iterators.size(currentResource.getChildren().iterator());
                String nodeName = "item" + count;
                currentResource = resourceResolver.create(currentResource, nodeName, new HashMap<>());
                isArray = false;
            } else if (StringUtils.isNotBlank(path)) {
                // get or create new node
                Resource subResource = currentResource.getChild(path);
                if (subResource == null) {
                    currentResource = resourceResolver.create(currentResource, path, new HashMap<>());
                } else {
                    currentResource = subResource;
                }
            }
        }
    }
}
