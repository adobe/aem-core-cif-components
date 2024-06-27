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
package com.adobe.cq.commerce.core.components.internal.models.v2.button;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.models.Button;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.day.cq.wcm.api.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Button.class,
    resourceType = ButtonImpl.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ButtonImpl implements Button {

    protected static final String RESOURCE_TYPE = "core/cif/components/content/button/v2/button";
    private static final String SUPER_RESOURCE_TYPE = "core/wcm/components/button/v2/button";

    private static final String DEFAULT_LABEL = "Label";
    private static final String PRODUCT = "product";
    private static final String CATEGORY = "category";
    private static final String EXTERNAL_LINK = "externalLink";
    private static final String LINK_TO = "linkTo";

    // the link property used by the WCM Core Component Button
    private static final String PN_BUTTON_LINK_URL = "linkURL";

    @Self(injectionStrategy = InjectionStrategy.OPTIONAL)
    private MagentoGraphqlClient magentoGraphqlClient;
    @Self
    private SlingHttpServletRequest request;
    @ValueMapValue(name = "productSku", injectionStrategy = InjectionStrategy.OPTIONAL)
    private String productIdentifier;
    @ValueMapValue(name = "categoryId", injectionStrategy = InjectionStrategy.OPTIONAL)
    private String categoryIdentifier;
    @ValueMapValue(name = "categoryIdType", injectionStrategy = InjectionStrategy.OPTIONAL)
    private String categoryIdType;
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String externalLink;
    @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
    private String linkTo;
    @ValueMapValue
    @Default(values = LINK_TO)
    private String linkType;
    @ScriptVariable
    private Page currentPage;
    @ScriptVariable
    protected Resource resource;
    @OSGiService
    private UrlProvider urlProvider;
    @OSGiService
    protected ModelFactory modelFactory;

    private Button delegate;
    private ComponentData componentData;

    @PostConstruct
    private void initModel() {
        Map<String, Object> allProperties = new HashMap<>(resource.getValueMap());
        String link = null;

        if (EXTERNAL_LINK.equals(linkType)) {
            link = externalLink;
        } else if (PRODUCT.equals(linkType) && productIdentifier != null) {
            link = urlProvider.toProductUrl(request, currentPage, productIdentifier);
        } else if (CATEGORY.equals(linkType) && categoryIdentifier != null) {
            urlProvider.setCategoryIdType(categoryIdType);
            link = urlProvider.toCategoryUrl(request, currentPage, categoryIdentifier);
        } else if (StringUtils.isNotEmpty(linkTo)) {
            link = linkTo + ".html";
        }

        allProperties.put(PN_BUTTON_LINK_URL, link);
        Resource delegateResource = new ValueMapResource(resource, SUPER_RESOURCE_TYPE, new ValueMapDecorator(allProperties));
        delegate = modelFactory.getModelFromWrappedRequest(request, delegateResource, Button.class);
    }

    /**
     * Returns the product's SKU the link type is product and the sku is set.
     *
     * @return the product identifier
     */
    public String getSku() {
        return PRODUCT.equals(linkType) ? productIdentifier : null;
    }

    @Override
    public String getText() {
        return StringUtils.defaultIfEmpty(delegate != null ? delegate.getText() : null, DEFAULT_LABEL);
    }

    @Override
    public Link getButtonLink() {
        return delegate != null ? delegate.getButtonLink() : null;
    }

    @Override
    @Deprecated
    public String getLink() {
        return StringUtils.defaultIfEmpty(delegate != null ? delegate.getLink() : null, "#");
    }

    @Override
    public String getIcon() {
        return delegate != null ? delegate.getIcon() : null;
    }

    @Override
    public String getAccessibilityLabel() {
        return delegate != null ? delegate.getAccessibilityLabel() : null;
    }

    @Override
    public String getId() {
        return delegate != null ? delegate.getId() : null;
    }

    @Override
    public ComponentData getData() {
        if (componentData == null && delegate != null) {
            ComponentData parentData = delegate.getData();
            if (parentData != null) {
                componentData = DataLayerBuilder.extending(parentData).asComponent().withType(() -> RESOURCE_TYPE).build();
            }
        }
        return componentData;
    }

    @Override
    public String getAppliedCssClasses() {
        return delegate != null ? delegate.getAppliedCssClasses() : null;
    }

    @Override
    public String getExportedType() {
        return delegate != null ? delegate.getExportedType() : StringUtils.EMPTY;
    }

    private static class ValueMapResource extends ResourceWrapper {

        private final ValueMap valueMap;
        private final String resourceType;

        public ValueMapResource(Resource resource, String resourceType, ValueMap valueMap) {
            super(resource);
            this.valueMap = valueMap;
            this.resourceType = resourceType;
            this.valueMap.put("sling:resourceType", resourceType);
        }

        @Override
        public String getResourceType() {
            return resourceType;
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            return type.equals(ValueMap.class) ? (AdapterType) valueMap : super.adaptTo(type);
        }

        @Override
        public ValueMap getValueMap() {
            return valueMap;
        }
    }
}
