/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.spi.generator.SitemapGenerator;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.sitemap.SitemapCategoryFilter;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryResult;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.shopify.graphql.support.ID;

@Component(
    service = SitemapGenerator.class,
    property = {
        Constants.SERVICE_RANKING + ":Integer=100"
    })
@Designate(ocd = CategoriesSitemapGenerator.Configuration.class)
public class CategoriesSitemapGenerator extends SitemapGeneratorBase implements SitemapGenerator {

    @ObjectClassDefinition(name = "CIF Category Sitemap Generator")
    @interface Configuration {

        @AttributeDefinition(
            name = "Add Last Modified",
            description = "If enabled, a Category's last update date will be set as last "
                + "modified date to an url entry. This does not take into account any associated/referenced content on the category page nor "
                + "the last modified date know to AEM.")
        boolean enableLastModified() default true;
    }

    static final String PN_PENDING_CATEGORIES = "pendingCategories";
    static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";

    @Reference
    private UrlProvider urlProvider;
    @Reference
    private SitemapLinkExternalizerProvider externalizerProvider;
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapCategoryFilter categoryFilter;

    private boolean addLastModified;

    @Activate
    protected void activate(Configuration configuration) {
        this.addLastModified = configuration.enableLastModified();
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
    public void generate(Resource sitemapRoot, String name, Sitemap sitemap, Context context) throws SitemapException {
        MagentoGraphqlClient graphql = sitemapRoot.adaptTo(MagentoGraphqlClient.class);
        Page categoryPage = sitemapRoot.adaptTo(Page.class);
        ComponentsConfiguration configuration = sitemapRoot.adaptTo(ComponentsConfiguration.class);

        if (graphql == null || categoryPage == null || configuration == null) {
            throw new SitemapException("Failed to build category sitemap at: " + sitemapRoot.getPath());
        }

        ResourceResolver resourceResolver = sitemapRoot.getResourceResolver();
        SitemapLinkExternalizer externalizer = externalizerProvider.getExternalizer(resourceResolver);

        String rootCategoryIdentifier = configuration.get(PN_MAGENTO_ROOT_CATEGORY_ID, String.class);
        Deque<String> categoryUids = new LinkedList<>(Arrays.asList(
            context.getProperty(PN_PENDING_CATEGORIES, new String[] { rootCategoryIdentifier })));

        while (!categoryUids.isEmpty()) {
            String categoryId = categoryUids.poll();
            String query = Operations.query(categoryQueryFor(categoryId)).toString();
            GraphqlResponse<Query, Error> resp = graphql.execute(query);

            if (CollectionUtils.isNotEmpty(resp.getErrors())) {
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
                List<String> childUids = children.map(CategoryTree::getUid).map(ID::toString).collect(Collectors.toList());

                for (int i = childUids.size() - 1; i >= 0; i--) {
                    // adding the children in reverse order to the front of the dequeue wil implement a depth first traversal keeping
                    // memory consumption of the queue under control
                    categoryUids.addFirst(childUids.get(i));
                }

                boolean ignoredByFilter = categoryFilter != null && !categoryFilter.shouldInclude(categoryPage, category);

                if (!categoryId.equals(rootCategoryIdentifier) && !ignoredByFilter) {
                    // skip root category, and ignored categories
                    CategoryUrlFormat.Params params = new CategoryUrlFormat.Params(category);
                    params.setPage(categoryPage.getPath());
                    String urlStr = externalizer.toExternalCategoryUrl(null, categoryPage, params);
                    Url url = sitemap.addUrl(urlStr);
                    if (addLastModified) {
                        addLastModified(url, category);
                    }
                } else if (ignoredByFilter && logger.isDebugEnabled()) {
                    logger.debug("Ignore category {}, not allowed by filter: {}", category.getUid(),
                        categoryFilter.getClass().getSimpleName());
                }

                context.setProperty(PN_PENDING_CATEGORIES, categoryUids.toArray(new String[0]));
            }

            if (it.hasNext()) {
                logger.warn("More the one category returned for '{}': {}", categoryId, it.next().getUrlPath());
            }
        }
    }

    private QueryQueryDefinition categoryQueryFor(String categoryUid) {
        return q -> q.categories(
            arguments -> arguments
                .filters(new CategoryFilterInput()
                    .setCategoryUid(new FilterEqualTypeInput()
                        .setEq(categoryUid))),
            resultSet -> resultSet
                .items(category -> {
                    category
                        .urlKey()
                        .urlPath()
                        .uid()
                        .children(child -> child.uid());

                    if (addLastModified) {
                        category
                            .createdAt()
                            .updatedAt();
                    }
                }));
    }
}
