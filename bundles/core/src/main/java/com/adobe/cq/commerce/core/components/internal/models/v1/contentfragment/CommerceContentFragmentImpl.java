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
package com.adobe.cq.commerce.core.components.internal.models.v1.contentfragment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.*;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragment;
import com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragmentList;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;

import static com.day.cq.dam.api.DamConstants.NT_DAM_ASSET;

/**
 * <code>CommerceContentFragmentImpl</code> is the Sling Model for the Commerce Content Fragment component
 * providing a Core WCM ContentFragment implementation with commerce specific functionality.
 *
 * Principle of operation:
 * - looks up the content fragment resource based on a content fragment model and product sku or category identifier
 * - instantiates Core WCM ContentFragment for the content fragment resource or {@link EmptyContentFragment} if no content fragment is found
 * - delegates calls to the WCM content fragment except {@link EmptyContentFragment#getName()} and {@link #getParagraphs()} which are
 * customized
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ContentFragment.class,
    resourceType = CommerceContentFragmentImpl.RESOURCE_TYPE)
public class CommerceContentFragmentImpl implements ContentFragment {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceContentFragmentImpl.class);
    private static final List<UrlProvider.CategoryIdentifierType> VALID_CATEGORY_IDENTIFIERS = Collections.unmodifiableList(Arrays.asList(
        UrlProvider.CategoryIdentifierType.ID, UrlProvider.CategoryIdentifierType.UID));
    /**
     * The resource type of the component associated with this Sling model.
     */
    public static final String RESOURCE_TYPE = "core/cif/components/commerce/contentfragment/v1/contentfragment";
    public static final String CORE_WCM_CONTENTFRAGMENT_RT = "core/wcm/components/contentfragment/v1/contentfragment";
    public static final String DEFAULT_DAM_PARENT_PATH = "/content/dam";
    public static final ContentFragment EMPTY_CONTENT_FRAGMENT = new EmptyContentFragment();
    @ValueMapValue(name = ContentFragmentList.PN_MODEL_PATH, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String modelPath;
    @ValueMapValue(name = ContentFragmentList.PN_PARENT_PATH, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String parentPath = DEFAULT_DAM_PARENT_PATH;

    @Inject
    private ModelFactory modelFactory;
    @SlingObject
    private ResourceResolver resourceResolver;
    @Self
    private SlingHttpServletRequest request;
    @Inject
    private Page currentPage;
    @Inject
    private UrlProvider urlProvider;
    private ContentFragment contentFragment = EMPTY_CONTENT_FRAGMENT;

    // needed for rawcontent rendering
    @ValueMapValue(name = ContentFragment.PN_DISPLAY_MODE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String displayMode;
    @Inject
    private FragmentRenderService renderService;
    @ScriptVariable
    private Resource resource;
    @ValueMapValue(name = "linkElement", injectionStrategy = InjectionStrategy.OPTIONAL)
    private String linkElement;
    private String linkValue;

    @PostConstruct
    void initModel() {
        if (StringUtils.isBlank(modelPath)) {
            LOGGER.warn("Please provide a model path");
            return;
        }

        if (StringUtils.isBlank(linkElement)) {
            LOGGER.warn("Please provide a link element");
            return;
        }

        Resource result = findContentFragment();
        if (result != null) {
            ValueMapResourceWrapper resourceWrapper = new ValueMapResourceWrapper(request.getResource(), CORE_WCM_CONTENTFRAGMENT_RT);
            resourceWrapper.getValueMap().putAll(request.getResource().getValueMap());
            resourceWrapper.getValueMap().put("fragmentPath", result.getPath());
            contentFragment = modelFactory.getModelFromWrappedRequest(request, resourceWrapper, ContentFragment.class);
            if (contentFragment == null) {
                contentFragment = EMPTY_CONTENT_FRAGMENT;
            }
        }
    }

    private Resource findContentFragment() {
        Session session = resourceResolver.adaptTo(Session.class);
        if (session == null) {
            LOGGER.warn("Session was null therefore no query was executed");
            return null;
        }

        QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
        if (queryBuilder == null) {
            LOGGER.warn("Query builder was null therefore no query was executed");
            return null;
        }

        if (SiteNavigation.isProductPage(currentPage)) {
            String sku = null;
            Pair<UrlProvider.ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);
            if (UrlProvider.ProductIdentifierType.SKU.equals(identifier.getLeft())) {
                sku = identifier.getRight();
            } else {
                Product product = request.adaptTo(Product.class);
                if (product != null && product.getFound()) {
                    sku = product.getSku();
                }
            }
            if (StringUtils.isBlank(sku)) {
                LOGGER.warn("Cannot find sku or product for current request");
            } else {
                linkValue = sku;
            }
        } else if (SiteNavigation.isCategoryPage(currentPage)) {
            String categoryIdentifier = null;
            Pair<UrlProvider.CategoryIdentifierType, String> identifier = urlProvider.getCategoryIdentifier(request);
            if (VALID_CATEGORY_IDENTIFIERS.contains(identifier.getLeft())) {
                categoryIdentifier = identifier.getRight();
            }
            if (StringUtils.isBlank(categoryIdentifier)) {
                LOGGER.warn("Cannot find category identifier for current request");
            } else {
                linkValue = categoryIdentifier;
            }
        }

        if (StringUtils.isBlank(linkValue)) {
            return null;
        }

        Map<String, String> queryParameterMap = new HashMap<>();
        queryParameterMap.put("path", parentPath);
        queryParameterMap.put("type", NT_DAM_ASSET);
        queryParameterMap.put("p.limit", "1");
        queryParameterMap.put("1_property", JcrConstants.JCR_CONTENT + "/data/cq:model");
        queryParameterMap.put("1_property.value", modelPath);
        queryParameterMap.put("2_property", JcrConstants.JCR_CONTENT + "/data/master/" + linkElement);
        queryParameterMap.put("2_property.value", linkValue);

        PredicateGroup predicateGroup = PredicateGroup.create(queryParameterMap);
        Query query = queryBuilder.createQuery(predicateGroup, session);

        SearchResult searchResult = query.getResult();
        return searchResult.getTotalMatches() > 0 ? searchResult.getResources().next() : null;
    }

    /*
     * Adapted from Core WCM components to support paragraph rendering of single text field content fragments with customer URL format.
     */
    @Override
    public String[] getParagraphs() {
        if (!"singleText".equals(displayMode)) {
            return null;
        }

        if (contentFragment.getElements() == null || contentFragment.getElements().isEmpty()) {
            return null;
        }

        DAMContentElement damContentElement = contentFragment.getElements().get(0);

        // restrict this method to text elements
        if (!damContentElement.isMultiLine()) {
            return null;
        }

        if (StringUtils.isBlank(linkValue)) {
            return null;
        }

        String selector;
        if (SiteNavigation.isProductPage(currentPage)) {
            Pair<UrlProvider.ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);
            if (UrlProvider.ProductIdentifierType.URL_KEY.equals(identifier.getLeft())) {
                selector = identifier.getRight();
            } else {
                selector = linkValue;
            }
        } else {
            selector = linkValue;
        }

        if (StringUtils.isBlank(selector)) {
            return null;
        }

        // rawcontent selector fist for raw content rendering
        // CIF product SKU or category identifier selector last as mandated by the CIF URL Provider
        ValueMap config = new ValueMapDecorator(new HashMap<>());
        config.put("dam.cfm.useSelector", "rawcontent." + selector);

        // render the fragment
        String content = renderService.render(resource, config);
        if (content == null) {
            return null;
        }

        // split into paragraphs
        return content.split("(?=(<p>|<h1>|<h2>|<h3>|<h4>|<h5>|<h6>))");
    }

    @Override
    public String getGridResourceType() {
        return contentFragment.getGridResourceType();
    }

    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        return contentFragment.getExportedItems();
    }

    @Override
    public String[] getExportedItemsOrder() {
        return contentFragment.getExportedItemsOrder();
    }

    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }

    @Override
    public String getTitle() {
        return contentFragment.getTitle();
    }

    @Override
    public String getName() {
        return contentFragment.getName();
    }

    @Override
    public String getDescription() {
        return contentFragment.getDescription();
    }

    @Override
    public String getType() {
        return contentFragment.getType();
    }

    @Override
    public List<DAMContentElement> getElements() {
        return contentFragment.getElements();
    }

    @Override
    public Map<String, DAMContentElement> getExportedElements() {
        return contentFragment.getExportedElements();
    }

    @Override
    public String[] getExportedElementsOrder() {
        return contentFragment.getExportedElementsOrder();
    }

    @Override
    public List<Resource> getAssociatedContent() {
        return contentFragment.getAssociatedContent();
    }

    @Override
    public String getEditorJSON() {
        return contentFragment.getEditorJSON();
    }

    static class EmptyContentFragment implements ContentFragment {
        /**
         * The empty return value means that the model does not contain a valid content fragment.
         */
        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getGridResourceType() {
            return "";
        }

        @Override
        public Map<String, ? extends ComponentExporter> getExportedItems() {
            return Collections.emptyMap();
        }

        @Override
        public String[] getExportedItemsOrder() {
            return new String[0];
        }

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public List<DAMContentElement> getElements() {
            return null;
        }

        @Override
        public Map<String, DAMContentElement> getExportedElements() {
            return Collections.emptyMap();
        }

        @Override
        public String[] getExportedElementsOrder() {
            return new String[0];
        }

        @Override
        public List<Resource> getAssociatedContent() {
            return null;
        }

        @Override
        public String getEditorJSON() {
            return "{}";
        }
    }
}
