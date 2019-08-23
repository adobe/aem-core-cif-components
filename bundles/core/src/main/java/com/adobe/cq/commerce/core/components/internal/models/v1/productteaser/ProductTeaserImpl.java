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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductTeaser.class, resourceType = ProductTeaserImpl.RESOURCE_TYPE)
public class ProductTeaserImpl implements ProductTeaser {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productteaser/v1/productteaser";
    private static final String SELECTION_PROPERTY = "selection";

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    @ScriptVariable
    private ValueMap properties;

    private ProductInterface product; // Can be the base product or one of its variant
    private String baseProductSlug; // This is always ths lug of the base product, to build the product page url
    private String variantSku; // If not null, this holds the sku of the selected variant

    private NumberFormat priceFormatter;
    private Page productPage;
    private MagentoGraphqlClient magentoGraphqlClient;

    @PostConstruct
    protected void initModel() {
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
        String combinedSku = properties.get(SELECTION_PROPERTY, String.class);
        if (combinedSku != null && !combinedSku.isEmpty()) {
            // Get MagentoGraphqlClient from the resource.
            magentoGraphqlClient = MagentoGraphqlClient.create(resource);

            // Fetch product data
            if (magentoGraphqlClient != null) {
                product = fetchProduct(combinedSku);
            }

            Locale locale = currentPage.getLanguage(false);
            priceFormatter = Utils.buildPriceFormatter(locale, getCurrency());
        }
    }

    @Override
    public String getName() {
        return product != null ? product.getName() : null;
    }

    @Override
    public String getFormattedPrice() {
        Double price = getPrice();
        if (price != null) {
            return priceFormatter.format(price);
        }
        return null;
    }

    @Override
    public String getUrl() {
        if (product != null) {
            return SiteNavigation.toProductUrl(productPage.getPath(), baseProductSlug, variantSku);
        }
        return null;
    }

    @Override
    public String getImage() {
        if (product != null) {
            return product.getImage().getUrl();
        }
        return null;
    }

    private String getCurrency() {
        if (product != null) {
            return product.getPrice().getRegularPrice().getAmount().getCurrency().toString();
        }
        return null;
    }

    private Double getPrice() {
        if (product != null) {
            return product.getPrice().getRegularPrice().getAmount().getValue();
        }
        return null;
    }

    private ProductInterface fetchProduct(String combinedSku) {
        Pair<String, String> skus = SiteNavigation.toProductSkus(combinedSku);
        String sku = skus.getLeft();

        FilterTypeInput input = new FilterTypeInput().setEq(sku);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
        QueryQuery.ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);
        ProductsQueryDefinition queryArgs = q -> q.items(generateProductQuery());

        String queryString = Operations.query(query -> query.products(searchArgs, queryArgs)).toString();

        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);
        Query rootQuery = response.getData();
        List<ProductInterface> products = rootQuery.getProducts().getItems();
        if (products.size() > 0) {
            ProductInterface baseProduct = products.get(0);
            baseProductSlug = baseProduct.getUrlKey();

            // Check if the selected product is a variant
            if (skus.getRight() != null && baseProduct instanceof ConfigurableProduct) {
                ConfigurableProduct configurableProduct = (ConfigurableProduct) baseProduct;
                String selectedSku = skus.getRight();
                SimpleProduct variant = findVariant(configurableProduct, selectedSku);
                if (variant != null) {
                    variantSku = selectedSku;
                    return variant;
                }
            }

            return baseProduct;
        }
        return null;
    }

    private SimpleProduct findVariant(ConfigurableProduct configurableProduct, String variantSku) {
        List<ConfigurableVariant> variants = configurableProduct.getVariants();
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream().map(v -> v.getProduct()).filter(sp -> variantSku.equals(sp.getSku())).findFirst().orElse(null);
    }

    private ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
            .regularPrice(rp -> rp
                .amount(a -> a.currency().value()));
    }

    private ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q
            .name()
            .image(i -> i.url())
            .urlKey()
            .price(generatePriceQuery())
            .onConfigurableProduct(cp -> cp
                .variants(v -> v
                    .product(generateSimpleProductQuery())));
    }

    private SimpleProductQueryDefinition generateSimpleProductQuery() {
        return q -> q
            .sku()
            .name()
            .image(i -> i.url())
            .price(generatePriceQuery());
    }

}