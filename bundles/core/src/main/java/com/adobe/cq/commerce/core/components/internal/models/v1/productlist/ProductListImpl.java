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


import com.adobe.cq.commerce.core.components.internal.models.v1.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl implements ProductList {

    protected static final String RESOURCE_TYPE = "venia/components/commerce/productlist/v1/productlist";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListImpl.class);

    private static final boolean SHOW_TITLE_DEFAULT = true;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private ValueMap properties;

    @ScriptVariable
    private Style currentStyle;

    @Inject
    private Resource resource;

    @Inject
    private Page currentPage;

    private Page productPage;
    private CategoryInterface category;
    private boolean showTitle;
    private MagentoGraphqlClient magentoGraphqlClient;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));

        // get product template page
        productPage = Utils.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        // Parse category id from URL
        final Integer categoryId = parseCategoryId();

        // get GraphQL client and query data
        if (categoryId != null) {
            magentoGraphqlClient = MagentoGraphqlClient.create(resource);
            if (magentoGraphqlClient != null) {
                category = fetchCategory(categoryId);
            }
        }
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

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        Collection<ProductListItem> listItems = new ArrayList<>();

        if (category != null) {
            final CategoryProducts products = category.getProducts();
            if (products != null) {
                for (ProductInterface product : products.getItems()) {
                    listItems.add(new ProductListItemImpl(
                            product.getSku(),
                            product.getUrlKey(),
                            product.getName(),
                            product.getPrice().getRegularPrice().getAmount().getValue(),
                            product.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                            product.getSmallImage().getUrl(),
                            productPage));
                }
            }
        }
        return listItems;
    }

    /* --- GraphQL queries --- */
    private ProductPricesQueryDefinition generatePriceQuery() {
        return q -> q
                .regularPrice(rp -> rp
                        .amount(a -> a
                                .currency()
                                .value()));
    }

    private ProductInterfaceQueryDefinition generateProductQuery() {
        return q -> q
                .id()
                .name()
                .smallImage(i -> i.url())
                .urlKey()
                .price(generatePriceQuery());
    }

    private CategoryTreeQueryDefinition generateProductListQuery() {

        final Integer cPage = request.getCookie("currentPage") == null ? 1 : Integer.parseInt(request.getCookie("currentPage").getValue()); //cant trap NumberFormatException as only `final` var to be passed in Lambda

        final Integer pSize = currentPage.getProperties().get("pageSize") == null ? 6 : Integer.parseInt(currentPage.getProperties().get("pageSize").toString());

        CategoryTreeQuery.ProductsArgumentsDefinition pArgs = q -> q
                .currentPage(cPage)
                .pageSize(pSize);
        CategoryTreeQueryDefinition categoryTreeQueryDefinition = q -> q
                .id()
                .description()
                .name()
                .productCount()
                .products(pArgs, categoryProductsQuery -> categoryProductsQuery.items(generateProductQuery()).totalCount());
        return categoryTreeQueryDefinition;
    }

    /* --- Utility methods --- */

    /**
     * Retrieve and return the category data from backend.
     *
     * @param categoryId the category id of category we request
     * @return {@link CategoryInterface}
     */
    private CategoryInterface fetchCategory(int categoryId) {
        LOGGER.debug("Trying to load category data for {}", categoryId);

        // Construct GraphQL query
        QueryQuery.CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);

        CategoryTreeQueryDefinition queryArgs = generateProductListQuery();
        String queryString = Operations.query(query -> query.category(searchArgs, queryArgs)).toString();

        // Send GraphQL request
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(queryString);

        // Get category & product list from response
        Query rootQuery = response.getData();
        return rootQuery.getCategory();
    }


    /**
     * Returns the selector of the current request which is expected to be the category id.
     *
     * @return category id
     */
    private Integer parseCategoryId() {
        // TODO this should be change to slug/url_path if that is available to retrieve category data,
        //  currently we only can use the category id for that.
        Integer categoryId = null;

        try {
            categoryId = Integer.parseInt(this.request.getRequestPathInfo().getSelectorString());
        } catch (NumberFormatException nef) {
            LOGGER.warn("Could not parse category id from current page selectors.");
        }
        return categoryId;
    }
}

