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

package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl implements ProductList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    protected static final String PLACEHOLDER_DATA = "/productlist-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);
    private static final String CURRENT_PAGE_QUERY_STRING = "page";

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    private static final int PAGE_SIZE_DEFAULT = 6;
    private static final String CATEGORY_IMAGE_FOLDER = "catalog/category/";

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    private Page productPage;
    private boolean showTitle;
    private boolean showImage;
    private boolean loadClientPrice;

    private int navPageSize = PAGE_SIZE_DEFAULT;

    private Optional<String> categoryId = Optional.empty();

    @Inject
    private SearchResultsService searchResultsService;

    private AbstractCategoryRetriever categoryRetriever;

    private MagentoGraphqlClient magentoGraphqlClient;

    private SearchResultsSet searchResultsSet;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        showImage = properties.get(PN_SHOW_IMAGE, currentStyle.get(PN_SHOW_IMAGE, SHOW_IMAGE_DEFAULT));
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

        String currentPageIndexCandidate = request.getParameter(CURRENT_PAGE_QUERY_STRING);
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(currentPageIndexCandidate);

        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        // get product template page
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);

        // Parse category id from URL
        final String categoryId = parseCategoryId();

        // get GraphQL client and query data
        if (magentoGraphqlClient != null) {
            if (categoryId != null) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryId);
                categoryRetriever.setCurrentPage(currentPageIndex);
                categoryRetriever.setPageSize(navPageSize);
            } else if (!wcmMode.isDisabled()) {
                try {
                    categoryRetriever = new CategoryPlaceholderRetriever(magentoGraphqlClient, PLACEHOLDER_DATA);
                } catch (IOException e) {
                    LOGGER.warn("Cannot use placeholder data", e);
                }
                loadClientPrice = false;
            }
        }

        SearchOptionsImpl searchOptions = new SearchOptionsImpl();
        searchOptions.setCurrentPage(currentPageIndex);
        searchOptions.setAttributeFilters(searchFilters);
        searchOptions.setPageSize(navPageSize);
        searchOptions.setCategoryId(categoryId);

        searchResultsSet = searchResultsService.performSearch(searchOptions, resource, productPage, request);
    }

    @Nullable
    @Override
    public String getTitle() {
        return categoryRetriever.fetchCategory() != null ? categoryRetriever.fetchCategory().getName() : StringUtils.EMPTY;
    }

    @Override
    public boolean showTitle() {
        return showTitle;
    }

    @Override
    public String getImage() {
        if (categoryRetriever != null) {
            if (StringUtils.isEmpty(categoryRetriever.fetchCategory().getImage())) {
                return StringUtils.EMPTY;
            }
            return categoryRetriever.fetchCategory().getImage();
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public boolean showImage() {
        return showImage;
    }

    @Override
    public boolean loadClientPrice() {
        return loadClientPrice;
    }

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        return searchResultsSet.getProductListItems();
    }

    protected Map<String, String> createFilterMap(final Map<String, String[]> parameterMap) {
        Map<String, String> filters = new HashMap<>();
        parameterMap.entrySet().forEach(filterCandidate -> {
            String code = filterCandidate.getKey();
            String[] value = filterCandidate.getValue();

            // we'll make sure there is a value defined for the key
            if (value.length != 1) {
                return;
            }

            filters.put(code, value[0]);
        });

        return filters;
    }

    /**
     * Returns the selector of the current request which is expected to be the category id.
     *
     * @return category id
     */
    private String parseCategoryId() {
        // TODO this should be change to slug/url_path if that is available to retrieve category data,
        // currently we only can use the category id for that.
        return request.getRequestPathInfo().getSelectorString();
    }

    @Override
    public SearchResultsSet getSearchResultsSet() {
        return searchResultsSet;
    }

    @Override
    public String getSelectors() {
        return categoryId.orElse(null);
    }

    protected Integer calculateCurrentPageCursor(final String currentPageIndexCandidate) {
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        return StringUtils.isNumeric(currentPageIndexCandidate) && Integer.valueOf(currentPageIndexCandidate) > 0
            ? Integer
                .parseInt(currentPageIndexCandidate)
            : 1;

    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        return this.categoryRetriever;
    }

}
