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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.CategoryIdentifierType;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.internal.converters.ProductToProductListItemConverter;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl implements ProductList {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    protected static final String PLACEHOLDER_DATA = "productlist-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;

    private Page productPage;
    private boolean showTitle;
    private boolean showImage;
    private boolean loadClientPrice;
    private int navPageSize;

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

    @Inject
    private UrlProvider urlProvider;

    private AbstractCategoryRetriever categoryRetriever;
    private SearchOptionsImpl searchOptions;
    private SearchResultsSet searchResultsSet;
    private boolean usePlaceholderData;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        showImage = properties.get(PN_SHOW_IMAGE, currentStyle.get(PN_SHOW_IMAGE, SHOW_IMAGE_DEFAULT));
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, SearchOptionsImpl.PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

        String currentPageIndexCandidate = request.getParameter(SearchOptionsImpl.CURRENT_PAGE_PARAMETER_ID);
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(currentPageIndexCandidate);

        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        // get product template page
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        MagentoGraphqlClient magentoGraphqlClient = MagentoGraphqlClient.create(resource);

        // Parse category identifier from URL
        Pair<CategoryIdentifierType, String> identifier = urlProvider.getCategoryIdentifier(request);

        // get GraphQL client and query data
        if (magentoGraphqlClient != null) {
            if (identifier != null && StringUtils.isNotBlank(identifier.getRight())) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(identifier.getLeft(), identifier.getRight());
            } else if (!wcmMode.isDisabled()) {
                usePlaceholderData = true;
                loadClientPrice = false;
                try {
                    categoryRetriever = new CategoryPlaceholderRetriever(magentoGraphqlClient, PLACEHOLDER_DATA);
                } catch (IOException e) {
                    LOGGER.warn("Cannot use placeholder data", e);
                }
            }
        }

        if (usePlaceholderData) {
            searchResultsSet = new SearchResultsSetImpl();
        } else {
            searchOptions = new SearchOptionsImpl();
            searchOptions.setCurrentPage(currentPageIndex);
            searchOptions.setPageSize(navPageSize);
            searchOptions.setAttributeFilters(searchFilters);
            searchOptions.setCategoryId(identifier.getRight());
        }
    }

    @Nullable
    @Override
    public String getTitle() {
        return categoryRetriever != null && categoryRetriever.fetchCategory() != null ? categoryRetriever.fetchCategory().getName()
            : StringUtils.EMPTY;
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
        if (usePlaceholderData) {
            CategoryProducts categoryProducts = categoryRetriever.fetchCategory().getProducts();
            ProductToProductListItemConverter converter = new ProductToProductListItemConverter(productPage, request, urlProvider);
            return categoryProducts.getItems().stream()
                .map(converter)
                .filter(p -> p != null) // the converter returns null if the conversion fails
                .collect(Collectors.toList());
        } else {
            return getSearchResultsSet().getProductListItems();
        }
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

    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            Consumer<ProductInterfaceQuery> productQueryHook = categoryRetriever != null ? categoryRetriever.getProductQueryHook() : null;
            searchResultsSet = searchResultsService.performSearch(searchOptions, resource, productPage, request, productQueryHook);
        }
        return searchResultsSet;
    }

    protected Integer calculateCurrentPageCursor(final String currentPageIndexCandidate) {
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        return StringUtils.isNumeric(currentPageIndexCandidate) && Integer.valueOf(currentPageIndexCandidate) > 0
            ? Integer.parseInt(currentPageIndexCandidate)
            : 1;
    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        return categoryRetriever;
    }

}
