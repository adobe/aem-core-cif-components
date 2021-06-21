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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.sitemap.SitemapCategoryFilter;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.shopify.graphql.support.ID;

@Component(
    service = SitemapGenerator.class,
    property = {
        Constants.SERVICE_RANKING + ":Integer=100"
    })
public class CategoriesSitemapGenerator implements SitemapGenerator {

    @ObjectClassDefinition(name = "CIF Category Sitemap Generator")
    @interface Configuration {

        @AttributeDefinition(
            name = "Pagination Size",
            description = "The number of products to query from the commerce backend per iteration.")
        int pageSize() default 10;
    }

    private static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";
    private static final String PN_ENABLE_UID_SUPPORT = "enableUIDSupport";
    private static final String PN_PENDING_CATEGORIES = "pendingCategories";
    private static final Logger LOG = LoggerFactory.getLogger(CategoriesSitemapGenerator.class);

    @Reference
    private SearchResultsService searchResultsService;
    @Reference
    private UrlProvider urlProvider;
    @Reference
    private SitemapLinkExternalizer externalizer;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapCategoryFilter categoryFilter;

    private int pageSize = 100;

    @Activate
    protected void activate(Configuration configuration) {
        this.pageSize = configuration.pageSize();
    }

    @Override
    public Set<String> getNames(Resource sitemapRoot) {
        Page page = sitemapRoot.adaptTo(Page.class);
        Page specificPage = page != null ? SiteNavigation.getCategoryPage(page) : null;
        return specificPage != null && specificPage.getPath().equals(page.getPath())
            ? Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME)
            : Collections.emptySet();
    }

    @Override
    public void generate(Resource sitemapRoot, String name, Sitemap sitemap, GenerationContext context) throws SitemapException {
        MagentoGraphqlClient graphql = MagentoGraphqlClient.create(sitemapRoot, sitemapRoot.adaptTo(Page.class), null);
        Page categoryPage = sitemapRoot.adaptTo(Page.class);
        ComponentsConfiguration configuration = sitemapRoot.adaptTo(ComponentsConfiguration.class);

        if (graphql == null || categoryPage == null || configuration == null) {
            throw new SitemapException("Failed to build product sitemap at: " + sitemapRoot.getPath());
        }

        // parameter map to be reused while iterating
        UrlProvider.ParamsBuilder paramsBuilder = new UrlProvider.ParamsBuilder().page(externalizer.externalize(sitemapRoot));

        Deque<String> categoryIds = new LinkedList<>();
        boolean enableUidSupport = configuration.get(PN_ENABLE_UID_SUPPORT, Boolean.FALSE);
        String rootCategoryIdentifier = configuration.get(PN_MAGENTO_ROOT_CATEGORY_ID, String.class);
        String[] pendingCategories = context.getProperty(PN_PENDING_CATEGORIES, String[].class);
        if (pendingCategories != null) {
            categoryIds.addAll(Arrays.asList(pendingCategories));
        } else {
            // to start with
            categoryIds.add(rootCategoryIdentifier);
        }

        while (!categoryIds.isEmpty()) {
            String categoryId = categoryIds.poll();
            String query = Operations.query(categoryQueryFor(categoryId, enableUidSupport)).toString();
            GraphqlResponse<Query, Error> resp = graphql.execute(query);

            if (resp.getErrors() != null && resp.getErrors().size() > 0) {
                SitemapException ex = new SitemapException("Failed to execute graphql query.");
                resp.getErrors().forEach(error -> ex.addSuppressed(new Exception(error.getMessage())));
                throw ex;
            }

            CategoryResult categories = resp.getData().getCategories();
            // expected to be exactly one
            Iterator<CategoryTree> it = categories.getItems().iterator();

            if (it.hasNext()) {
                CategoryTree category = it.next();
                Stream<CategoryTree> children = category.getChildren().stream();
                List<String> childIds = (enableUidSupport
                    ? children.map(CategoryTree::getUid).map(ID::toString)
                    : children.map(CategoryTree::getId).map(Object::toString)).collect(Collectors.toList());

                for (int i = childIds.size() - 1; i >= 0; i--) {
                    // adding the children in reverse order to the front of the dequeue wil implement a depth first traversal keeping
                    // memory consumption of the queue under control
                    categoryIds.addFirst(childIds.get(i));
                }

                boolean ignoredByFilter = categoryFilter != null && !categoryFilter.shouldInclude(categoryPage, category);

                if (!categoryId.equals(rootCategoryIdentifier) && !ignoredByFilter) {
                    // skip root category, and ignored categories
                    Map<String, String> params = paramsBuilder
                        .id(category.getId().toString())
                        .uid(category.getUid().toString())
                        .urlKey(category.getUrlKey())
                        .urlPath(category.getUrlPath())
                        .map();
                    sitemap.addUrl(urlProvider.toCategoryUrl(null, null, params));
                } else if (ignoredByFilter && LOG.isDebugEnabled()) {
                    LOG.debug("Ignore category {}, not allowed by filter: {}", category.getUid(),
                        categoryFilter.getClass().getSimpleName());
                }

                context.setProperty(PN_PENDING_CATEGORIES, categoryIds.toArray(new String[0]));
            }

            if (it.hasNext()) {
                LOG.warn("More the one category returned for '{}': {}", categoryId, it.next().getUrlPath());
            }
        }
    }

    private static QueryQueryDefinition categoryQueryFor(String categoryId, boolean uidSupport) {
        FilterEqualTypeInput equalToCategoryId = new FilterEqualTypeInput().setEq(categoryId);
        CategoryFilterInput categoryFilter = uidSupport
            ? new CategoryFilterInput().setCategoryUid(equalToCategoryId)
            : new CategoryFilterInput().setIds(equalToCategoryId);
        return q -> q.categories(
            arguments -> arguments
                .filters(categoryFilter),
            resultSet -> resultSet
                .items(category -> category
                    .urlKey()
                    .urlPath()
                    .uid()
                    .id()
                    .children(child -> child
                        .uid()
                        .id())));
    }
}
