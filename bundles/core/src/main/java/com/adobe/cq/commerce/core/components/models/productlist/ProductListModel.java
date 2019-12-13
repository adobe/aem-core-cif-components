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

package com.adobe.cq.commerce.core.components.models.productlist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.scripting.sightly.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.GraphqlModel;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;

import static com.adobe.cq.commerce.core.components.models.product.Product.PN_LOAD_CLIENT_PRICE;
import static com.adobe.cq.commerce.core.components.models.productlist.ProductList.PN_PAGE_SIZE;
import static com.adobe.cq.commerce.core.components.models.productlist.ProductList.PN_SHOW_IMAGE;
import static com.adobe.cq.commerce.core.components.models.productlist.ProductList.PN_SHOW_TITLE;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductListModel.class)
public class ProductListModel extends GraphqlModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductListModel.class);

    protected static final String PLACEHOLDER_DATA = "/productlist-component-placeholder-data.json";

    private static final boolean SHOW_TITLE_DEFAULT = true;
    private static final boolean SHOW_IMAGE_DEFAULT = true;
    private static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    private static final int PAGE_SIZE_DEFAULT = 6;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Style currentStyle;

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    private Page productPage;
    private boolean showTitle;
    private boolean showImage;
    private boolean loadClientPrice;

    private int navPageCursor = 1;
    private int navPageSize = PAGE_SIZE_DEFAULT;
    private Integer navPagePrev;
    private Integer navPageNext;
    private List<Integer> navPages;

    @PostConstruct
    private void initModel() {
        // read properties
        showTitle = properties.get(PN_SHOW_TITLE, currentStyle.get(PN_SHOW_TITLE, SHOW_TITLE_DEFAULT));
        showImage = properties.get(PN_SHOW_IMAGE, currentStyle.get(PN_SHOW_IMAGE, SHOW_IMAGE_DEFAULT));
        navPageSize = properties.get(PN_PAGE_SIZE, currentStyle.get(PN_PAGE_SIZE, PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, currentStyle.get(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));

        setNavPageCursor();

        // get product template page
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }

        // Parse category id from URL
        final String categoryId = parseCategoryId();

        if (categoryId != null) {
            // categoryRetriever = new CategoryRetriever(magentoGraphqlClient);
            Map<String, Object> vars = new HashMap<>();
            vars.put("categoryId", categoryId);
            vars.put("currentPage", navPageCursor);
            vars.put("pageSize", navPageSize);
            executeQuery(vars);
        } else if (!wcmMode.isDisabled()) {
            try {
                String json = IOUtils.toString(getClass().getResourceAsStream(PLACEHOLDER_DATA), StandardCharsets.UTF_8);
                Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
                data = graphqlRecordFactory.recordFrom(rootQuery);
            } catch (Exception e) {
                LOGGER.warn("Cannot use placeholder data", e);
            }
            loadClientPrice = false;
        }
    }

    @Override
    public String getMediaBaseUrl() {
        return (String) ((Record) ((Record) data).getProperty("storeConfig")).getProperty("secure_base_media_url");
    }

    public int getTotalCount() {
        return (Integer) ((Record) ((Record) ((Record) data).getProperty("category")).getProperty("products")).getProperty("total_count");
    }

    public boolean showTitle() {
        return showTitle;
    }

    public int getCurrentNavPage() {
        return navPageCursor;
    }

    public int getNextNavPage() {
        if (navPageNext == null) {
            this.setupPagination();
        }

        if ((getTotalCount() % navPageSize) == 0) {
            // if currentNavPage is already at last, set navPageNext to currentNavPage
            navPageNext = (navPageCursor < (getTotalCount() / navPageSize)) ? (navPageCursor + 1) : navPageCursor;
        } else {
            navPageNext = (navPageCursor < ((getTotalCount() / navPageSize) + 1)) ? (navPageCursor + 1) : navPageCursor;
        }
        return navPageNext;
    }

    public boolean showImage() {
        return showImage;
    }

    public boolean loadClientPrice() {
        return loadClientPrice;
    }

    public int getPreviousNavPage() {
        if (navPagePrev == null) {
            this.setupPagination();
        }
        return navPagePrev;
    }

    public List<Integer> getPageList() {
        if (navPages == null) {
            this.setupPagination();
        }
        return navPages;
    }

    /* --- Utility methods --- */

    /**
     * Returns the selector of the current request which is expected to be the category id.
     *
     * @return category id
     */
    private String parseCategoryId() {
        // TODO this should be change to slug/url_path if that is available to retrieve category data,
        // currently we only can use the category id for that.
        return request.getRequestPathInfo().getSelectorString();
    }

    /**
     * Obtains value from request for page, sets Pagination values for current, next and previous pages
     *
     * @return void
     */
    void setupPagination() {
        int navPagesSize = 0;

        navPagePrev = (navPageCursor <= 1) ? 1 : (navPageCursor - 1);
        if ((getTotalCount() % navPageSize) == 0) {
            navPagesSize = getTotalCount() / navPageSize;
            navPageNext = (navPageCursor < (getTotalCount() / navPageSize)) ? (navPageCursor + 1) : navPageCursor;
        } else {
            navPagesSize = (getTotalCount() / navPageSize) + 1;
            navPageNext = (navPageCursor < ((getTotalCount() / navPageSize) + 1)) ? (navPageCursor + 1) : navPageCursor;
        }
        navPages = new ArrayList<>();

        for (int i = 0; i < navPagesSize; i++) {
            navPages.add(i + 1);
        }
    }

    /**
     * Sets value of navPageCursor from URL param if provided, else keeps it to default 1
     *
     * @return void
     */
    void setNavPageCursor() {
        // check if pageCursor available in queryString, already set to 1 if not.
        if (request.getParameter("page") != null) {
            try {
                navPageCursor = Integer.parseInt(request.getParameter("page"));
                if (navPageCursor <= 0) {
                    LOGGER.warn("invalid value of CGI variable page encountered, using default instead");
                    navPageCursor = 1;
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("non-parseable value for CGI variable page encountered, keeping navPageCursor value to default ");
            }
        }
    }

    @Override
    public String getVariantSku() {
        return null;
    }
}
