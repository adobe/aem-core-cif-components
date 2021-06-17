/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.common.SitemapLinkExternalizer;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.sitemap.SitemapProductFilter;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;

@Component(
    service = SitemapGenerator.class,
    property = {
        Constants.SERVICE_RANKING + ":Integer=100"
    })
public class ProductsSitemapGenerator implements SitemapGenerator {

    @ObjectClassDefinition(name = "CIF Product Sitemap Generator")
    @interface Configuration {

        @AttributeDefinition(
            name = "Pagination Size",
            description = "The number of products to query from the commerce backend per iteration.")
        int pageSize() default 10;
    }

    private static final String PN_NEXT_PRODUCT = "nextProduct";
    private static final String PN_NEXT_PAGE = "nextPage";
    private static final String PN_MAXIMUM_PAGES = "maxPages";
    private static final Logger LOG = LoggerFactory.getLogger(ProductsSitemapGenerator.class);

    @Reference
    private SearchResultsService searchResultsService;
    @Reference
    private UrlProvider urlProvider;
    @Reference
    private SitemapLinkExternalizer externalizer;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapProductFilter productFilter;

    private int pageSize = 100;

    @Activate
    protected void activate(Configuration configuration) {
        this.pageSize = configuration.pageSize();
    }

    @Override
    public Set<String> getNames(Resource sitemapRoot) {
        Page page = sitemapRoot.adaptTo(Page.class);
        Page specificPage = page != null ? SiteNavigation.getProductPage(page) : null;
        return specificPage != null && specificPage.getPath().equals(page.getPath())
            ? Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME)
            : Collections.emptySet();
    }

    @Override
    public void generate(Resource sitemapRoot, String name, Sitemap sitemap, GenerationContext context) throws SitemapException {
        MagentoGraphqlClient graphql = MagentoGraphqlClient.create(sitemapRoot, sitemapRoot.adaptTo(Page.class), null);
        Page productPage = sitemapRoot.adaptTo(Page.class);

        if (graphql == null || productPage == null) {
            throw new SitemapException("Failed to build product sitemap at: " + sitemapRoot.getPath());
        }

        int currentIndex = context.getProperty(PN_NEXT_PRODUCT, 0);
        int currentPageIndex = context.getProperty(PN_NEXT_PAGE, 1);
        int maxPages = context.getProperty(PN_MAXIMUM_PAGES, Integer.MAX_VALUE);
        // parameter map to be reused while iterating
        UrlProvider.ParamsBuilder paramsBuilder = new UrlProvider.ParamsBuilder()
            .page(externalizer.externalize(sitemapRoot));

        while (currentPageIndex <= maxPages) {
            String query = Operations.query(productsQueryFor(currentPageIndex, pageSize)).toString();
            GraphqlResponse<Query, Error> resp = graphql.execute(query);

            if (resp.getErrors() != null && resp.getErrors().size() > 0) {
                SitemapException ex = new SitemapException("Failed to execute graphql query.");
                resp.getErrors().forEach(error -> ex.addSuppressed(new Exception(error.getMessage())));
                throw ex;
            }

            Products products = resp.getData().getProducts();
            List<ProductInterface> items = products.getItems();
            maxPages = products.getTotalCount() / pageSize;

            if (products.getTotalCount() % pageSize > 0) {
                // there is a fractional part of items on the last page
                maxPages++;
            }

            for (int i = currentIndex; i < items.size(); i++) {
                ProductInterface product = items.get(i);
                if (productFilter != null && !productFilter.shouldInclude(productPage, product)) {
                    LOG.debug("Ignore product {}, not allowed by filter: {}", product.getSku(), productFilter.getClass().getSimpleName());
                    continue;
                }
                // TODO: should we handle variants? Currently we ignore them as crawlers will ignore the fragment in the urls anyway.
                Map<String, String> params = paramsBuilder
                    .sku(product.getSku()).urlKey(product.getUrlKey())
                    .variantSku(null).variantUrlKey(null)
                    .map();
                sitemap.addUrl(urlProvider.toProductUrl(null, null, params));
                context.setProperty(PN_NEXT_PRODUCT, i + 1);
            }

            context.setProperty(PN_NEXT_PRODUCT, currentIndex = 0);
            context.setProperty(PN_NEXT_PAGE, ++currentPageIndex);
            context.setProperty(PN_MAXIMUM_PAGES, maxPages);
        }
    }

    private static QueryQueryDefinition productsQueryFor(int pageIndex, int pageSize) {
        return q -> q.products(
            arguments -> arguments
                // TODO: figure out a better way to query for all products
                .search(StringUtils.EMPTY)
                .pageSize(pageSize)
                .currentPage(pageIndex),
            resultSet -> resultSet
                .totalCount()
                .items(product -> product
                    .urlKey()
                    .sku()
                    .onConfigurableProduct(configurableProduct -> configurableProduct
                        .variants(variant -> variant.product(SimpleProductQuery::sku)))));
    }
}
