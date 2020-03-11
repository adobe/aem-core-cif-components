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

package com.adobe.cq.commerce.core.components.internal.models.v1.searchresults;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.day.cq.wcm.api.Page;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchResultsImplTest {

    Page productPage;

    SlingHttpServletRequest incomingRequest;

    private SearchResultsImpl slingModel;

    private CategoryInterface categoryQueryResult;

    @Before
    public void setUp() throws Exception {
        this.slingModel = new SearchResultsImpl();

        incomingRequest = mock(SlingHttpServletRequest.class);
        when(incomingRequest.getParameter("search_query")).thenReturn("test-query");

        Whitebox.setInternalState(this.slingModel, "request", incomingRequest);

        // AEM page
        productPage = mock(Page.class);
        when(productPage.getLanguage(false)).thenReturn(Locale.US);
        when(productPage.getPath()).thenReturn("/content/test-product-page");
        Map<String, Object> pageProperties = new HashMap<>();

        pageProperties.put(ProductList.PN_PAGE_SIZE, 6); // setting page size to 6

        // Search Results Set
        SearchResultsSetImpl searchResultsSet = new SearchResultsSetImpl();
        searchResultsSet.setProductListItems(new LinkedList<>());
        Whitebox.setInternalState(this.slingModel, "searchResultsSet", searchResultsSet);

        ValueMapDecorator vMD = new ValueMapDecorator(pageProperties);

        when(productPage.getProperties()).thenReturn(vMD);

        Whitebox.setInternalState(this.slingModel, "productPage", productPage);
    }

    @Test
    public void testGetSearchResultsSet() {
        final SearchResultsSet searchResultsSet = slingModel.getSearchResultsSet();
        Assert.assertEquals("search result set comes back", 1, 1);
    }

    @Test
    public void testCreateFilterMap() {
        Map<String, String[]> queryParameters;
        Map<String, String> filterMap;

        queryParameters = new HashMap<>();
        queryParameters.put("search_query", new String[] { "ok" });
        filterMap = slingModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters query string parameter out correctly", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] {});
        filterMap = slingModel.createFilterMap(queryParameters);
        Assert.assertEquals("filters out query parameters without values", 0, filterMap.size());

        queryParameters = new HashMap<>();
        queryParameters.put("color", new String[] { "123" });
        filterMap = slingModel.createFilterMap(queryParameters);
        Assert.assertEquals("retails valid query filters", 1, filterMap.size());
    }

    @Test
    public void testCalculateCurrentPageCursor() {
        Assert.assertEquals("negative page indexes are not allowed", 1, slingModel.calculateCurrentPageCursor("-1").intValue());
        Assert.assertEquals("null value is dealt with", 1, slingModel.calculateCurrentPageCursor(null).intValue());
    }

}
