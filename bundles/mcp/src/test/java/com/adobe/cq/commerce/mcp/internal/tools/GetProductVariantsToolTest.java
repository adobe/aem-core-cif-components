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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Collections;

import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ConfigurableAttributeOption;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductImage;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductPrice;
import com.adobe.cq.commerce.magento.graphql.ProductStockStatus;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetProductVariantsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsConfigurableVariants() throws Exception {
        SimpleProduct variantProduct = mock(SimpleProduct.class);
        when(variantProduct.getSku()).thenReturn("VD09-LA-L");
        when(variantProduct.getName()).thenReturn("Alexia Maxi Dress-L");
        when(variantProduct.getStockStatus()).thenReturn(ProductStockStatus.IN_STOCK);
        ProductImage image = mock(ProductImage.class);
        when(image.getUrl()).thenReturn("https://example.com/vd09.jpg");
        when(image.getLabel()).thenReturn("VD09");
        when(variantProduct.getImage()).thenReturn(image);
        Money money = mock(Money.class);
        when(money.getValue()).thenReturn(98.0);
        when(money.getCurrency()).thenReturn(CurrencyEnum.USD);
        ProductPrice productPrice = mock(ProductPrice.class);
        when(productPrice.getFinalPrice()).thenReturn(money);
        PriceRange priceRange = mock(PriceRange.class);
        when(priceRange.getMinimumPrice()).thenReturn(productPrice);
        when(variantProduct.getPriceRange()).thenReturn(priceRange);

        ConfigurableAttributeOption attribute = mock(ConfigurableAttributeOption.class);
        when(attribute.getCode()).thenReturn("size");
        when(attribute.getValueIndex()).thenReturn(179);

        ConfigurableVariant variant = mock(ConfigurableVariant.class);
        when(variant.getProduct()).thenReturn(variantProduct);
        when(variant.getAttributes()).thenReturn(Collections.singletonList(attribute));

        ConfigurableProductOptionsValues optionValue = mock(ConfigurableProductOptionsValues.class);
        when(optionValue.getValueIndex()).thenReturn(179);
        when(optionValue.getLabel()).thenReturn("L");

        ConfigurableProductOptions option = mock(ConfigurableProductOptions.class);
        when(option.getAttributeCode()).thenReturn("size");
        when(option.getLabel()).thenReturn("Size");
        when(option.getValues()).thenReturn(Collections.singletonList(optionValue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getSku()).thenReturn("VD09");
        when(product.getName()).thenReturn("Alexia Maxi Dress");
        when(product.getUrlKey()).thenReturn("alexia-maxi-dress");
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(option));
        when(product.getVariants()).thenReturn(Collections.singletonList(variant));

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        GetProductVariantsTool tool = new GetProductVariantsTool() {
            @Override
            protected ProductInterface fetch(StoreContext c, String sku) {
                return product;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "VD09"));
        assertEquals("get_product_variants", tool.name());
        assertTrue(out.get("configurable").asBoolean());
        assertEquals(1, out.get("options").size());
        assertEquals("size", out.get("options").get(0).get("code").asText());
        assertEquals(1, out.get("variants").size());
        assertEquals("VD09-LA-L", out.get("variants").get(0).get("sku").asText());
        assertTrue(out.get("variants").get(0).get("inStock").asBoolean());
    }

    @Test
    public void returnsEmptyVariantsForSimpleProduct() throws Exception {
        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("VP11");
        when(product.getName()).thenReturn("Amara Crochet Shorts");
        when(product.getUrlKey()).thenReturn("amara-crochet-shorts");

        StoreContext ctx = mock(StoreContext.class);
        GetProductVariantsTool tool = new GetProductVariantsTool() {
            @Override
            protected ProductInterface fetch(StoreContext c, String sku) {
                return product;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "VP11"));
        assertFalse(out.get("configurable").asBoolean());
        assertEquals(0, out.get("variants").size());
    }
}
