/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe Systems Incorporated
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

package com.adobe.cq.commerce.core.components.internal.models.v1.productteaser;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.productteaser.Productteaser;
import com.adobe.cq.commerce.core.components.service.ProductGraphqlService;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Model(adaptables = SlingHttpServletRequest.class, adapters = Productteaser.class, resourceType = ProductteaserImpl.RESOURCE_TYPE)
public class ProductteaserImpl implements Productteaser {

    protected static final String RESOURCE_TYPE = "venia/components/commerce/product/v1/Productteaser";
    private static final Logger LOG = LoggerFactory.getLogger(ProductteaserImpl.class);
    private static final String PRODUCT_PATH_PROP = "productSku";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    private ProductInterface product;


    private NumberFormat priceFormatter;


    @Inject
    private ProductGraphqlService productGraphqlService;


    @ScriptVariable
    private ValueMap properties;

    private Page productPage;

    @PostConstruct
    private void initModel() {
        String sku = null;
        String productPath = null;
        productPage = Utils.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        productPath = properties.get(PRODUCT_PATH_PROP, String.class);
        if (productPath != null && !productPath.isEmpty()) {
            sku = getSkuFromUrl(productPath);
            GraphqlClient client = resource.adaptTo(GraphqlClient.class);
            if (client == null) {
                LOG.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
            }
            Locale locale = currentPage.getLanguage(false);
            this.product = productGraphqlService.getProduct(client, sku);
            this.priceFormatter = Utils.buildPriceFormatter(locale, this.getCurrency());
        }
    }

    @Override
    public String getName() {
        return (this.product != null ? this.product.getName() : null);

    }

    @Override
    public String getFormattedPrice() {

        Double price = this.getPrice();

        if (price != null) {
            return this.priceFormatter.format(price);
        }
        return null;
    }

    @Override
    public String getUrl() {
        return (this.product != null ? String.format("%s.%s.html", productPage.getPath(), product.getUrlKey()) : null);
    }

    @Override
    public List<Asset> getAssets() {
        if (this.product != null) {
            return productGraphqlService.filterAndSortAssets(this.product.getMediaGalleryEntries());
        }
        return null;
    }

    private String getCurrency() {
        if (this.product != null) {
            return this.product.getPrice().getRegularPrice().getAmount().getCurrency().toString();
        }
        return null;
    }

    private Double getPrice() {
        if (this.product != null) {
            return this.product.getPrice().getRegularPrice().getAmount().getValue();
        }
        return null;
    }

    // Product DnD from content finder  provides only path of the product to be replaced in
    private String getSkuFromUrl(String url) {
        if (url != null && !url.isEmpty() && url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1, url.length());
        }
        return null;
    }


}