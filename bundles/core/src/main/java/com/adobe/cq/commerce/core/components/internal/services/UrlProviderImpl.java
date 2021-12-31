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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
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
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageUrlFormatAdapter;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.CategoryPageWithUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageUrlFormatAdapter;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSku;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithSkuAndUrlPath;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlKey;
import com.adobe.cq.commerce.core.components.internal.services.urlformats.ProductPageWithUrlPath;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.day.cq.wcm.api.Page;

@Component(service = { UrlProvider.class, UrlProviderImpl.class })
@Designate(ocd = UrlProviderConfiguration.class)
public class UrlProviderImpl implements UrlProvider {

    /**
     * The attribute name of the request attribute holding a previously resolved identifier of the request. Only set after either of
     * {@link UrlProviderImpl#getCategoryIdentifier(SlingHttpServletRequest)} or
     * {@link UrlProviderImpl#getProductIdentifier(SlingHttpServletRequest)} has been called before.
     */
    public static final String CIF_IDENTIFIER_ATTR = "cif.identifier";

    /**
     * A {@link Map} of default patterns for product pages supported by the default implementation of
     * {@link UrlProvider}.
     */
    public static final Map<String, ProductUrlFormat> DEFAULT_PRODUCT_URL_FORMATS = new HashMap<String, ProductUrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
            put(ProductPageWithSkuAndUrlKey.PATTERN, ProductPageWithSkuAndUrlKey.INSTANCE);
            put(ProductPageWithUrlPath.PATTERN, ProductPageWithUrlPath.INSTANCE);
            put(ProductPageWithSkuAndUrlPath.PATTERN, ProductPageWithSkuAndUrlPath.INSTANCE);
        }
    };

    /**
     * A {@link Map} of default patterns for category pages supported by the default implementation of
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
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + UrlFormat.PROP_USE_AS + "=" + UrlFormat.PRODUCT_PAGE_URL_FORMAT + ")")
    private UrlFormat productPageUrlFormat;
    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY,
        target = "(" + UrlFormat.PROP_USE_AS + "=" + UrlFormat.CATEGORY_PAGE_URL_FORMAT + ")")
    private UrlFormat categoryPageUrlFormat;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY)
    private ProductUrlFormat newProductUrlFormat;

    @Reference(
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY)
    private CategoryUrlFormat newCategoryUrlFormat;

    @Reference
    private SpecificPageStrategy specificPageStrategy;

    @Activate
    public void activate(UrlProviderConfiguration conf) {
        if (newProductUrlFormat == null) {
            if (productPageUrlFormat != null) {
                newProductUrlFormat = new ProductPageUrlFormatAdapter(productPageUrlFormat);
            } else {
                newProductUrlFormat = DEFAULT_PRODUCT_URL_FORMATS
                    .getOrDefault(conf.productPageUrlFormat(), ProductPageWithUrlKey.INSTANCE);
            }
        }
        if (newCategoryUrlFormat == null) {
            if (categoryPageUrlFormat != null) {
                newCategoryUrlFormat = new CategoryPageUrlFormatAdapter(categoryPageUrlFormat);
            } else {
                newCategoryUrlFormat = DEFAULT_CATEGORY_URL_FORMATS
                    .getOrDefault(conf.categoryPageUrlFormat(), CategoryPageWithUrlPath.INSTANCE);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        productPageUrlFormat = null;
        newProductUrlFormat = null;
        categoryPageUrlFormat = null;
        newCategoryUrlFormat = null;
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toProductUrl(request, page, new ProductUrlFormat.Params(params));
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, String productIdentifier) {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        if (StringUtils.isNotBlank(productIdentifier)) {
            params.setSku(productIdentifier);
            // assume that any other format then the ProductPageWithSku requires more parameters
            if (!(newProductUrlFormat instanceof ProductPageWithSku)) {
                MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
                if (magentoGraphqlClient != null) {
                    ProductUrlParameterRetriever retriever = new ProductUrlParameterRetriever(magentoGraphqlClient);
                    retriever.setIdentifier(productIdentifier);
                    ProductInterface product = retriever.fetchProduct();
                    if (product != null) {
                        params.setUrlKey(product.getUrlKey());
                        params.setUrlPath(product.getUrlPath());
                    } else {
                        LOGGER.debug("Could not generate product page URL for {}.", productIdentifier);
                    }
                }
            }
        }
        return toProductUrl(request, page, params);
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, Page page, ProductUrlFormat.Params params) {
        if (page != null) {
            Map<String, String> paramsMap = params.asMap();
            Set<String> searchValues = new HashSet<>();
            // compatible to the previous implementation, may be removed
            searchValues.addAll(paramsMap.values());

            String pageParam = getPageParam(page, params, specificPageStrategy::getSpecificPage);
            if (!pageParam.equals(params.getPage())) {
                params = new ProductUrlFormat.Params(params);
                params.setPage(pageParam);
            }
        }

        return newProductUrlFormat.format(params);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return toCategoryUrl(request, page, new CategoryUrlFormat.Params(params));
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, String categoryIdentifier) {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid(categoryIdentifier);

        MagentoGraphqlClient magentoGraphqlClient = request != null ? request.adaptTo(MagentoGraphqlClient.class) : null;
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
    public String toCategoryUrl(SlingHttpServletRequest request, @Nullable Page page, CategoryUrlFormat.Params params) {
        if (page != null) {
            Map<String, String> paramsMap = params.asMap();
            Set<String> searchValues = new HashSet<>();
            // compatible to the previous implementation, may be removed
            searchValues.addAll(paramsMap.values());
            String pageParam = getPageParam(page, params, specificPageStrategy::getSpecificPage);
            if (!pageParam.equals(params.getPage())) {
                params = new CategoryUrlFormat.Params(params);
                params.setPage(pageParam);
            }
        }

        return newCategoryUrlFormat.format(params);
    }

    private <T> String getPageParam(Page page, T params, BiFunction<Page, T, Page> specificPageSelector) {
        // enable rendering of deep links only on author
        boolean deepLinkSpecificPages = specificPageStrategy.isGenerateSpecificPageUrlsEnabled();

        if (deepLinkSpecificPages) {
            Page subPage = specificPageSelector.apply(page, params);
            if (subPage != null) {
                return subPage.getPath();
            }
        }

        return page.getPath();
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

        ProductUrlFormat.Params productIdentifiers = newProductUrlFormat.parse(request.getRequestPathInfo(),
            request.getRequestParameterMap());

        // if we get the product sku from URL no extra lookup is needed
        if (StringUtils.isNotEmpty(productIdentifiers.getSku())) {
            identifier = productIdentifiers.getSku();
        } else if (StringUtils.isNotEmpty(productIdentifiers.getUrlKey())) {
            // lookup internal product identifier (sku) based on URL product identifier (url_key)
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient != null) {
                UrlToProductRetriever productRetriever = new UrlToProductRetriever(magentoGraphqlClient);
                productRetriever.setIdentifier(productIdentifiers.getUrlKey());
                ProductInterface product = productRetriever.fetchProduct();
                identifier = product != null ? product.getSku() : null;
            } else {
                LOGGER.warn("No backend GraphQL client provided, cannot retrieve product identifier for {}", request.getRequestURL()
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

        CategoryUrlFormat.Params categoryIdentifiers = newCategoryUrlFormat
            .parse(request.getRequestPathInfo(), request.getRequestParameterMap());

        if (StringUtils.isNotEmpty(categoryIdentifiers.getUid())) {
            identifier = categoryIdentifiers.getUid();
        } else if (StringUtils.isNotEmpty(categoryIdentifiers.getUrlKey())) {
            // lookup internal product identifier (sku) based on URL product identifier (url_key)
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient != null) {
                UrlToCategoryRetriever categoryRetriever = new UrlToCategoryRetriever(magentoGraphqlClient);
                categoryRetriever.setIdentifier(categoryIdentifiers.getUrlKey());
                CategoryInterface category = categoryRetriever.fetchCategory();
                identifier = category != null ? category.getUid().toString() : null;
            } else {
                LOGGER.warn("No backend GraphQL client provided, cannot retrieve product identifier for {}", request.getRequestURL()
                    .toString());
            }
        }

        if (identifier != null) {
            request.setAttribute(CIF_IDENTIFIER_ATTR, identifier);
        }

        return identifier;
    }

    /**
     * When the FragmentRenderService executes an internal request it passes its configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was configured we have to pass the identifier parsed from the
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
     * When the FragmentRenderService executes an internal request it passes its configuration as attribute to the internal request.
     * As this request may not be formatted in the way the UrlProvider was configured we have to pass the identifier parsed from the
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

    /**
     * Parses and returns the product sku or url_key used in the given Sling HTTP request based on the URLProvider configuration for product
     * page URLs.
     *
     * @param request The current Sling HTTP request.
     * @return The product sku or url_key from the URL.
     */
    public ProductUrlFormat.Params parseProductUrlIdentifier(SlingHttpServletRequest request) {
        return newProductUrlFormat.parse(request.getRequestPathInfo(), request.getRequestParameterMap());
    }

    /**
     * Parses and returns the category url_path used in the given Sling HTTP request based on the URLProvider configuration for product
     * page URLs.
     *
     * @param request The current Sling HTTP request.
     * @return The category url_path from the URL.
     */
    public CategoryUrlFormat.Params parseCategoryUrlIdentifier(SlingHttpServletRequest request) {
        return newCategoryUrlFormat.parse(request.getRequestPathInfo(), request.getRequestParameterMap());
    }
}
