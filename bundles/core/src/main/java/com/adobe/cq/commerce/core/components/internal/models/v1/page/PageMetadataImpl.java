/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = PageMetadata.class)
public class PageMetadataImpl implements PageMetadata {

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    protected Resource resource;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private CommerceComponentModelFinder componentFinder;

    @Self
    private SiteStructure siteStructure;

    private PageMetadata provider;

    @PostConstruct
    void initModel() {
        // We cannot directly adapt from the current request because this would wrongly inject the page resource
        // into the product or productlist component.
        // We hence use a dedicated method in modelFactory to inject the right component resource.

        if (siteStructure.isProductPage(currentPage)) {
            provider = componentFinder.findProductComponentModel(request);
        } else if (siteStructure.isCategoryPage(currentPage)) {
            provider = componentFinder.findProductListComponentModel(request);
        }
    }

    @Override
    public String getMetaDescription() {
        String metaDescription = provider != null ? provider.getMetaDescription() : null;
        return metaDescription != null ? metaDescription : resource.getValueMap().get(JcrConstants.JCR_DESCRIPTION, String.class);
    }

    @Override
    public String getMetaKeywords() {
        String metaKeywords = provider != null ? provider.getMetaKeywords() : null;
        if (metaKeywords == null && currentPage instanceof com.adobe.cq.wcm.core.components.models.Page) {
            metaKeywords = ((com.adobe.cq.wcm.core.components.models.Page) currentPage).getKeywords().toString();
        }
        return metaKeywords;
    }

    @Override
    public String getMetaTitle() {
        String metaTitle = provider != null ? provider.getMetaTitle() : null;
        return metaTitle != null ? metaTitle : currentPage.getTitle();
    }

    @Override
    public String getCanonicalUrl() {
        return provider != null ? provider.getCanonicalUrl() : null;
    }

    @Override
    public Map<Locale, String> getAlternateLanguageLinks() {
        return provider != null ? provider.getAlternateLanguageLinks() : null;
    }
}
