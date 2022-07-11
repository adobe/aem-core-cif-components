/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.site.SiteStructureFactory;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageUrlFormatAdapter;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageUrlFormatAdapter;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithCategoryAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuCategoryAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.GenericUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.scripting.WCMBindingsConstants;

@Component(service = { UrlProvider.class, UrlProviderImpl.class })
@Designate(ocd = UrlProviderConfiguration.class)
public class UrlProviderImpl implements UrlProvider {

    /**
     * The attribute name of the request attribute holding a previously resolved
     * identifier of the request. Only set after either of
     * {@link UrlProviderImpl#getCategoryIdentifier(SlingHttpServletRequest)} or
     * {@link UrlProviderImpl#getProductIdentifier(SlingHttpServletRequest)} has
     * been called before.
     */
    public static final String CIF_IDENTIFIER_ATTR = "cif.identifier";

    static final String PN_PRODUCT_PAGE_URL_FORMAT = "productPageUrlFormat";
    static final String PN_CATEGORY_PAGE_URL_FORMAT = "categoryPageUrlFormat";

    /**
     * A {@link Map} of default patterns for product pages supported by the default
     * implementation of
     * {@link UrlProvider}.
     */
    public static final Map<String, ProductUrlFormat> DEFAULT_PRODUCT_URL_FORMATS = new HashMap<String, ProductUrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
            put(ProductPageWithSkuAndUrlKey.PATTERN, ProductPageWithSkuAndUrlKey.INSTANCE);
            put(ProductPageWithUrlPath.PATTERN, ProductPageWithUrlPath.INSTANCE);
            put(ProductPageWithSkuAndUrlPath.PATTERN, ProductPageWithSkuAndUrlPath.INSTANCE);
            put(ProductPageWithCategoryAndUrlKey.PATTERN, ProductPageWithCategoryAndUrlKey.INSTANCE);
            put(ProductPageWithSkuCategoryAndUrlKey.PATTERN, ProductPageWithSkuCategoryAndUrlKey.INSTANCE);
        }
    };

    /**
     * A {@link Map} of default patterns for category pages supported by the default
     * implementation of
     * {@link UrlProvider}.
     */
    public static final Map<String, CategoryUrlFormat> DEFAULT_CATEGORY_URL_FORMATS = new HashMap<String, CategoryUrlFormat>() {
        {
            put(CategoryPageWithUrlPath.PATTERN, CategoryPageWithUrlPath.INSTANCE);
            put(CategoryPageWithUrlKey.PATTERN, CategoryPageWithUrlKey.INSTANCE);
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlProviderImpl.class);

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + UrlFormat.PROP_USE_AS + "=" + UrlFormat.PRODUCT_PAGE_URL_FORMAT + ")")
    private List<UrlFormat> productPageUrlFormat;

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + UrlFormat.PROP_USE_AS + "=" + UrlFormat.CATEGORY_PAGE_URL_FORMAT + ")")
    private List<UrlFormat> categoryPageUrlFormat;

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY)
    private List<ProductUrlFormat> newProductUrlFormat;

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY)
    private List<CategoryUrlFormat> newCategoryUrlFormat;

    @Reference
    private SpecificPageStrategy specificPageStrategy;
    @Reference
    private PageManagerFactory pageManagerFactory;
    @Reference
    private SiteStructureFactory siteStructureFactory;

    private boolean enableContextAwareProductUrls;

    private ProductUrlFormat systemDefaultProductUrlFormat;
    private CategoryUrlFormat systemDefaultCategoryUrlFormat;

    @Activate
    public void activate(UrlProviderConfiguration conf) {
        if (CollectionUtils.isEmpty(newProductUrlFormat)) {
            if (CollectionUtils.isNotEmpty(productPageUrlFormat)) {
                systemDefaultProductUrlFormat = new ProductPageUrlFormatAdapter(productPageUrlFormat.get(0));
            } else {
                systemDefaultProductUrlFormat = DEFAULT_PRODUCT_URL_FORMATS
                    .getOrDefault(conf.productPageUrlFormat(), ProductPageWithUrlKey.INSTANCE);
            }
        } else {
            systemDefaultProductUrlFormat = newProductUrlFormat.get(0);
        }

        if (CollectionUtils.isEmpty(newCategoryUrlFormat)) {
            if (CollectionUtils.isNotEmpty(categoryPageUrlFormat)) {
                systemDefaultCategoryUrlFormat = new CategoryPageUrlFormatAdapter(categoryPageUrlFormat.get(0));
            } else {
                systemDefaultCategoryUrlFormat = DEFAULT_CATEGORY_URL_FORMATS
                    .getOrDefault(conf.categoryPageUrlFormat(), CategoryPageWithUrlPath.INSTANCE);
            }
        } else {
            systemDefaultCategoryUrlFormat = newCategoryUrlFormat.get(0);
        }

        enableContextAwareProductUrls = conf.enableContextAwareProductUrls();
    }

    @Deactivate
    protected void deactivate() {
        productPageUrlFormat = null;
        newProductUrlFormat = null;
        categoryPageUrlFormat = null;
        newCategoryUrlFormat = null;
        systemDefaultCategoryUrlFormat = null;
        systemDefaultProductUrlFormat = null;
    }

    private static <T> T getUrlFormatFromContext(SlingHttpServletRequest request, Page page, String propertyName,
        Map<String, T> defaultUrlFormats, T defaultUrlFormat, List<UrlFormat> urlFormats, List<T> newUrlFormats,
        Function<UrlFormat, T> adapter) {

        if (request == null && page == null) {
            throw new IllegalArgumentException("The request and the page parameters cannot both be null");
        }

        Resource resource = page != null ? page.getContentResource() : request.getResource();

        ComponentsConfiguration properties = resource.adaptTo(ComponentsConfiguration.class);

        if (properties != null) {
            String formatPattern = properties.get(propertyName, String.class);

            if (StringUtils.isNotBlank(formatPattern)) {
                if (defaultUrlFormats.containsKey(formatPattern)) {
                    return defaultUrlFormats.get(formatPattern);
                }

                // find a new format if applicable
                for (T newUrlFormat : newUrlFormats) {
                    if (newUrlFormat.getClass().getName().equals(formatPattern)) {
                        return newUrlFormat;
                    }
                }

                // try to find legacy pattern
                for (UrlFormat legacyUrlFormat : urlFormats) {
                    if (legacyUrlFormat.getClass().getName().equals(formatPattern)) {
                        return adapter.apply(legacyUrlFormat);
                    }
                }
            }
        }

        return defaultUrlFormat;
    }

    private ProductUrlFormat getProductUrlFormatFromContext(Page page) {
        return getProductUrlFormatFromContext(null, page);
    }

    private ProductUrlFormat getProductUrlFormatFromContext(SlingHttpServletRequest request, Page page) {
        return getUrlFormatFromContext(request, page, PN_PRODUCT_PAGE_URL_FORMAT, DEFAULT_PRODUCT_URL_FORMATS,
            systemDefaultProductUrlFormat, productPageUrlFormat, newProductUrlFormat, ProductPageUrlFormatAdapter::new);
    }

    private CategoryUrlFormat getCategoryUrlFormatFromContext(Page page) {
        return getCategoryUrlFormatFromContext(null, page);
    }

    private CategoryUrlFormat getCategoryUrlFormatFromContext(SlingHttpServletRequest request, Page page) {
        return getUrlFormatFromContext(request, page, PN_CATEGORY_PAGE_URL_FORMAT, DEFAULT_CATEGORY_URL_FORMATS,
            systemDefaultCategoryUrlFormat, categoryPageUrlFormat, newCategoryUrlFormat,
            CategoryPageUrlFormatAdapter::new);
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toProductUrl(request, page, new ProductUrlFormat.Params(params));
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, String productIdentifier) {
        ProductUrlFormat.Params params = null;
        if (StringUtils.isNotBlank(productIdentifier)) {
            // assume that any other format then the ProductPageWithSku requires more
            // parameters
            if (!(getProductUrlFormatFromContext(request, page) instanceof ProductPageWithSku)) {
                MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
                if (magentoGraphqlClient != null) {
                    ProductUrlParameterRetriever retriever = new ProductUrlParameterRetriever(magentoGraphqlClient);
                    retriever.setIdentifier(productIdentifier);
                    ProductInterface product = retriever.fetchProduct();
                    if (product != null) {
                        params = new ProductUrlFormat.Params(product);
                        params.setSku(productIdentifier);
                    } else {
                        LOGGER.debug("Could not generate product page URL for {}.", productIdentifier);
                    }
                }
            }
        }

        if (params == null) {
            params = new ProductUrlFormat.Params();
            params.setSku(productIdentifier);
        }

        return toProductUrl(request, page, params);
    }

    @Override
    public String toProductUrl(@Nullable SlingHttpServletRequest request, Page givenPage, ProductUrlFormat.Params params) {
        ProductUrlFormat.Params copy = new ProductUrlFormat.Params(params);
        ProductUrlFormat productUrlFormat = getProductUrlFormatFromContext(request, givenPage);
        SiteStructure siteStructure = null;

        if (enableContextAwareProductUrls) {
            if (params.getCategoryUrlParams().getUrlKey() == null && params.getCategoryUrlParams().getUrlPath() == null) {
                siteStructure = siteStructureFactory.getSiteStructure(request, givenPage);
                // if there is no category context given for the product parameters, try to retain them from the current page. That may be a
                // product page or a category page. Both may encode the category context in the url. A use case for that would be for
                // example
                // a related products component on a product page, that does not know about the category context but should link to related
                // products in the same category if applicable.

                // TODO: target to be refactored with 3.0 (CIF-2634)
                // currently the UrlProvider accepts a page parameter, which is a product page according to SiteNavigation#getProductPage
                // for all CIF Components. It would be more helpful if this is actually the currentPage as we can select the product page
                // from there anyway. This will be a breaking change.
                SlingBindings slingBindings = request != null ? (SlingBindings) request.getAttribute(SlingBindings.class.getName()) : null;
                String categoryUrlKey = null;
                String categoryUrlPath = null;
                if (slingBindings != null) {
                    Page currentPage = (Page) slingBindings.get(WCMBindingsConstants.NAME_CURRENT_PAGE);
                    if (currentPage != null) {
                        if (siteStructure.isProductPage(currentPage)) {
                            ProductUrlFormat.Params parseParams = parseProductUrlFormatParameters(request);
                            categoryUrlKey = parseParams.getCategoryUrlParams().getUrlKey();
                            categoryUrlPath = parseParams.getCategoryUrlParams().getUrlPath();
                        } else if (siteStructure.isCategoryPage(currentPage)) {
                            CategoryUrlFormat.Params parsedParams = parseCategoryUrlFormatParameters(request);
                            categoryUrlKey = parsedParams.getUrlKey();
                            categoryUrlPath = parsedParams.getUrlPath();
                        }
                    }
                }
                if (categoryUrlKey != null || categoryUrlPath != null) {
                    copy.getCategoryUrlParams().setUrlKey(categoryUrlKey);
                    copy.getCategoryUrlParams().setUrlPath(categoryUrlPath);
                }
            }
        } else {
            if (params.getCategoryUrlParams().getUrlKey() != null && params.getCategoryUrlParams().getUrlPath() != null) {
                // unset the category context to enforce canonical urls to be returned by the formats
                // see UrlFormatBase.selectUrlPath()
                copy.getCategoryUrlParams().setUrlKey(null);
                copy.getCategoryUrlParams().setUrlPath(null);
            }
        }

        if (givenPage != null) {
            if (siteStructure == null) {
                siteStructure = siteStructureFactory.getSiteStructure(request, givenPage);
            }

            Pair<Page, ProductUrlFormat> pair = getSpecificPageAndFormat(siteStructure, copy,
                this::getProductUrlFormatFromContext, specificPageStrategy::getGenericPage, specificPageStrategy::getSpecificPage);

            if (pair != null) {
                Page specificPage = pair.getLeft();
                productUrlFormat = pair.getRight();
                copy.setPage(specificPage.getPath());
            }
        }

        return productUrlFormat.format(copy);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toCategoryUrl(request, page, new CategoryUrlFormat.Params(params));
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, String categoryIdentifier) {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid(categoryIdentifier);

        MagentoGraphqlClient magentoGraphqlClient = request != null ? request.adaptTo(MagentoGraphqlClient.class)
            : null;
        if (magentoGraphqlClient != null && StringUtils.isNotBlank(categoryIdentifier)) {
            CategoryUrlParameterRetriever retriever = new CategoryUrlParameterRetriever(magentoGraphqlClient);
            retriever.setIdentifier(categoryIdentifier);
            CategoryInterface category = retriever.fetchCategory();
            if (category != null) {
                params.setUrlKey(category.getUrlKey());
                params.setUrlPath(category.getUrlPath());
            } else {
                LOGGER.debug("Could not generate category page URL for {}.", categoryIdentifier);
            }
        }
        return toCategoryUrl(request, page, params);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, @Nullable Page givenPage, CategoryUrlFormat.Params params) {
        CategoryUrlFormat categoryUrlFormat = getCategoryUrlFormatFromContext(request, givenPage);
        if (givenPage != null) {
            SiteStructure siteStructure = siteStructureFactory.getSiteStructure(request, givenPage);
            Pair<Page, CategoryUrlFormat> pair = getSpecificPageAndFormat(siteStructure, params,
                this::getCategoryUrlFormatFromContext, specificPageStrategy::getGenericPage, specificPageStrategy::getSpecificPage);

            if (pair != null) {
                Page specificPage = pair.getLeft();
                categoryUrlFormat = pair.getRight();

                if (!specificPage.getPath().equals(params.getPage())) {
                    params = new CategoryUrlFormat.Params(params);
                    params.setPage(specificPage.getPath());
                }
            }
        }

        return categoryUrlFormat.format(params);
    }

    /**
     * Resolves the right page and its url format from the given site structure.
     * <p>
     * If deep linking is enabled and a specific page was found for the given parameters, the specific page is returned together with the
     * format of the search root it is located in. If not the first matching generic page and its format is returned.
     *
     * @param siteStructure the {@link SiteStructure}
     * @param params the parameters to search a search root / specific page for
     * @param formatSelector a {@link Function} that maps a page to a format
     * @param specificPageSelector a {@link BiFunction} that returns a specific page for a given search root and parameters
     * @param <T> the type of parameters passed
     * @param <F> the type of {@link GenericUrlFormat} to return
     * @return
     */
    private <T, F extends GenericUrlFormat<T>> Pair<Page, F> getSpecificPageAndFormat(SiteStructure siteStructure, T params,
        Function<Page, F> formatSelector, BiFunction<SiteStructure, T, Page> genericPageSelector,
        BiFunction<Page, T, Page> specificPageSelector) {
        Page genericPage = genericPageSelector.apply(siteStructure, params);

        if (genericPage != null) {
            boolean deepLinkSpecificPages = specificPageStrategy.isGenerateSpecificPageUrlsEnabled();
            // A search root may have a different cloud configuration set to configure features like the url provider format. We should
            // take the format that is relevant for the search root.
            F searchRootFormat = formatSelector.apply(genericPage);

            // if deep linking is enabled, try to return the path of the specific page
            if (deepLinkSpecificPages) {
                Page specificPage = specificPageSelector.apply(genericPage, searchRootFormat.retainParsableParameters(params));
                if (specificPage != null) {
                    return Pair.of(specificPage, searchRootFormat);
                }
            }

            return Pair.of(genericPage, searchRootFormat);
        }

        return null;
    }

    @Override
    public String getProductIdentifier(SlingHttpServletRequest request) {
        String identifier = getIdentifierFromRequest(request);
        if (identifier != null) {
            return identifier;
        }

        identifier = getIdentifierFromFragmentRenderRequest(request);
        if (identifier != null) {
            return identifier;
        }
        Page page = getCurrentPage(request);
        ProductUrlFormat.Params productIdentifiers = getProductUrlFormatFromContext(request, page).parse(
            request.getRequestPathInfo(),
            request.getRequestParameterMap());

        // if we get the product sku from URL no extra lookup is needed
        if (StringUtils.isNotEmpty(productIdentifiers.getSku())) {
            identifier = productIdentifiers.getSku();
        } else if (StringUtils.isNotEmpty(productIdentifiers.getUrlKey())) {
            // lookup internal product identifier (sku) based on URL product identifier
            // (url_key)
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient != null) {
                UrlToProductRetriever productRetriever = new UrlToProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(productIdentifiers.getUrlKey());
                ProductInterface product = productRetriever.fetchProduct();
                identifier = product != null ? product.getSku() : null;
            } else {
                LOGGER.warn("No backend GraphQL client provided, cannot retrieve product identifier for {}",
                    request.getRequestURL()
                        .toString());
            }
        }

        if (identifier != null) {
            request.setAttribute(CIF_IDENTIFIER_ATTR, identifier);
        }

        return identifier;
    }

    @Override
    public String getCategoryIdentifier(SlingHttpServletRequest request) {
        String identifier = getIdentifierFromRequest(request);
        if (identifier != null) {
            return identifier;
        }

        identifier = getIdentifierFromFragmentRenderRequest(request);
        if (identifier != null) {
            return identifier;
        }

        Page page = getCurrentPage(request);
        CategoryUrlFormat.Params categoryIdentifiers = getCategoryUrlFormatFromContext(request, page)
            .parse(request.getRequestPathInfo(), request.getRequestParameterMap());

        if (StringUtils.isNotEmpty(categoryIdentifiers.getUid())) {
            identifier = categoryIdentifiers.getUid();
        } else {
            // lookup internal category identifier (uid) based on URL category identifier (url_path or url_key, depending on the
            // url format)
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient == null) {
                LOGGER.warn("No backend GraphQL client provided, cannot retrieve category identifier for {}", request.getRequestURL()
                    .toString());
                return null;
            }

            UrlToCategoryRetriever categoryRetriever = null;

            if (StringUtils.isNotEmpty(categoryIdentifiers.getUrlPath())) {
                categoryRetriever = new UrlToCategoryRetriever.ByUrlPath(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryIdentifiers.getUrlPath());
            } else if (StringUtils.isNotEmpty(categoryIdentifiers.getUrlKey())) {
                categoryRetriever = new UrlToCategoryRetriever.ByUrlKey(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryIdentifiers.getUrlKey());
            }

            CategoryInterface category = categoryRetriever != null ? categoryRetriever.fetchCategory() : null;
            identifier = category != null ? category.getUid().toString() : null;
        }

        if (identifier != null) {
            request.setAttribute(CIF_IDENTIFIER_ATTR, identifier);
        }

        return identifier;
    }

    /**
     * When the FragmentRenderService executes an internal request it passes its
     * configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was
     * configured we have to pass the identifier parsed from the
     * original request as attribute in this configuration.
     *
     * @param request
     * @return
     */
    private String getIdentifierFromFragmentRenderRequest(SlingHttpServletRequest request) {
        Object fragmentRenderConfig = request.getAttribute(FragmentRenderService.class.getName() + ".config");
        if (fragmentRenderConfig instanceof ValueMap) {
            String identifier = ((ValueMap) fragmentRenderConfig).get(CIF_IDENTIFIER_ATTR, String.class);
            if (StringUtils.isNotEmpty(identifier)) {
                return identifier;
            }
        }

        return null;
    }

    /**
     * When the FragmentRenderService executes an internal request it passes its
     * configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was
     * configured we have to pass the identifier parsed from the
     * original request as attribute in this configuration.
     *
     * @param request
     * @return
     */
    private String getIdentifierFromRequest(SlingHttpServletRequest request) {
        Object cachedIdentifier = request.getAttribute(CIF_IDENTIFIER_ATTR);
        if (cachedIdentifier instanceof String) {
            return (String) cachedIdentifier;
        }
        return null;
    }

    @Override
    public ProductUrlFormat.Params parseProductUrlFormatParameters(SlingHttpServletRequest request) {
        Page page = getCurrentPage(request);
        ProductUrlFormat productUrlFormat = getProductUrlFormatFromContext(request, page);

        return productUrlFormat.parse(request.getRequestPathInfo(), request.getRequestParameterMap());
    }

    @Override
    public CategoryUrlFormat.Params parseCategoryUrlFormatParameters(SlingHttpServletRequest request) {
        Page page = getCurrentPage(request);
        CategoryUrlFormat categoryUrlFormat = getCategoryUrlFormatFromContext(request, page);

        return categoryUrlFormat.parse(request.getRequestPathInfo(), request.getRequestParameterMap());
    }

    private Page getCurrentPage(SlingHttpServletRequest request) {
        Page page = null;
        SlingBindings slingBindings = request != null
            ? (SlingBindings) request.getAttribute(SlingBindings.class.getName())
            : null;
        if (slingBindings != null) {
            page = (Page) slingBindings.get(WCMBindingsConstants.NAME_CURRENT_PAGE);
        } else {
            Resource resource = request.getResource();
            page = resource.adaptTo(Page.class);
            if (page == null) {
                PageManager pageManager = pageManagerFactory.getPageManager(resource.getResourceResolver());
                page = pageManager.getContainingPage(resource);
            }
        }

        return page;
    }
}
