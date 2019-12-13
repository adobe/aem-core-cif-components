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

package com.adobe.cq.commerce.core.components.models.productteaser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.scripting.sightly.Record;

import com.adobe.cq.commerce.core.components.models.GraphqlModel;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductTeaserModel.class)
public class ProductTeaserModel extends GraphqlModel {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productteaser/v1/productteaser";
    private static final String SELECTION_PROPERTY = "selection";

    private String variantSku; // If not null, this holds the sku of the selected variant

    private Record product;
    private Record variant;

    @PostConstruct
    private void initModel() {
        String selection = properties.get(SELECTION_PROPERTY, String.class);
        if (selection == null || selection.isEmpty()) {
            return;
        }

        // The product DnD from content finder provides the product path
        if (selection.startsWith("/")) {
            selection = StringUtils.substringAfterLast(selection, "/");
        }

        Pair<String, String> skus = SiteNavigation.toProductSkus(selection);
        String sku = skus.getLeft();

        // Fetch product data
        Map<String, Object> values = new HashMap<>();
        values.put("selection", sku);
        Query rootQuery = executeQuery(values);
        if (rootQuery == null) {
            return;
        }

        List<ProductInterface> products = rootQuery.getProducts().getItems();
        if (products.isEmpty()) {
            return;
        }

        ProductInterface variant = null;
        ProductInterface baseProduct = products.get(0);
        // Check if the selected product is a variant
        if (skus.getRight() != null && baseProduct instanceof ConfigurableProduct) {
            ConfigurableProduct configurableProduct = (ConfigurableProduct) baseProduct;
            String selectedSku = skus.getRight();

            SimpleProduct var = null;
            List<ConfigurableVariant> variants = configurableProduct.getVariants();
            if (variants != null && !variants.isEmpty()) {
                var = variants.stream().map(v -> v.getProduct()).filter(sp -> selectedSku.equals(sp.getSku())).findFirst()
                    .orElse(null);
            }
            if (var != null) {
                variantSku = selectedSku;
                variant = var;
            }
        }

        this.product = graphqlRecordFactory.recordFrom(baseProduct);
        this.variant = variant == null ? this.product : graphqlRecordFactory.recordFrom(variant);
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
