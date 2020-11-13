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

package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

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

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    private PageMetadata provider;

    @PostConstruct
    void initModel() {
        if (isProductPage()) {
            Product product = request.adaptTo(Product.class);
            provider = product;
        } else if (isCategoryPage()) {
            ProductList productList = request.adaptTo(ProductList.class);
            provider = productList;
        }
    }

    @Override
    public String getMetaDescription() {
        String metaDescription = provider != null ? provider.getMetaDescription() : null;
        return metaDescription != null ? metaDescription : properties.get(JcrConstants.JCR_DESCRIPTION, String.class);
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

    private boolean isProductPage() {
        Page productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            return false;
        }

        // The product page might be in a Launch so we first extract the paths of the production versions
        String currentPagePath = currentPage.getPath().substring(currentPage.getPath().lastIndexOf("/content/"));
        String productPagePath = productPage.getPath().substring(productPage.getPath().lastIndexOf("/content/"));

        return currentPagePath.equals(productPagePath) || currentPagePath.startsWith(productPagePath + "/");
    }

    private boolean isCategoryPage() {
        Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
        if (categoryPage == null) {
            return false;
        }

        // The category page might be in a Launch so we first extract the paths of the production versions
        String currentPagePath = currentPage.getPath().substring(currentPage.getPath().lastIndexOf("/content/"));
        String categoryPagePath = categoryPage.getPath().substring(categoryPage.getPath().lastIndexOf("/content/"));

        return currentPagePath.equals(categoryPagePath) || currentPagePath.startsWith(categoryPagePath + "/");
    }

}
