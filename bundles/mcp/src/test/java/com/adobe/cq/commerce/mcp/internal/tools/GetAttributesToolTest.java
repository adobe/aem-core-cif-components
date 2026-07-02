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

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.core.search.services.SearchFilterService;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAttributesToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void listsFilterableAttributes() throws Exception {
        FilterAttributeMetadata meta = mock(FilterAttributeMetadata.class);
        when(meta.getAttributeCode()).thenReturn("color");
        when(meta.getFilterInputType()).thenReturn("equal");

        SearchFilterService service = mock(SearchFilterService.class);
        when(service.retrieveCurrentlyAvailableCommerceFilters(any(Page.class)))
            .thenReturn(Collections.singletonList(meta));

        GetAttributesTool tool = new GetAttributesTool();
        tool.searchFilterService = service;

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(mock(Page.class));

        JsonNode out = tool.call(ctx, mapper.createObjectNode());
        assertEquals("color", out.get("attributes").get(0).get("code").asText());
        assertEquals("get_attributes", tool.name());
    }
}
