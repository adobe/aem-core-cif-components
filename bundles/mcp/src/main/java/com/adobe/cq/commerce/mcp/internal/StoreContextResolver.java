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
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(service = StoreContextResolver.class)
public class StoreContextResolver {

    private Page currentPage(SlingHttpServletRequest request) {
        Resource r = request.getResource();
        PageManager pm = r.getResourceResolver().adaptTo(PageManager.class);
        return pm == null ? null : pm.getContainingPage(r);
    }

    public boolean isNavRoot(SlingHttpServletRequest request) {
        Page page = currentPage(request);
        if (page == null) {
            return false;
        }
        SiteStructure ss = page.adaptTo(SiteStructure.class);
        if (ss == null) {
            return false;
        }
        Page landing = ss.getLandingPage();
        return landing != null && landing.getPath().equals(page.getPath());
    }

    public StoreContext resolve(SlingHttpServletRequest request) {
        Page landing = currentPage(request);
        SiteStructure ss = landing == null ? null : landing.adaptTo(SiteStructure.class);
        Page productPage = landing;
        if (ss != null && ss.getSearchResultsPage() != null && ss.getSearchResultsPage().getPage() != null) {
            productPage = ss.getSearchResultsPage().getPage();
        }
        MagentoGraphqlClient client = request.adaptTo(MagentoGraphqlClient.class);
        return new StoreContext(request.getResource(), request, landing, productPage, client);
    }
}
