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

package com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsRetriever.RelationType;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * A {@link DataSource} implementation that includes all the values for the {@link RelationType} enum.
 */
@Component(
    immediate = true,
    service = Servlet.class,
    property = { "sling.servlet.resourceTypes=core/cif/components/commerce/relatedproducts/relationtypes" })
public class RelationTypesDataSourceServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        ResourceBundle resourceBundle = request.getResourceBundle(null);
        List<Resource> values = new ArrayList<>();

        for (RelationType relationType : RelationType.values()) {
            ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
            vm.put("value", relationType);
            vm.put("text", toText(resourceBundle, relationType.getText()));
            values.add(new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), JcrConstants.NT_UNSTRUCTURED, vm));
        }

        DataSource ds = new SimpleDataSource(values.iterator());
        request.setAttribute(DataSource.class.getName(), ds);
    }

    private String toText(ResourceBundle resourceBundle, String text) {
        try {
            return resourceBundle.getString(text);
        } catch (MissingResourceException e) {
            return text;
        }
    }
}
