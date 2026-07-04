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

import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.SortField;
import com.adobe.cq.commerce.magento.graphql.SortFields;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class GetSortOptionsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void mapsDefaultAndOptions() {
        SortField price = new SortField().setValue("price").setLabel("Price");
        SortField name = new SortField().setValue("name").setLabel("Name");
        SortFields sortFields = new SortFields().setDefault("position").setOptions(Arrays.asList(price, name));

        GetSortOptionsTool tool = new GetSortOptionsTool() {
            @Override
            protected SortFields fetchSortFields(StoreContext ctx) {
                return sortFields;
            }
        };

        StoreContext ctx = mock(StoreContext.class);

        JsonNode out = tool.call(ctx, mapper.createObjectNode());

        assertEquals("get_sort_options", tool.name());
        assertEquals("position", out.get("default").asText());
        JsonNode options = out.get("options");
        assertEquals(2, options.size());
        assertEquals("price", options.get(0).get("value").asText());
        assertEquals("Price", options.get(0).get("label").asText());
        assertEquals("name", options.get(1).get("value").asText());
        assertEquals("Name", options.get(1).get("label").asText());

        // per catalog §3.1: relevance is servlet-injected for searchresults only, never synthesized here
        for (JsonNode option : options) {
            assertFalse("relevance".equals(option.get("value").asText()));
        }
    }

    @Test
    public void handlesNullSortFieldsGracefully() {
        GetSortOptionsTool tool = new GetSortOptionsTool() {
            @Override
            protected SortFields fetchSortFields(StoreContext ctx) {
                return null;
            }
        };

        StoreContext ctx = mock(StoreContext.class);

        JsonNode out = tool.call(ctx, mapper.createObjectNode());

        assertNull(out.get("default").textValue());
        assertEquals(0, out.get("options").size());
    }

    @Test
    public void handlesEmptyOptionsGracefully() {
        SortFields sortFields = new SortFields().setDefault(null).setOptions(Collections.emptyList());

        GetSortOptionsTool tool = new GetSortOptionsTool() {
            @Override
            protected SortFields fetchSortFields(StoreContext ctx) {
                return sortFields;
            }
        };

        StoreContext ctx = mock(StoreContext.class);

        JsonNode out = tool.call(ctx, mapper.createObjectNode());

        assertNull(out.get("default").textValue());
        assertEquals(0, out.get("options").size());
    }
}
