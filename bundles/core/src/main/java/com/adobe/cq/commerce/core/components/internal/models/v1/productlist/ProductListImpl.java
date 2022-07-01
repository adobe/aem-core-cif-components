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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.common.CommerceExperienceFragmentContainerImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment.CommerceExperienceFragmentImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.CategoryStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.CommerceExperienceFragmentContainer;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.storefrontcontext.CategoryStorefrontContext;
import com.adobe.cq.commerce.core.search.internal.converters.ProductToProductListItemConverter;
import com.adobe.cq.commerce.core.search.internal.models.SearchAggregationOptionImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchAggregation;
import com.adobe.cq.commerce.core.search.models.SearchAggregationOption;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.Sorter;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl extends ProductCollectionImpl implements ProductList {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    protected static final String PLACEHOLDER_DATA = "productlist-component-placeholder-data.json";
    protected static final String PN_FRAGMENT_LOCATION = "fragmentLocation";
    protected static final String PN_FRAGMENT_CSS_CLASS = "fragmentCssClass";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final String CATEGORY_PROPERTY = "category";
    static final String CATEGORY_AGGREGATION_ID = "category_id";

    private boolean showTitle;
    private boolean showImage;

    // This script variable is not injected when the model is instantiated in
    // SpecificPageServlet
    @ScriptVariable(name = "wcmmode", injectionStrategy = InjectionStrategy.OPTIONAL)
    private SightlyWCMMode wcmMode = null;
    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;
    @SlingObject
    private SlingScriptHelper sling;
    @ValueMapValue(name = CATEGORY_PROPERTY, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String categoryUid;

    @OSGiService
    private CommerceExperienceFragmentsRetriever fragmentsRetriever;

    protected AbstractCategoryRetriever categoryRetriever;
    private boolean usePlaceholderData;
    private boolean isAuthor;
    private String canonicalUrl;

    private Pair<CategoryInterface, SearchResultsSet> categorySearchResultsSet;

    protected List<CommerceExperienceFragmentContainer> fragments = new ArrayList<>();
    protected int pageGridSize;

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
        // make sure the current page from the query string is reasonable i.e. numeric
        // and over 0
        Integer currentPageIndex = calculateCurrentPageCursor(currentPageIndexCandidate);

        Map<String, String> searchFilters = createFilterMap(request.getParameterMap());

        if (StringUtils.isBlank(categoryUid)) {
            // If not provided via the category property extract category identifier from
            // URL
            categoryUid = urlProvider.getCategoryIdentifier(request);
        }

        if (magentoGraphqlClient != null) {
            if (StringUtils.isNotBlank(categoryUid)) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
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
        pageGridSize = navPageSize;

        if (usePlaceholderData) {
            searchResultsSet = new SearchResultsSetImpl();
        } else {
            if (currentPageIndex == 1) {
                Resource fragmentsNode = resource.getChild(ProductList.NN_FRAGMENTS);
                if (fragmentsNode != null && fragmentsNode.hasChildren()) {
                    Iterable<Resource> configuredFragments = fragmentsNode.getChildren();
                    for (Resource fragment : configuredFragments) {
                        String fragmentCssClass = fragment.getValueMap().get(PN_FRAGMENT_CSS_CLASS, String.class);
                        ValueMapResourceWrapper resourceWrapper = new ValueMapResourceWrapper(
                            fragment,
                            CommerceExperienceFragmentImpl.RESOURCE_TYPE);
                        String fragmentLocation = fragment.getValueMap().get(PN_FRAGMENT_LOCATION,
                            String.class);
                        resourceWrapper.getValueMap().put(PN_FRAGMENT_LOCATION, fragmentLocation);
                        if (!fragmentsRetriever
                            .getExperienceFragmentsForCategory(categoryUid, fragmentLocation, currentPage)
                            .isEmpty()) {
                            fragments.add(new CommerceExperienceFragmentContainerImpl(resourceWrapper,
                                fragmentCssClass));
                        }
                    }
                }
            }

            searchOptions = new SearchOptionsImpl();
            searchOptions.setCurrentPage(currentPageIndex);
            searchOptions.setPageSize(navPageSize);
            searchOptions.setAttributeFilters(searchFilters);

            // configure sorting
            String defaultSortField = properties.get(PN_DEFAULT_SORT_FIELD, String.class);
            String defaultSortOrder = properties.get(PN_DEFAULT_SORT_ORDER, Sorter.Order.ASC.name());

            if (StringUtils.isNotBlank(defaultSortField)) {
                Sorter.Order value = Sorter.Order.fromString(defaultSortOrder, Sorter.Order.ASC);
                searchOptions.setDefaultSorter(defaultSortField, value);
            }
        }
    }

    @Nullable
    @Override
    public String getTitle() {
        return getCategory() != null ? getCategory().getName() : StringUtils.EMPTY;
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
            ProductToProductListItemConverter converter = new ProductToProductListItemConverter(currentPage, request,
                urlProvider, getId(),
                category);
            return categoryProducts.getItems().stream()
                .map(converter)
                .filter(Objects::nonNull) // the converter returns null if the conversion fails
                .collect(Collectors.toList());
        } else {
            return getSearchResultsSet().getProductListItems();
        }
    }

    @Override
    public List<CommerceExperienceFragmentContainer> getExperienceFragments() {
        return fragments;
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = getCategorySearchResultsSet().getRight();

            List<SearchAggregation> searchAggregations = searchResultsSet.getSearchAggregations()
                .stream()
                .filter(searchAggregation -> !SearchOptionsImpl.CATEGORY_UID_PARAMETER_ID
                    .equals(searchAggregation.getIdentifier()))
                .collect(Collectors.toList());

            CategoryTree categoryTree = (CategoryTree) getCategorySearchResultsSet().getLeft();
            processCategoryAggregation(searchAggregations, categoryTree);

            ((SearchResultsSetImpl) searchResultsSet).setSearchAggregations(searchAggregations);
        }
        return searchResultsSet;
    }

    private void processCategoryAggregation(List<SearchAggregation> searchAggregations, CategoryTree categoryTree) {
        if (categoryTree != null && categoryTree.getChildren() != null) {
            List<CategoryTree> childCategories = categoryTree.getChildren();
            searchAggregations.stream()
                .filter(aggregation -> CATEGORY_AGGREGATION_ID.equals(aggregation.getIdentifier())).findAny()
                .ifPresent(categoryAggregation -> {
                    List<SearchAggregationOption> options = categoryAggregation.getOptions();

                    // find and process category aggregation options related to child categories of
                    // current category
                    List<SearchAggregationOption> filteredOptions = options.stream().map(
                        option -> option instanceof SearchAggregationOptionImpl
                            ? (SearchAggregationOptionImpl) option
                            : new SearchAggregationOptionImpl(option))
                        .filter(option -> {
                            Optional<CategoryTree> categoryRef = childCategories.stream()
                                .filter(c -> String.valueOf(c.getId()).equals(
                                    option.getFilterValue()))
                                .findAny();

                            if (categoryRef.isPresent()) {
                                CategoryTree category = categoryRef.get();
                                CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
                                params.setUid(category.getUid().toString());
                                params.setUrlKey(category.getUrlKey());
                                params.setUrlPath(category.getUrlPath());
                                option.setPageUrl(urlProvider.toCategoryUrl(request, currentPage, params));
                                option.getAddFilterMap().remove(CATEGORY_AGGREGATION_ID);
                                return true;
                            } else {
                                return false;
                            }
                        }).collect(Collectors.toList());

                    // keep filtered options only or remove category aggregation if no option was
                    // found
                    if (filteredOptions.isEmpty()) {
                        searchAggregations.removeIf(a -> CATEGORY_AGGREGATION_ID.equals(a.getIdentifier()));
                    } else {
                        options.clear();
                        options.addAll(filteredOptions);
                        // move category aggregation to front
                        searchAggregations.stream().filter(a -> CATEGORY_AGGREGATION_ID.equals(a.getIdentifier()))
                            .findAny().ifPresent(
                                aggregation -> {
                                    searchAggregations.remove(aggregation);
                                    searchAggregations.add(0, aggregation);
                                });
                    }
                });
        } else {
            searchAggregations.removeIf(a -> CATEGORY_AGGREGATION_ID.equals(a.getIdentifier()));
        }
    }

    private Pair<CategoryInterface, SearchResultsSet> getCategorySearchResultsSet() {
        if (categorySearchResultsSet == null) {
            Consumer<ProductInterfaceQuery> productQueryHook = categoryRetriever != null
                ? categoryRetriever.getProductQueryHook()
                : null;
            categorySearchResultsSet = searchResultsService
                .performSearch(searchOptions, resource, currentPage, request, productQueryHook, categoryRetriever);
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
            CategoryInterface category = getCategory();
            SitemapLinkExternalizerProvider sitemapLinkExternalizerProvider = sling
                .getService(SitemapLinkExternalizerProvider.class);

            if (category != null && sitemapLinkExternalizerProvider != null) {
                canonicalUrl = sitemapLinkExternalizerProvider.getExternalizer(request.getResourceResolver())
                    .toExternalCategoryUrl(request, currentPage, new CategoryUrlFormat.Params(category));
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
