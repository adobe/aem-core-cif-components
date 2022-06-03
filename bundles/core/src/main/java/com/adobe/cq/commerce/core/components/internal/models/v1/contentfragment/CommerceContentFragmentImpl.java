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
package com.adobe.cq.commerce.core.components.internal.models.v1.contentfragment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.models.contentfragment.CommerceContentFragment;
import com.adobe.cq.commerce.core.components.services.SiteNavigation;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.dam.cfm.content.FragmentRenderService;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.contentfragment.ContentFragment;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;
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
    adapters = { ContentFragment.class, CommerceContentFragment.class },
    resourceType = CommerceContentFragmentImpl.RESOURCE_TYPE)
public class CommerceContentFragmentImpl implements CommerceContentFragment {
    static final String RESOURCE_TYPE = "core/cif/components/commerce/contentfragment/v1/contentfragment";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceContentFragmentImpl.class);
    private static final String CORE_WCM_CONTENTFRAGMENT_RT = "core/wcm/components/contentfragment/v1/contentfragment";
    private static final ContentFragment EMPTY_CONTENT_FRAGMENT = new EmptyContentFragment();

    @ValueMapValue(name = CommerceContentFragment.PN_MODEL_PATH, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String modelPath;

    @ValueMapValue(name = CommerceContentFragment.PN_PARENT_PATH, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String parentPath = DamConstants.MOUNTPOINT_ASSETS;

    @OSGiService
    private ModelFactory modelFactory;

    @SlingObject
    private ResourceResolver resourceResolver;

    @Self
    private SlingHttpServletRequest request;

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;

    @ScriptVariable
    private Page currentPage;

    @OSGiService
    private UrlProvider urlProvider;

    // needed for rawcontent rendering
    @ValueMapValue(name = ContentFragment.PN_DISPLAY_MODE, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String displayMode;

    @OSGiService
    private FragmentRenderService renderService;

    @SlingObject
    private Resource resource;

    @ValueMapValue(name = CommerceContentFragment.PN_LINK_ELEMENT, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String linkElement;

    @OSGiService
    private SiteNavigation siteNavigation;

    private ContentFragment contentFragment = EMPTY_CONTENT_FRAGMENT;
    private String modelTitle = "";

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

        Resource modelResource = resourceResolver.getResource(modelPath);
        if (modelResource != null) {
            modelTitle = modelResource.getValueMap().get("jcr:content/jcr:title", "");
        }

        Resource resource = findContentFragment();
        if (resource != null) {
            ValueMapResourceWrapper resourceWrapper = new ValueMapResourceWrapper(request.getResource(), CORE_WCM_CONTENTFRAGMENT_RT);
            resourceWrapper.getValueMap().putAll(request.getResource().getValueMap());
            resourceWrapper.getValueMap().put("fragmentPath", resource.getPath());
            ContentFragment contentFragment = modelFactory.getModelFromWrappedRequest(request, resourceWrapper, ContentFragment.class);
            if (contentFragment != null) {
                this.contentFragment = contentFragment;
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

        String linkValue = null;
        if (siteNavigation.isProductPage(currentPage)) {
            linkValue = findProductSku();
        } else if (siteNavigation.isCategoryPage(currentPage)) {
            linkValue = findCategoryIdentifier();
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

    private String findCategoryIdentifier() {
        String categoryUid = urlProvider.getCategoryIdentifier(request);
        if (StringUtils.isBlank(categoryUid)) {
            LOGGER.warn("Cannot find category identifier for current request");
        }
        return categoryUid;
    }

    private String findProductSku() {
        String sku = urlProvider.getProductIdentifier(request);
        if (StringUtils.isBlank(sku)) {
            LOGGER.warn("Cannot find sku or product for current request");
        }
        return sku;
    }

    /*
     * Adapted from Core WCM components to support paragraph rendering of single text field content fragments with custom URL format.
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

        String value = urlProvider.getProductIdentifier(request);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        // we pass the identifier as the FragmentRenderService uses an internal request
        // that not necessarily supports the format the UrlProvider is configured for
        // see: com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl#getIdentifierFromFragmentRenderRequest(request)
        ValueMap config = new ValueMapDecorator(new HashMap<>());
        config.put(UrlProviderImpl.CIF_IDENTIFIER_ATTR, value);

        // render the fragment
        String content = renderService.render(resource, config);
        if (content == null) {
            return null;
        }

        // split into paragraphs
        return content.split("(?=(<p>|<h1>|<h2>|<h3>|<h4>|<h5>|<h6>))");
    }

    @Override
    public String getModelTitle() {
        return modelTitle;
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

    @Override
    public String getId() {
        return contentFragment.getId();
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
