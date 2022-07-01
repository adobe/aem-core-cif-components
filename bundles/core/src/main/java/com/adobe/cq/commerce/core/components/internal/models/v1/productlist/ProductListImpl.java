/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.productlist;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.CategoryStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.storefrontcontext.CategoryStorefrontContext;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.internal.converters.ProductToProductListItemConverter;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ProductList.class,
    resourceType = ProductListImpl.RESOURCE_TYPE,
    cache = true)
public class ProductListImpl extends ProductCollectionImpl implements ProductList {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    protected static final String PLACEHOLDER_DATA = "productlist-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final String CATEGORY_PROPERTY = "category";

    private boolean showTitle;
    private boolean showImage;

    // This script variable is not injected when the model is instantiated in SpecificPageServlet
    @ScriptVariable(name = "wcmmode", injectionStrategy = InjectionStrategy.OPTIONAL)
    private SightlyWCMMode wcmMode = null;
    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;
    @SlingObject
    private SlingScriptHelper sling;
    @ValueMapValue(name = CATEGORY_PROPERTY, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String categoryUid;

    protected AbstractCategoryRetriever categoryRetriever;
    private boolean usePlaceholderData;
    private boolean isAuthor;
    private String canonicalUrl;

    private Pair<CategoryInterface, SearchResultsSet> categorySearchResultsSet;

    @PostConstruct
    protected void initModel() {
        if (properties == null) {
            properties = request.getResource().getValueMap();
        }
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        showImage = properties.get(PN_SHOW_IMAGE, currentStyle.get(PN_SHOW_IMAGE, SHOW_IMAGE_DEFAULT));
        isAuthor = wcmMode != null && !wcmMode.isDisabled();

        String currentPageIndexCandidate = request.getParameter(SearchOptionsImpl.CURRENT_PAGE_PARAMETER_ID);
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(currentPageIndexCandidate);

        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        if (StringUtils.isBlank(categoryUid)) {
            // If not provided via the category property extract category identifier from URL
            categoryUid = urlProvider.getCategoryIdentifier(request);
        }

        if (magentoGraphqlClient != null) {
            if (StringUtils.isNotBlank(categoryUid)) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient) {
                    @Override
                    public CategoryInterface fetchCategory() {
                        searchOptions.setCategoryUid(categoryUid);
                        getCategorySearchResultsSet();
                        return categorySearchResultsSet.getLeft();
                    }
                };
                categoryRetriever.setIdentifier(categoryUid);
            } else if (isAuthor) {
                usePlaceholderData = true;
                loadClientPrice = false;
                try {
                    categoryRetriever = new CategoryPlaceholderRetriever(magentoGraphqlClient, PLACEHOLDER_DATA);
                } catch (IOException e) {
                    LOGGER.warn("Cannot use placeholder data", e);
                }
            } else {
                // There isn't any selector on publish instance
                searchResultsSet = new SearchResultsSetImpl();
                categorySearchResultsSet = Pair.of(null, searchResultsSet);
                return;
            }
        }

        if (usePlaceholderData) {
            searchResultsSet = new SearchResultsSetImpl();
        } else {
            searchOptions = new SearchOptionsImpl();
            searchOptions.setCurrentPage(currentPageIndex);
            searchOptions.setPageSize(navPageSize);
            searchOptions.setAttributeFilters(searchFilters);

            // configure sorting
            searchOptions.addSorterKey("price", "Price", Sorter.Order.ASC);
            searchOptions.addSorterKey("name", "Product Name", Sorter.Order.ASC);
        }
    }

    @Nullable
    @Override
    public String getTitle() {
        return getCategory() != null ? getCategory().getName()
            : StringUtils.EMPTY;
    }

    @Override
    public boolean showTitle() {
        return showTitle;
    }

    @Override
    public String getImage() {
        if (getCategory() != null) {
            if (StringUtils.isEmpty(getCategory().getImage())) {
                return StringUtils.EMPTY;
            }
            return getCategory().getImage();
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public boolean showImage() {
        return showImage;
    }

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        if (usePlaceholderData) {
            CategoryInterface category = getCategory();
            CategoryProducts categoryProducts = category.getProducts();
            ProductToProductListItemConverter converter = new ProductToProductListItemConverter(productPage, request, urlProvider, getId(),
                category);
            return categoryProducts.getItems().stream()
                .map(converter)
                .filter(Objects::nonNull) // the converter returns null if the conversion fails
                .collect(Collectors.toList());
        } else {
            return getSearchResultsSet().getProductListItems();
        }
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = getCategorySearchResultsSet().getRight();

            List<SearchAggregation> aggregations = searchResultsSet.getSearchAggregations()
                .stream()
                .filter(searchAggregation -> !SearchOptionsImpl.CATEGORY_UID_PARAMETER_ID.equals(searchAggregation.getIdentifier()))
                .collect(Collectors.toList());

            aggregations.removeIf(agg -> "category_id".equals(agg.getIdentifier()));

            ((SearchResultsSetImpl) searchResultsSet).setSearchAggregations(aggregations);
        }
        return searchResultsSet;
    }

    private Pair<CategoryInterface, SearchResultsSet> getCategorySearchResultsSet() {
        if (categorySearchResultsSet == null) {
            Consumer<ProductInterfaceQuery> productQueryHook = categoryRetriever != null ? categoryRetriever.getProductQueryHook() : null;
            categorySearchResultsSet = searchResultsService
                .performSearch(searchOptions, resource, productPage, request, productQueryHook, categoryRetriever);
        }
        return categorySearchResultsSet;
    }

    protected CategoryInterface getCategory() {
        if (usePlaceholderData) {
            return categoryRetriever.fetchCategory();
        }
        return getCategorySearchResultsSet().getLeft();
    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        return categoryRetriever;
    }

    @Override
    public String getMetaDescription() {
        return getCategory() != null ? getCategory().getMetaDescription() : null;
    }

    @Override
    public String getMetaKeywords() {
        return getCategory() != null ? getCategory().getMetaKeywords() : null;
    }

    @Override
    public String getMetaTitle() {
        return StringUtils.defaultString(getCategory() != null ? getCategory().getMetaTitle() : null, getTitle());
    }

    @Override
    public String getCanonicalUrl() {
        if (usePlaceholderData) {
            // placeholder data has no canonical url
            return null;
        }
        if (canonicalUrl == null) {
            Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
            CategoryInterface category = getCategory();
            SitemapLinkExternalizerProvider sitemapLinkExternalizerProvider = sling.getService(SitemapLinkExternalizerProvider.class);

            if (category != null && categoryPage != null && sitemapLinkExternalizerProvider != null) {
                canonicalUrl = sitemapLinkExternalizerProvider.getExternalizer(request.getResourceResolver())
                    .toExternalCategoryUrl(request, categoryPage, new CategoryUrlFormat.Params(category));
            } else {
                // fallback to legacy logic
                if (isAuthor) {
                    canonicalUrl = externalizer.authorLink(resource.getResourceResolver(), request.getRequestURI());
                } else {
                    canonicalUrl = externalizer.publishLink(resource.getResourceResolver(), request.getRequestURI());
                }
            }
        }
        return canonicalUrl;
    }

    @Override
    public Map<Locale, String> getAlternateLanguageLinks() {
        // we don't support alternate language links on categories yet
        return Collections.emptyMap();
    }

    @Override
    public CategoryStorefrontContext getStorefrontContext() {
        return new CategoryStorefrontContextImpl(getCategory(), resource);
    }
}
