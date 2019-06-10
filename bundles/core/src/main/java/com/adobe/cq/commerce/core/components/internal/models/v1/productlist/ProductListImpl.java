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
    private static final int PAGE_SIZE_DEFAULT = 6;

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

    private int navPageCursor = 1;
    private int navPagePrev;
    private int navPageNext;
    private int navPageSize = PAGE_SIZE_DEFAULT;
    private int[] navPages;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, PAGE_SIZE_DEFAULT));

        setNavPageCursor();

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
        setupPagination();
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

    @Override
    public int getTotalCount() {
        return category.getProducts().getTotalCount();
    }

    @Override
    public int getCurrentNavPage() {
        return this.navPageCursor;
    }

    @Override
    public int getNextNavPage() {
        if ((this.getTotalCount() % this.navPageSize) == 0) {

            //if currentNavPage is already at last, set navPageNext to currentNavPage
            this.navPageNext = (this.navPageCursor < (this.getTotalCount() / this.navPageSize)) ? (this.navPageCursor + 1) : this.navPageCursor;

        } else {
            this.navPageNext = (this.navPageCursor < ((this.getTotalCount() / this.navPageSize) + 1)) ? (this.navPageCursor + 1) : this.navPageCursor;

        }
        return this.navPageNext;
    }

    @Override
    public int getPreviousNavPage() {
        return this.navPagePrev;
    }

    @Override
    public int[] getPageList() {
        return this.navPages;
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

        CategoryTreeQuery.ProductsArgumentsDefinition pArgs = q -> q
                .currentPage(this.navPageCursor)
                .pageSize(this.navPageSize);
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
        } catch (NullPointerException | NumberFormatException nef) {
            LOGGER.warn("Could not parse category id from current page selectors.");
        }
        return categoryId;
    }

    /**
     * Obtains value from request for page, sets Pagination values for current, next and previous pages
     *
     * @return void
     */
    void setupPagination() {
        this.navPagePrev = (this.navPageCursor <= 1) ? 1 : (this.navPageCursor - 1);
        if ((this.getTotalCount() % this.navPageSize) == 0) {
            this.navPages = new int[(this.getTotalCount() / this.navPageSize)];
            this.navPageNext = (this.navPageCursor < (this.getTotalCount() / this.navPageSize)) ? (this.navPageCursor + 1) : this.navPageCursor;
        } else {
            this.navPages = new int[(this.getTotalCount() / this.navPageSize) + 1];
            this.navPageNext = (this.navPageCursor < ((this.getTotalCount() / this.navPageSize) + 1)) ? (this.navPageCursor + 1) : this.navPageCursor;
        }
        for (int i = 0; i < this.navPages.length; i++) {
            this.navPages[i] = (i + 1);
        }
    }

    /**
     * Sets value of navPageCursor from URL param if provided, else keeps it to default 1
     *
     * @return void
     */
    void setNavPageCursor() {
        //check if pageCursor available in queryString, already set to 1 if not.
        if (request.getParameter("page") != null) {
            try {
                this.navPageCursor = Integer.parseInt(request.getParameter("page"));
                if (this.navPageCursor <= 0) {
                    LOGGER.warn("invalid value of CGI variable page encountered, using default instead");
                    this.navPageCursor = 1;
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("non-parseable value for CGI variable page encountered, keeping navPageCursor value to default ");
            }
        }
    }
}