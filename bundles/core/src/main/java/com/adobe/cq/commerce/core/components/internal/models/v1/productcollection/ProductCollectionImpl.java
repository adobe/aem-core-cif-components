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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcollection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl;
import com.adobe.cq.commerce.core.search.internal.models.SearchResultsSetImpl;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.services.SearchResultsService;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.Externalizer;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.designer.Style;

import static com.adobe.cq.commerce.core.search.internal.models.SearchOptionsImpl.PAGE_SIZE_DEFAULT;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ProductCollection.class,
    resourceType = ProductCollectionImpl.RESOURCE_TYPE)
public class ProductCollectionImpl extends DataLayerComponent implements ProductCollection {

    public static final String RESOURCE_TYPE = "core/cif/components/commerce/productcollection/v1/productcollection";

    protected static final boolean LOAD_CLIENT_PRICE_DEFAULT = true;
    protected static final String PAGINATION_TYPE_DEFAULT = "paginationbar";

    protected Page productPage;
    protected boolean loadClientPrice;
    protected int navPageSize;
    protected String paginationType;

    @Self
    protected SlingHttpServletRequest request;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected ValueMap properties;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected Style currentStyle;
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected Page currentPage;
    @OSGiService
    protected SearchResultsService searchResultsService;
    @OSGiService
    protected UrlProvider urlProvider;
    @OSGiService
    protected Externalizer externalizer;
    @OSGiService
    protected PageManagerFactory pageManagerFactory;

    protected SearchOptionsImpl searchOptions;
    protected SearchResultsSet searchResultsSet;

    @PostConstruct
    private void baseInitModel() {
        if (properties == null) {
            properties = request.getResource().getValueMap();
        }
        if (currentPage == null) {
            currentPage = pageManagerFactory.getPageManager(request.getResourceResolver())
                .getContainingPage(request.getResource());
        }

        navPageSize = properties.get(PN_PAGE_SIZE, getOptionalStyle(PN_PAGE_SIZE, PAGE_SIZE_DEFAULT));
        loadClientPrice = properties.get(PN_LOAD_CLIENT_PRICE, getOptionalStyle(PN_LOAD_CLIENT_PRICE, LOAD_CLIENT_PRICE_DEFAULT));
        paginationType = properties.get(PN_PAGINATION_TYPE, getOptionalStyle(PN_PAGINATION_TYPE, PAGINATION_TYPE_DEFAULT));

        // get product template page
        productPage = SiteNavigation.getProductPage(currentPage);
        if (productPage == null) {
            productPage = currentPage;
        }
    }

    protected final <T> T getOptionalStyle(String pn, T defaultValue) {
        return currentStyle != null ? currentStyle.get(pn, defaultValue) : defaultValue;
    }

    public Integer calculateCurrentPageCursor(final String currentPageIndexCandidate) {
        // make sure the current page from the query string is reasonable i.e. numeric and over 0
        try {
            int i = Integer.parseInt(currentPageIndexCandidate);
            if (i < 1) {
                i = 1;
            }
            return i;
        } catch (NumberFormatException x) {
            return 1;
        }
    }

    @Override
    public boolean loadClientPrice() {
        return loadClientPrice && !LaunchUtils.isLaunchBasedPath(currentPage.getPath());
    }

    @Override
    public String getPaginationType() {
        return paginationType;
    }

    protected Map<String, String> createFilterMap(final Map<String, String[]> parameterMap) {
        Map<String, String> filters = new HashMap<>();
        parameterMap.forEach((code, value) -> {
            // we'll make sure there is a value defined for the key
            if (value.length != 1) {
                return;
            }

            filters.put(code, value[0]);
        });

        return filters;
    }

    @Nonnull
    @Override
    public Collection<ProductListItem> getProducts() {
        return getSearchResultsSet().getProductListItems();
    }

    @Nonnull
    @Override
    public SearchResultsSet getSearchResultsSet() {
        if (searchResultsSet == null) {
            searchResultsSet = new SearchResultsSetImpl();
        }
        return searchResultsSet;
    }
}
