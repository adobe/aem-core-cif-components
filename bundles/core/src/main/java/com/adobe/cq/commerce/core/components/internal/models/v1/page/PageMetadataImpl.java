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

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.internal.models.v1.product.ProductImpl;
import com.adobe.cq.commerce.core.components.internal.models.v2.productlist.ProductListImpl;
import com.adobe.cq.commerce.core.components.models.page.PageMetadata;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = PageMetadata.class)
public class PageMetadataImpl implements PageMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageMetadataImpl.class);

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    protected Resource resource;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private ModelFactory modelFactory;

    private PageMetadata provider;

    @PostConstruct
    void initModel() {
        // We cannot directly adapt from the current request because this would wrongly inject the page resource
        // into the product or productlist component.
        // We hence use a dedicated method in modelFactory to inject the right component resource.

        if (SiteNavigation.isProductPage(currentPage)) {
            Resource componentResource = findChildResourceWithType(resource, ProductImpl.RESOURCE_TYPE);
            if (componentResource != null) {
                Product product = modelFactory.getModelFromWrappedRequest(request, componentResource, Product.class);
                provider = product;
            }
        } else if (SiteNavigation.isCategoryPage(currentPage)) {
            Resource componentResource = findChildResourceWithType(resource, ProductListImpl.RESOURCE_TYPE);
            if (componentResource == null) {
                componentResource = findChildResourceWithType(resource,
                    com.adobe.cq.commerce.core.components.internal.models.v1.productlist.ProductListImpl.RESOURCE_TYPE);
            }
            if (componentResource != null) {
                ProductList productList = modelFactory.getModelFromWrappedRequest(request, componentResource, ProductList.class);
                provider = productList;
            }
        }
    }

    private Resource findChildResourceWithType(Resource fromResource, String type) {
        LOGGER.debug("Looking for child resource type '{}' from {}", type, fromResource.getPath());
        for (Resource child : fromResource.getChildren()) {
            if (child.isResourceType(type)) {
                LOGGER.debug("Found child resource type '{}' at {}", type, child.getPath());
                return child;
            }
            Resource resource = findChildResourceWithType(child, type);
            if (resource != null) {
                return resource;
            }
        }
        return null;
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

}
