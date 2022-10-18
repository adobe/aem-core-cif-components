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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
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
import com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment.CommerceExperienceFragmentContainerImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment.CommerceExperienceFragmentImpl;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcollection.ProductCollectionImpl;
import com.adobe.cq.commerce.core.components.internal.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.adobe.cq.commerce.core.components.internal.services.sitemap.SitemapLinkExternalizerProvider;
import com.adobe.cq.commerce.core.components.internal.storefrontcontext.CategoryStorefrontContextImpl;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragmentContainer;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
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
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl extends ProductCollectionImpl implements ProductList {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productlist/v1/productlist";
    public static final String PN_FRAGMENT_LOCATION = "fragmentLocation";
    public static final String PN_FRAGMENT_CSS_CLASS = "fragmentCssClass";
    public static final String PN_FRAGMENT_PAGE = "fragmentPage";
    protected static final String PLACEHOLDER_DATA = "productlist-component-placeholder-data.json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);
    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final String CATEGORY_PROPERTY = "category";
    static final String CATEGORY_AGGREGATION_ID = "category_id";

    static Optional<CategoryInterface> PLACEHOLDER_CATEGORY;

    static CategoryInterface getPlaceholderCategory() {
        if (PLACEHOLDER_CATEGORY == null) {
            try {
                InputStream data = ProductListImpl.class.getClassLoader().getResourceAsStream(PLACEHOLDER_DATA);
                if (data != null) {
                    String json = IOUtils.toString(data, StandardCharsets.UTF_8);
                    Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
                    PLACEHOLDER_CATEGORY = Optional.of(rootQuery.getCategory());
                } else {
                    LOGGER.warn("Could not find placeholder data on classpath: {}", PLACEHOLDER_DATA);
                    PLACEHOLDER_CATEGORY = Optional.empty();
                }
            } catch (IOException ex) {
                LOGGER.warn("Could not load placeholder data", ex);
                PLACEHOLDER_CATEGORY = Optional.empty();
            }
        }
        return PLACEHOLDER_CATEGORY.orElse(null);
    }

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
    protected List<CommerceExperienceFragmentContainer> fragments;

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

        if (magentoGraphqlClient != null) {
            if (StringUtils.isNotBlank(categoryUid)) {
                categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryUid);
            } else {
                UnaryOperator<CategoryFilterInput> hook = urlProvider.getCategoryFilterHook(request);
                if (hook != null) {
                    categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
                    categoryRetriever.extendCategoryFilterWith(hook);
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
        return getSearchResultsSet().getProductListItems();
    }

    @Override
    public List<CommerceExperienceFragmentContainer> getExperienceFragments() {
        if (fragments == null) {
            CategoryInterface categoryInterface = getCategory();

            if (usePlaceholderData || categoryInterface == null) {
                // when using placeholder data or when the category does not exist / the category uid is unknown, we can skip the logic
                // to get the fragments completely
                fragments = Collections.emptyList();
                return fragments;
            }

            Resource fragmentsNode = resource.getChild(ProductList.NN_FRAGMENTS);
            int currentPageIndex = searchOptions.getCurrentPage();
            String categoryUidToSearchFor = categoryInterface.getUid().toString();
            fragments = new ArrayList<>();

            if (fragmentsNode != null && fragmentsNode.hasChildren()) {
                for (Resource fragment : fragmentsNode.getChildren()) {
                    ValueMap fragmentVm = fragment.getValueMap();
                    Integer fragmentPage = fragmentVm.get(PN_FRAGMENT_PAGE, -1);

                    if (!fragmentPage.equals(currentPageIndex)) {
                        continue;
                    }

                    String fragmentCssClass = fragment.getValueMap().get(PN_FRAGMENT_CSS_CLASS, String.class);
                    ValueMapResourceWrapper resourceWrapper = new ValueMapResourceWrapper(fragment,
                        CommerceExperienceFragmentImpl.RESOURCE_TYPE);
                    String fragmentLocation = fragment.getValueMap().get(PN_FRAGMENT_LOCATION, String.class);
                    resourceWrapper.getValueMap().put(PN_FRAGMENT_LOCATION, fragmentLocation);

                    if (!fragmentsRetriever.getExperienceFragmentsForCategory(categoryUidToSearchFor, fragmentLocation, currentPage)
                        .isEmpty()) {
                        fragments.add(new CommerceExperienceFragmentContainerImpl(resourceWrapper,
                            fragmentCssClass));
                    }
                }
            }
        }

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
            if (categoryRetriever != null) {
                // the retriever may be null, for example if there is no category information in the url
                categorySearchResultsSet = searchResultsService.performSearch(searchOptions, resource, currentPage, request,
                    categoryRetriever.getProductQueryHook(), productAttributeFilterHook, categoryRetriever);
            }
            if (categorySearchResultsSet == null || categorySearchResultsSet.getLeft() == null) {
                // category not found
                setPlaceholderData();
            }
            if (categorySearchResultsSet == null) {
                // fallback
                categorySearchResultsSet = Pair.of(null, new SearchResultsSetImpl());
            }
        }
        return categorySearchResultsSet;
    }

    private void setPlaceholderData() {
        // this logic is to preserve existing behaviour. we could show placeholder data also if there is no gql client
        if (isAuthor && magentoGraphqlClient != null) {
            usePlaceholderData = true;

            CategoryInterface placeholderCategory = getPlaceholderCategory();

            if (placeholderCategory != null) {
                SearchResultsSetImpl searchResultsSetImpl = new SearchResultsSetImpl();
                CategoryProducts categoryProducts = placeholderCategory.getProducts();
                ProductToProductListItemConverter converter = new ProductToProductListItemConverter(currentPage, request,
                    urlProvider, getId(),
                    placeholderCategory);
                List<ProductListItem> productListItems = categoryProducts.getItems().stream()
                    .map(converter)
                    .filter(Objects::nonNull) // the converter returns null if the conversion fails
                    .collect(Collectors.toList());

                searchResultsSetImpl.setProductListItems(productListItems);
                searchResultsSet = searchResultsSetImpl;
                categorySearchResultsSet = Pair.of(placeholderCategory, searchResultsSetImpl);
            }
        }
    }

    protected CategoryInterface getCategory() {
        return getCategorySearchResultsSet().getLeft();
    }

    @Override
    public boolean loadClientPrice() {
        // make sure we query first to see if we show placeholder data or not
        // usePlaceholderData is set as side effect of getCategorySearchResultsSet
        getCategorySearchResultsSet();
        return !usePlaceholderData && super.loadClientPrice();
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
