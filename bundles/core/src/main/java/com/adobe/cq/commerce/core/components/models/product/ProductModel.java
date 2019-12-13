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

package com.adobe.cq.commerce.core.components.models.product;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.scripting.sightly.Record;

import com.adobe.cq.commerce.core.components.models.GraphqlModel;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductModel.class, resourceType = ProductModel.RESOURCE_TYPE)
public class ProductModel extends GraphqlModel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/product/v1/product";
    private static final String SELECTION_PROPERTY = "selection";

    private String variantSku; // If not null, this holds the sku of the selected variant

    private Record product;
    private Record variant;
    private String mediaBaseUrl;

    @Self
    private SlingHttpServletRequest request;

    @PostConstruct
    private void initModel() {
        // Parse slug from URL
        String slug = parseProductSlug();

        if (org.apache.commons.lang3.StringUtils.isNotBlank(slug)) {

            Map<String, Object> vars = new HashMap<>();
            vars.put("slug", slug);
            Query query = executeQuery(vars);
            List<ProductInterface> products = query.getProducts().getItems();

            // TODO WORKAROUND
            // we need a temporary detour and use storeconfig to get the base media url since the product media gallery only returns the
            // images
            // file names but no full URLs
            mediaBaseUrl = query.getStoreConfig().getSecureBaseMediaUrl();

            // Return first product in list
            if (products.size() > 0) {
                product = graphqlRecordFactory.recordFrom(products.get(0));
            }

            // loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

            // } else if (!wcmMode.isDisabled()) {
            // useEditModePlaceholderData();
            // loadClientPrice = false;
            // }

            // Initialize NumberFormatter with locale from current page.
            // Alternatively, the locale can potentially be retrieved via
            // the storeConfig query introduced with Magento 2.3.1
            Locale locale = currentPage.getLanguage(false);
            // priceFormatter = Utils.buildPriceFormatter(locale, product != null ? getCurrency() : null);
        }

        /*
         * 
         * String selection = properties.get(SELECTION_PROPERTY, String.class);
         * if (selection == null || selection.isEmpty()) {
         * return;
         * }
         * 
         * // The product DnD from content finder provides the product path
         * if (selection.startsWith("/")) {
         * selection = StringUtils.substringAfterLast(selection, "/");
         * }
         * 
         * Pair<String, String> skus = SiteNavigation.toProductSkus(selection);
         * String sku = skus.getLeft();
         * 
         * // Fetch product data
         * Query rootQuery = executeQuery(sku);
         * if (rootQuery == null) {
         * return;
         * }
         * 
         * List<ProductInterface> products = rootQuery.getProducts().getItems();
         * if (products.isEmpty()) {
         * return;
         * }
         * 
         * ProductInterface variant = null;
         * ProductInterface baseProduct = products.get(0);
         * // Check if the selected product is a variant
         * if (skus.getRight() != null && baseProduct instanceof ConfigurableProduct) {
         * ConfigurableProduct configurableProduct = (ConfigurableProduct) baseProduct;
         * String selectedSku = skus.getRight();
         * 
         * SimpleProduct var = null;
         * List<ConfigurableVariant> variants = configurableProduct.getVariants();
         * if (variants != null && !variants.isEmpty()) {
         * var = variants.stream().map(v -> v.getProduct()).filter(sp -> selectedSku.equals(sp.getSku())).findFirst()
         * .orElse(null);
         * }
         * if (var != null) {
         * variantSku = selectedSku;
         * variant = var;
         * }
         * }
         * 
         * this.product = new DataRecord(baseProduct);
         * this.variant = variant == null ? this.product : new DataRecord(variant);
         * 
         * setFormatter(new ProductURLFormatter());
         * 
         */
    }

    private String parseProductSlug() {
        return request.getRequestPathInfo().getSelectorString();
    }

    public Record getProduct() {
        return product;
    }

    public Record getVariant() {
        return variant;
    }

    @Override
    public String getVariantSku() {
        return variantSku;
    }
}
