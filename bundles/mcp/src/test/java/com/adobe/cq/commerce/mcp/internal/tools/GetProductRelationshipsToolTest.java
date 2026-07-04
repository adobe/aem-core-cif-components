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

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductLinksInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetProductRelationshipsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private ProductLinksInterface link(String linkType, String linkedSku, String linkedType, int position, String sku) {
        ProductLinksInterface link = mock(ProductLinksInterface.class);
        when(link.getLinkType()).thenReturn(linkType);
        when(link.getLinkedProductSku()).thenReturn(linkedSku);
        when(link.getLinkedProductType()).thenReturn(linkedType);
        when(link.getPosition()).thenReturn(position);
        when(link.getSku()).thenReturn(sku);
        return link;
    }

    @Test
    public void returnsAllLinksWhenNoFilter() throws Exception {
        ProductLinksInterface related = link("related", "MJ02", "simple", 1, "MJ01");
        ProductLinksInterface upsell = link("upsell", "MJ03", "simple", 2, "MJ01");

        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("MJ01");
        when(product.getProductLinks()).thenReturn(Arrays.asList(related, upsell));

        StoreContext ctx = mock(StoreContext.class);
        GetProductRelationshipsTool tool = new GetProductRelationshipsTool() {
            @Override
            protected ProductInterface fetch(StoreContext c, String sku) {
                return product;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "MJ01"));
        assertEquals("get_product_relationships", tool.name());
        assertEquals("MJ01", out.get("sku").asText());
        assertEquals(2, out.get("links").size());
        assertEquals("related", out.get("links").get(0).get("linkType").asText());
        assertEquals("MJ02", out.get("links").get(0).get("linkedProductSku").asText());
        assertEquals("simple", out.get("links").get(0).get("linkedProductType").asText());
        assertEquals(1, out.get("links").get(0).get("position").asInt());
        assertEquals("MJ01", out.get("links").get(0).get("sku").asText());
        assertEquals("upsell", out.get("links").get(1).get("linkType").asText());
        assertEquals("MJ03", out.get("links").get(1).get("linkedProductSku").asText());
    }

    @Test
    public void filtersByLinkTypeCaseInsensitively() throws Exception {
        ProductLinksInterface related = link("related", "MJ02", "simple", 1, "MJ01");
        ProductLinksInterface upsell = link("upsell", "MJ03", "simple", 2, "MJ01");

        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("MJ01");
        when(product.getProductLinks()).thenReturn(Arrays.asList(related, upsell));

        StoreContext ctx = mock(StoreContext.class);
        GetProductRelationshipsTool tool = new GetProductRelationshipsTool() {
            @Override
            protected ProductInterface fetch(StoreContext c, String sku) {
                return product;
            }
        };

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "MJ01").put("linkType", "RELATED"));
        assertEquals(1, out.get("links").size());
        assertEquals("related", out.get("links").get(0).get("linkType").asText());
        assertEquals("MJ02", out.get("links").get(0).get("linkedProductSku").asText());
    }

    @Test
    public void missingSkuThrows() {
        StoreContext ctx = mock(StoreContext.class);
        GetProductRelationshipsTool tool = new GetProductRelationshipsTool();
        assertThrows(IllegalArgumentException.class, () -> tool.call(ctx, mapper.createObjectNode()));
    }
}
