/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v3.product;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.product.VariantValue;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class ProductImplTest extends com.adobe.cq.commerce.core.components.internal.models.v2.product.ProductImplTest {

    @Override
    protected void adaptToProduct() {
        // This ensures we re-run all the unit tests with version 3 of ProductImpl
        productModel = context.request().adaptTo(ProductImpl.class);
    }

    @Before
    public void updateGraphQlResponse() throws IOException {
        Utils.setupHttpResponse("graphql/magento-graphql-product-result-uid-variants.json", httpClient, 200,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result-uid-variants.json", httpClient, 200,
            "{products(filter:{sku");
    }

    @Test
    public void testUidVariants() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        adaptToProduct();
        List<Variant> variants = productModel.getVariants();
        assertNotNull(variants);
        String jsonVariants = productModel.getVariantsJson();
        String expectedJsonVariants = Utils.getResource("results/result-product-variants-uid.json");
        assertEquals(mapper.readTree(expectedJsonVariants), mapper.readTree(jsonVariants));
    }

    @Test
    public void testGetVariantAttributesUid() throws IOException {
        adaptToProduct();
        List<VariantAttribute> attributes = productModel.getVariantAttributes();

        assertNotNull(attributes);

        for (int i = 0; i < attributes.size(); i++) {
            VariantAttribute attribute = attributes.get(i);

            for (int j = 0; j < attribute.getValues().size(); j++) {
                VariantValue value = attribute.getValues().get(j);
                assertNotNull(value.getUid());
            }
        }
    }

    @Test
    public void testSwatchDataInVariantAttributes() throws IOException {
        Query rootQuery = Utils.getQueryFromResource("graphql/magento-graphql-configurableproduct-uid-result.json");
        product = rootQuery.getProducts().getItems().get(0);

        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-uid-result.json", httpClient, 200,
            "{products(filter:{url_key");
        Utils.setupHttpResponse("graphql/magento-graphql-configurableproduct-uid-result.json", httpClient, 200, "{products(filter:{sku");

        adaptToProduct();
        List<VariantAttribute> attributes = productModel.getVariantAttributes();
        assertNotNull(attributes);

        ConfigurableProduct cp = (ConfigurableProduct) product;
        assertEquals(cp.getConfigurableOptions().size(), attributes.size());

        for (int i = 0; i < attributes.size(); i++) {
            VariantAttribute attribute = attributes.get(i);
            ConfigurableProductOptions option = cp.getConfigurableOptions().get(i);

            assertEquals(option.getAttributeCode(), attribute.getId());
            assertEquals(option.getLabel(), attribute.getLabel());

            for (int j = 0; j < attribute.getValues().size(); j++) {
                VariantValue value = attribute.getValues().get(j);
                ConfigurableProductOptionsValues optionValue = option.getValues().get(j);
                assertEquals(optionValue.getUid().toString(), value.getUid());
                assertEquals(optionValue.getLabel(), value.getLabel());
                assertEquals(optionValue.getDefaultLabel().trim().replaceAll("\\s+", "-").toLowerCase(), value.getCssClassModifier());
                assertTrue("SwatchData type mismatch", optionValue.getSwatchData().getGraphQlTypeName().toUpperCase().startsWith(
                    value.getSwatchType().toString()));
            }
        }
    }

    @Test
    public void testVisibleSectionsWithStyle() {
        ProductImpl.SECTIONS_MAP.values().forEach(v -> {
            if (v.equals("showSku")) {
                when(style.get(eq(v), anyBoolean())).thenReturn(Boolean.TRUE);
            } else {
                when(style.get(eq(v), anyBoolean())).thenReturn(Boolean.FALSE);
            }
        });
        adaptToProduct();
        Set<String> expected = new HashSet<>();
        expected.add("SKU");
        assertEquals(expected, productModel.getVisibleSections());
    }
}
