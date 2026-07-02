/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.commerce.mcp.internal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.day.cq.wcm.api.Page;

public class StoreContext implements McpCallContext {
    private final Resource resource;
    private final SlingHttpServletRequest request;
    private final Page landingPage;
    private final Page productPage;
    private final MagentoGraphqlClient client;

    public StoreContext(Resource resource, SlingHttpServletRequest request, Page landingPage, Page productPage,
                        MagentoGraphqlClient client) {
        this.resource = resource;
        this.request = request;
        this.landingPage = landingPage;
        this.productPage = productPage;
        this.client = client;
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    @Override
    public Page getLandingPage() {
        return landingPage;
    }

    @Override
    public Page getProductPage() {
        return productPage;
    }

    public MagentoGraphqlClient getClient() {
        return client;
    }
}
