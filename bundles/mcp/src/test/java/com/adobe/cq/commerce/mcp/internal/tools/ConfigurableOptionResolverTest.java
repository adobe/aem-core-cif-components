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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptions;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProductOptionsValues;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurableOptionResolverTest {
    private final ConfigurableOptionResolver resolver = new ConfigurableOptionResolver();

    @Test
    public void simpleProductNeedsNoOptions() {
        ProductInterface simple = mock(ProductInterface.class);
        List<ID> result = resolver.resolve(simple, new HashMap<>());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void resolvesMatchingOptionsToUids() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("fashion_color", "Blue");

        List<ID> result = resolver.resolve(product, supplied);
        assertEquals(Collections.singletonList(new ID("value-blue")), result);
    }

    @Test
    public void matchesBySuppliedLabelToo() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        when(blue.getUid()).thenReturn(new ID("value-blue"));

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("color", "blue");

        List<ID> result = resolver.resolve(product, supplied);
        assertEquals(Collections.singletonList(new ID("value-blue")), result);
    }

    @Test
    public void throwsWithAvailableValuesWhenOptionMissing() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");
        ConfigurableProductOptionsValues red = mock(ConfigurableProductOptionsValues.class);
        when(red.getLabel()).thenReturn("Red");

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Arrays.asList(blue, red));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, new HashMap<>()));
        assertTrue(ex.getMessage().contains("Color"));
        assertTrue(ex.getMessage().contains("Blue"));
        assertTrue(ex.getMessage().contains("Red"));
    }

    @Test
    public void throwsWithAvailableValuesWhenOptionValueInvalid() {
        ConfigurableProductOptionsValues blue = mock(ConfigurableProductOptionsValues.class);
        when(blue.getLabel()).thenReturn("Blue");

        ConfigurableProductOptions color = mock(ConfigurableProductOptions.class);
        when(color.getAttributeCode()).thenReturn("fashion_color");
        when(color.getLabel()).thenReturn("Color");
        when(color.getValues()).thenReturn(Collections.singletonList(blue));

        ConfigurableProduct product = mock(ConfigurableProduct.class);
        when(product.getConfigurableOptions()).thenReturn(Collections.singletonList(color));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("fashion_color", "Purple");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, supplied));
        assertTrue(ex.getMessage().contains("must be one of"));
        assertTrue(ex.getMessage().contains("Blue"));
    }
}
