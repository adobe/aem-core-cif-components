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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;


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
import com.adobe.cq.commerce.core.components.internal.models.v1.common.PriceImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.ProductListItemImpl;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;

//todo-kevin: check what CategoryProducts and GroupedProducts were doing.
//import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
//import com.adobe.cq.commerce.magento.graphql.GroupedProduct;
//import com.adobe.cq.commerce.magento.graphql.ProductImage;
//import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import com.adobe.cq.commerce.core.search.SearchOptions;
import com.adobe.cq.commerce.core.search.SearchResultsService;
import com.adobe.cq.commerce.core.search.SearchResultsSet;
import com.adobe.cq.commerce.core.search.internal.SearchOptionsImpl;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;

import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl implements ProductList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    protected static final String PLACEHOLDER_DATA = "productlist-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);
    private static final String CURRENT_PAGE_QUERY_STRING = "page";

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    private static final int PAGE_SIZE_DEFAULT = 6;

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

    @Inject
    private SearchResultsService searchResultsService;

    private Page productPage;
    private CategoryInterface category;
    private boolean showTitle;
    private boolean showImage;
    private boolean loadClientPrice;
    private MagentoGraphqlClient magentoGraphqlClient;


    private Locale locale;
    private int navPageCursor = 1;
    private int navPageSize = PAGE_SIZE_DEFAULT;
    private Integer navPagePrev;
    private Integer navPageNext;
    private List<Integer> navPages;

    private SearchResultsSet searchResultsSet;

    private Integer categoryId;

    private AbstractCategoryRetriever categoryRetriever;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        showImage = properties.get(PN_SHOW_IMAGE, currentStyle.get(PN_SHOW_IMAGE, SHOW_IMAGE_DEFAULT));
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

        String currentPageIndexCandidate = request.getParameter(CURRENT_PAGE_QUERY_STRING);
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = StringUtils.isNumeric(currentPageIndexCandidate) && Integer.valueOf(currentPageIndexCandidate) > 0
            ? Integer
                .valueOf(currentPageIndexCandidate)
            : 1;
        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        // get product template page
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        locale = productPage.getLanguage(false);

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);

        // Parse category id from URL
        final String categoryId = parseCategoryId();

        // get GraphQL client and query data
        if (magentoGraphqlClient != null) {
            if (categoryId != null) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryId);
                categoryRetriever.setCurrentPage(navPageCursor);
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

        SearchOptions searchOptions = new SearchOptionsImpl();
        searchOptions.setCurrentPage(currentPageIndex);
        searchOptions.setAttributeFilters(searchFilters);
        searchOptions.setCategoryId(categoryId);

        searchResultsSet = searchResultsService.performSearch(searchOptions, resource, productPage);
    }

    @Nullable
    @Override
    public String getTitle() {
        return category != null ? category.getName() : StringUtils.EMPTY;
    }

    @Override
    public boolean showTitle() {
        return showTitle;
    }

    @Override
    public int getTotalCount() {
        return searchResultsSet.getTotalResults();
    }

    @Override
    public int getCurrentNavPage() {
        return navPageCursor;
    }

    @Override
    public int getNextNavPage() {
        if (navPageNext == null) {
            this.setupPagination();
        }

        if ((getTotalCount() % navPageSize) == 0) {
            // if currentNavPage is already at last, set navPageNext to currentNavPage
            navPageNext = (navPageCursor < (getTotalCount() / navPageSize)) ? (navPageCursor + 1) : navPageCursor;
        } else {
            navPageNext = (navPageCursor < ((getTotalCount() / navPageSize) + 1)) ? (navPageCursor + 1) : navPageCursor;
        }
        return navPageNext;
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

    @Override
    public int getPreviousNavPage() {
        if (navPagePrev == null) {
            this.setupPagination();
        }
        return navPagePrev;
    }

    @Override
    public List<Integer> getPageList() {
        if (navPages == null) {
            this.setupPagination();
        }
        return navPages;
    }

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {

        //todo-kevin: make sure converter accounts for any updated logic here
//                for (ProductInterface product : products.getItems()) {
//                    try {
//                        boolean isStartPrice = product instanceof GroupedProduct;
//                        Price price = new PriceImpl(product.getPriceRange(), locale, isStartPrice);
//                        ProductImage smallImage = product.getSmallImage();
//                        listItems.add(new ProductListItemImpl(
//                            product.getSku(),
//                            product.getUrlKey(),
//                            product.getName(),
//                            price,
//                            smallImage == null ? null : smallImage.getUrl(),
//                            productPage,
//                            null,
//                            request));

        return searchResultsSet.getProductListItems();
    }

    @Override
    public SearchResultsSet getSearchResultsSet() {
        return searchResultsSet;
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

    private CategoryTreeQueryDefinition generateProductListQuery() {
        CategoryTreeQueryDefinition categoryTreeQueryDefinition = q -> q
            .id()
            .description()
            .name()
            .image();

        return categoryTreeQueryDefinition;
    }

    private StoreConfigQueryDefinition generateStoreConfigQuery() {
        return q -> q.secureBaseMediaUrl();
    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        return this.categoryRetriever;
    }

    /* --- Utility methods --- */

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

    /**
     * Obtains value from request for page, sets Pagination values for current, next and previous pages
     *
     * @return void
     */
    void setupPagination() {
        int navPagesSize = 0;

        navPagePrev = (navPageCursor <= 1) ? 1 : (navPageCursor - 1);
        if ((getTotalCount() % navPageSize) == 0) {
            navPagesSize = getTotalCount() / navPageSize;
            navPageNext = (navPageCursor < (getTotalCount() / navPageSize)) ? (navPageCursor + 1) : navPageCursor;
        } else {
            navPagesSize = (getTotalCount() / navPageSize) + 1;
            navPageNext = (navPageCursor < ((getTotalCount() / navPageSize) + 1)) ? (navPageCursor + 1) : navPageCursor;
        }
        navPages = new ArrayList<>();

        if (navPagesSize < 8) {
            for (int i = 0; i < navPagesSize; i++) {
                navPages.add(i + 1);
            }
        } else {
            if (navPagePrev > 1) {
                navPages.add(1);
            }
            if (navPagePrev > 2) {
                navPages.add(0);
            }
            if (navPagePrev < navPageCursor) {
                navPages.add(navPagePrev);
            }
            navPages.add(navPageCursor);
            if (navPageNext > navPageCursor) {
                navPages.add(navPageNext);
            }
            if (navPageNext < navPagesSize - 1) {
                navPages.add(0);
            }
            if (navPageNext < navPagesSize) {
                navPages.add(navPagesSize);
            }
        }
    }

    protected Integer calculateCurrentPageCursor(final String currentPageIndexCandidate) {
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        return StringUtils.isNumeric(currentPageIndexCandidate) && Integer.valueOf(currentPageIndexCandidate) > 0
            ? Integer
                .valueOf(currentPageIndexCandidate)
            : 1;
    }

}
