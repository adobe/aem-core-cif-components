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
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
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

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductTeaser.class, resourceType = ProductTeaserImpl.RESOURCE_TYPE)
public class ProductTeaserImpl implements ProductTeaser {

    protected static final String RESOURCE_TYPE = "venia/components/commerce/productteaser/v1/productteaser";
    private static final Logger LOG = LoggerFactory.getLogger(ProductTeaserImpl.class);
    private static final String PRODUCT_PATH_PROP = "productPath";
    private static final String PRODUCT_IMAGE_FOLDER = "catalog/product";
    private String mediaBaseUrl;
    @Inject
    private Resource resource;
    @Inject
    private Page currentPage;
    @ScriptVariable
    private ValueMap properties;
    private ProductInterface product;
    private NumberFormat priceFormatter;
    private Page productPage;

    @PostConstruct
    private void initModel() {
        productPage = Utils.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        String productPath = properties.get(PRODUCT_PATH_PROP, String.class);
        if (productPath != null && !productPath.isEmpty()) {
            String sku = getSkuFromUrl(productPath);
            GraphqlClient client = resource.adaptTo(GraphqlClient.class);
            if (client == null) {
                LOG.error("Cannot get a GraphqlClient using the resource at {}", resource.getPath());
            }
            Locale locale = currentPage.getLanguage(false);
            this.product = getProduct(client, sku);
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
        return (this.product != null ?  Utils.constructUrlfromSlug(productPage.getPath(),product.getUrlKey()) : null);
    }

    @Override
    public String getImage() {

        if (this.product != null) {
            return this.product.getImage().getUrl();
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
       return StringUtils.substringAfterLast(url,"/");
    }

    private ProductInterface getProduct(GraphqlClient client, String sku) {
        FilterTypeInput input = new FilterTypeInput().setEq(sku);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
        ProductsQueryDefinition queryArgs = q -> q.items(this.generateProductQuery());
        String queryString = Operations.query(query -> query
                .products(searchArgs, queryArgs)
                .storeConfig(this.generateStoreConfigQuery())).toString();
        GraphqlResponse<Query, Error> response = client.execute(new GraphqlRequest(queryString), Query.class, Error.class, QueryDeserializer.getGson());
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();
        mediaBaseUrl = rootQuery.getStoreConfig().getSecureBaseMediaUrl();
        if (products.size() > 0) {
            return products.get(0);
        }
        return null;
    }

    private ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
                .regularPrice( rp -> rp
                        .amount( a -> a.currency().value()));
    }

    private ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q
                .name()
                .image( i -> i.url())
                .urlKey()
                .price(this.generatePriceQuery());
    }
    private StoreConfigQueryDefinition generateStoreConfigQuery() {
        return q -> q.secureBaseMediaUrl();
    }
}