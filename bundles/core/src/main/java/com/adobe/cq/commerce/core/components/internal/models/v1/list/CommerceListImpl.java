/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.list;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.apache.sling.models.factory.ModelFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.PageParams;
import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.wcm.core.components.models.List;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = List.class,
    resourceType = CommerceListImpl.RESOURCE_TYPE)
public class CommerceListImpl extends DataLayerComponent implements List {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceListImpl.class);
    static final String CORE_WCM_LIST_RESOURCE_TYPE = "core/wcm/components/list/v3/list";
    static final String RESOURCE_TYPE = "core/cif/components/commerce/list/v1/list";
    static final String LIST_SOURCE_PRODUCT_ASSOCIATION = "productAssociation";
    static final String LIST_SOURCE_CATEGORY_ASSOCIATION = "categoryAssociation";
    static final String LIST_SOURCE_STATIC = "static";
    static final String PN_MAX_ITEMS = "maxItems";
    static final String PN_PRODUCT = "product";
    static final String PN_CATEGORY = "category";

    @OSGiService
    private ModelFactory modelFactory;

    @Self
    @Via(type = ForcedResourceType.class, value = CORE_WCM_LIST_RESOURCE_TYPE)
    private List wcmList;

    @ValueMapValue(name = List.PN_SOURCE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String listFrom;

    @ValueMapValue(name = PN_MAX_ITEMS, injectionStrategy = InjectionStrategy.OPTIONAL)
    private int maxItems;

    @ValueMapValue(name = PN_PRODUCT, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String product;

    @ValueMapValue(name = PN_CATEGORY, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String category;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @Self
    private SiteStructure siteStructure;

    @OSGiService
    private AssociatedContentService associatedContentService;

    @OSGiService
    private UrlProvider urlProvider;

    @PostConstruct
    void initModel() {
        AssociatedContentQuery<Page> contentQuery = null;

        if (LIST_SOURCE_PRODUCT_ASSOCIATION.equals(listFrom)) {
            String sku = StringUtils.isNotBlank(product) ? product
                : siteStructure.isProductPage(currentPage) ? urlProvider.getProductIdentifier(request) : null;

            if (StringUtils.isNotBlank(sku)) {
                contentQuery = associatedContentService.listProductContentPages(resourceResolver,
                    PageParams.of(sku).path(siteStructure.getLandingPage().getPath()));
            } else {
                LOGGER.warn("Product SKU missing for Commerce List at " + resource.getPath());
            }
        } else if (LIST_SOURCE_CATEGORY_ASSOCIATION.equals(listFrom)) {
            String categoryUid = StringUtils.isNotBlank(category) ? category
                : siteStructure.isCategoryPage(currentPage) ? urlProvider.getCategoryIdentifier(request) : null;
            if (StringUtils.isNotBlank(categoryUid)) {
                contentQuery = associatedContentService.listCategoryContentPages(resourceResolver,
                    PageParams.of(categoryUid).path(siteStructure.getLandingPage().getPath()));
            } else {
                LOGGER.warn("Category identifier missing for Commerce List at " + resource.getPath());
            }
        }

        if (contentQuery == null) {
            return;
        }

        if (maxItems > 0) {
            contentQuery.withLimit(maxItems);
        }

        ArrayList<Page> pages = new ArrayList<>();
        contentQuery.execute().forEachRemaining(pages::add);
        String[] pagePaths = pages.stream().map(Page::getPath).toArray(String[]::new);

        ValueMapResourceWrapper resourceWrapper = new ValueMapResourceWrapper(request.getResource(), CORE_WCM_LIST_RESOURCE_TYPE);
        ValueMap valueMap = resourceWrapper.getValueMap();
        valueMap.putAll(request.getResource().getValueMap());
        valueMap.put(PN_PAGES, pagePaths);
        valueMap.put(PN_SOURCE, LIST_SOURCE_STATIC);

        CommerceListRequestWrapper requestWrapper = new CommerceListRequestWrapper(request, resourceWrapper);
        try {
            this.wcmList = modelFactory.createModel(requestWrapper, List.class);
        } catch (Exception x) {
            LOGGER.error("Cannot create Core WCM List model for Commerce List.", x);
        }
    }

    @Override
    public @NotNull Collection<ListItem> getListItems() {
        return wcmList.getListItems();
    }

    @Override
    public boolean displayItemAsTeaser() {
        return wcmList.displayItemAsTeaser();
    }

    @Override
    public boolean linkItems() {
        return wcmList.linkItems();
    }

    @Override
    public boolean showDescription() {
        return wcmList.showDescription();
    }

    @Override
    public boolean showModificationDate() {
        return wcmList.showModificationDate();
    }

    @Override
    public String getDateFormatString() {
        return wcmList.getDateFormatString();
    }

    @Override
    public @Nullable String getAppliedCssClasses() {
        return wcmList.getAppliedCssClasses();
    }

    @Override
    public @NotNull String getExportedType() {
        return RESOURCE_TYPE;
    }

    private static class CommerceListRequestWrapper extends SlingHttpServletRequestWrapper {
        private final Resource resource;
        private final SlingBindings slingBindings = new SlingBindings();

        public CommerceListRequestWrapper(SlingHttpServletRequest request, Resource resource) {
            super(request);
            this.resource = resource;

            SlingBindings existingBindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
            if (existingBindings != null) {
                slingBindings.putAll(existingBindings);
            }

            slingBindings.put(SlingBindings.REQUEST, this);
            slingBindings.put(SlingBindings.RESOURCE, resource);
            slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, resource.getValueMap());
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public Object getAttribute(String name) {
            if (SlingBindings.class.getName().equals(name)) {
                return slingBindings;
            } else {
                return super.getAttribute(name);
            }
        }
    }
}
