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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.search.models.SearchOptions;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchProductsToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void searchesAndMapsResults() throws Exception {
        Price price = mock(Price.class);
        when(price.getFinalPrice()).thenReturn(19.99);
        when(price.getCurrency()).thenReturn("USD");
        ProductListItem item = mock(ProductListItem.class);
        when(item.getSKU()).thenReturn("VT01");
        when(item.getTitle()).thenReturn("Tank");
        when(item.getSlug()).thenReturn("tank");
        when(item.getPriceRange()).thenReturn(price);

        SearchResultsSet resultsSet = mock(SearchResultsSet.class);
        when(resultsSet.getProductListItems()).thenReturn(Collections.singletonList(item));
        when(resultsSet.getTotalResults()).thenReturn(1);

        SearchResultsService service = mock(SearchResultsService.class);
        when(service.performSearch(any(SearchOptions.class), any(Resource.class),
            any(Page.class), any(SlingHttpServletRequest.class))).thenReturn(resultsSet);

        SearchProductsTool tool = new SearchProductsTool();
        tool.searchResultsService = service;

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getResource()).thenReturn(mock(Resource.class));
        when(ctx.getRequest()).thenReturn(mock(SlingHttpServletRequest.class));
        when(ctx.getProductPage()).thenReturn(mock(Page.class));

        JsonNode out = tool.call(ctx, mapper.readTree("{\"query\":\"tank\",\"page\":1,\"pageSize\":12}"));
        assertEquals(1, out.get("total").asInt());
        assertEquals("VT01", out.get("items").get(0).get("sku").asText());
        assertEquals("search_products", tool.name());
        assertFalse(tool.writesContent());
    }
}
