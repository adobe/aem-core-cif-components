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

import org.junit.Test;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResolvePickerSelectionToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void resolvesEachSku() throws Exception {
        ProductInterface p = mock(ProductInterface.class);
        when(p.getSku()).thenReturn("VT01");
        when(p.getName()).thenReturn("Tank");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getClient()).thenReturn(mock(MagentoGraphqlClient.class));

        ResolvePickerSelectionTool tool = new ResolvePickerSelectionTool() {
            @Override
            protected ProductInterface fetch(StoreContext c, String sku) {
                return p;
            }
        };
        JsonNode out = tool.call(ctx, mapper.readTree("{\"skus\":[\"VT01\"]}"));
        assertEquals("Tank", out.get("items").get(0).get("name").asText());
        assertEquals("resolve_picker_selection", tool.name());
    }
}
