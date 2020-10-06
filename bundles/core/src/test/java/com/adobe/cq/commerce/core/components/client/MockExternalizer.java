/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.client;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.day.cq.commons.Externalizer;

@Component(service = MockExternalizer.class)
public class MockExternalizer implements Externalizer {

    @Override
    public String externalLink(ResourceResolver resolver, String domain, String path) {
        return externalLink(resolver, domain, "https", path);
    }

    @Override
    public String externalLink(ResourceResolver resolver, String domain, String scheme, String path) {
        return scheme + "://" + domain + path;
    }

    @Override
    public String publishLink(ResourceResolver resolver, String path) {
        return externalLink(resolver, Externalizer.PUBLISH, path);
    }

    @Override
    public String publishLink(ResourceResolver resolver, String scheme, String path) {
        return externalLink(resolver, Externalizer.PUBLISH, scheme, path);
    }

    @Override
    public String authorLink(ResourceResolver resolver, String path) {
        return externalLink(resolver, Externalizer.AUTHOR, path);
    }

    @Override
    public String authorLink(ResourceResolver resolver, String scheme, String path) {
        return externalLink(resolver, Externalizer.AUTHOR, scheme, path);
    }

    @Override
    public String relativeLink(SlingHttpServletRequest request, String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String absoluteLink(SlingHttpServletRequest request, String scheme, String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String absoluteLink(ResourceResolver resolver, String scheme, String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String absoluteLink(String scheme, String path) {
        // TODO Auto-generated method stub
        return null;
    }

}
