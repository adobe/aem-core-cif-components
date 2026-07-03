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

import com.adobe.cq.commerce.magento.graphql.BundleItem;
import com.adobe.cq.commerce.magento.graphql.BundleItemOption;
import com.adobe.cq.commerce.magento.graphql.BundleProduct;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.shopify.graphql.support.ID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BundleOptionResolverTest {
    private final BundleOptionResolver resolver = new BundleOptionResolver();

    @Test
    public void nonBundleProductNeedsNoOptions() {
        ProductInterface simple = mock(ProductInterface.class);
        List<ID> result = resolver.resolve(simple, new HashMap<>());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void resolvesRequiredAndSkipsOmittedOptional() {
        BundleItemOption carmina = mock(BundleItemOption.class);
        when(carmina.getLabel()).thenReturn("Carmina Necklace");
        when(carmina.getUid()).thenReturn(new ID("bundle/2/2/1"));
        BundleItem necklace = mock(BundleItem.class);
        when(necklace.getTitle()).thenReturn("Necklace");
        when(necklace.getRequired()).thenReturn(true);
        when(necklace.getOptions()).thenReturn(Collections.singletonList(carmina));

        BundleItemOption goldEarrings = mock(BundleItemOption.class);
        when(goldEarrings.getLabel()).thenReturn("Gold Cirque Earrings");
        BundleItem earrings = mock(BundleItem.class);
        when(earrings.getTitle()).thenReturn("Cirque Earrings");
        when(earrings.getRequired()).thenReturn(false);
        when(earrings.getOptions()).thenReturn(Collections.singletonList(goldEarrings));

        BundleProduct product = mock(BundleProduct.class);
        when(product.getItems()).thenReturn(Arrays.asList(necklace, earrings));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("Necklace", "Carmina Necklace");

        List<ID> result = resolver.resolve(product, supplied);
        assertEquals(Collections.singletonList(new ID("bundle/2/2/1")), result);
    }

    @Test
    public void throwsWithAvailableValuesWhenRequiredItemMissing() {
        BundleItemOption carmina = mock(BundleItemOption.class);
        when(carmina.getLabel()).thenReturn("Carmina Necklace");
        BundleItemOption augusta = mock(BundleItemOption.class);
        when(augusta.getLabel()).thenReturn("Augusta Necklace");
        BundleItem necklace = mock(BundleItem.class);
        when(necklace.getTitle()).thenReturn("Necklace");
        when(necklace.getRequired()).thenReturn(true);
        when(necklace.getOptions()).thenReturn(Arrays.asList(carmina, augusta));

        BundleProduct product = mock(BundleProduct.class);
        when(product.getItems()).thenReturn(Collections.singletonList(necklace));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, new HashMap<>()));
        assertTrue(ex.getMessage().contains("Necklace"));
        assertTrue(ex.getMessage().contains("Carmina Necklace"));
        assertTrue(ex.getMessage().contains("Augusta Necklace"));
    }

    @Test
    public void throwsWithAvailableValuesWhenSuppliedValueInvalid() {
        BundleItemOption carmina = mock(BundleItemOption.class);
        when(carmina.getLabel()).thenReturn("Carmina Necklace");
        BundleItem necklace = mock(BundleItem.class);
        when(necklace.getTitle()).thenReturn("Necklace");
        when(necklace.getRequired()).thenReturn(true);
        when(necklace.getOptions()).thenReturn(Collections.singletonList(carmina));

        BundleProduct product = mock(BundleProduct.class);
        when(product.getItems()).thenReturn(Collections.singletonList(necklace));

        Map<String, String> supplied = new HashMap<>();
        supplied.put("Necklace", "Ruby Necklace");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve(product, supplied));
        assertTrue(ex.getMessage().contains("must be one of"));
        assertTrue(ex.getMessage().contains("Carmina Necklace"));
    }
}
