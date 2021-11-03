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
package com.adobe.cq.commerce.extensions.recommendations.internal.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.drew.lang.annotations.NotNull;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + AllowedRecTypesDataSourceServlet.RESOURCE_TYPE_V1,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class AllowedRecTypesDataSourceServlet extends SlingSafeMethodsServlet {

    public final static String RESOURCE_TYPE_V1 = "core/cif/extensions/product-recs/datasources/allowedrectypes/v1";
    public final static String PN_ALLOWED_TYPES = "allowedRecTypes";

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
        throws ServletException, IOException {
        SimpleDataSource allowedRecTypesDataSource = new SimpleDataSource(getAllowedRecTypes(request).iterator());
        request.setAttribute(DataSource.class.getName(), allowedRecTypesDataSource);
    }

    /**
     * Returns a list of allowed product recommendation types based on the template policy.
     */
    private List<Resource> getAllowedRecTypes(@NotNull SlingHttpServletRequest request) {
        List<Resource> allowedRecTypes = new ArrayList<>();

        ResourceResolver resolver = request.getResourceResolver();
        Resource contentResource = resolver.getResource((String) request.getAttribute(Value.CONTENTPATH_ATTRIBUTE));
        ContentPolicyManager policyManager = resolver.adaptTo(ContentPolicyManager.class);

        if (contentResource == null || policyManager == null) {
            return allowedRecTypes;
        }

        Optional
            .of(contentResource)
            .map(policyManager::getPolicy)
            .map(ContentPolicy::getProperties)
            .map(m -> m.get(PN_ALLOWED_TYPES, String[].class))
            .map(Arrays::stream)
            .orElseGet(Stream::empty)
            .map(RecType::getTypeByValue)
            .filter(Objects::nonNull)
            .forEach(t -> allowedRecTypes.add(new RecTypeResource(t, resolver)));

        return allowedRecTypes;
    }

    /**
     * Synthetic resource for a product recommendation type that can be used by a Granite form select field.
     */
    static class RecTypeResource extends SyntheticResource {
        public static final String PN_VALUE = "value";
        public static final String PN_TEXT = "text";

        private final RecType recType;
        private ValueMap valueMap;

        RecTypeResource(RecType recType, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.recType = recType;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == ValueMap.class) {
                if (valueMap == null) {
                    initValueMap();
                }
                return (AdapterType) valueMap;
            } else {
                return super.adaptTo(type);
            }
        }

        private void initValueMap() {
            valueMap = new ValueMapDecorator(new HashMap<String, Object>());
            valueMap.put(PN_VALUE, getValue());
            valueMap.put(PN_TEXT, getText());
        }

        public String getText() {
            return recType.getText();
        }

        public String getValue() {
            return recType.getValue();
        }
    }

    /**
     * Enum with all available product recommendation types.
     */
    enum RecType {
        MOST_VIEWED("most-viewed", "Most viewed"),
        MOST_PURCHASED("most-purchased", "Most purchased"),
        CONVERSION_RATE_PURCHASE("purchase-session-conversion-rate", "Conversion rate (purchase)"),
        MOST_ADDED_TO_CART("most-added-to-cart", "Most added to cart"),
        CONVERSION_RATE_ADD_TO_CART("add-to-cart-conversion-rate", "Conversion rate (add-to-cart)"),
        TRENDING("trending", "Trending"),
        VIEWED_VIEWED("viewed-viewed", "Viewed this, viewed that"),
        VIEWED_BOUGHT("viewed-bought", "Viewed this, bought that"),
        BOUGHT_BOUGHT("bought-bought", "Bought this, bought that"),
        MORE_LIKE_THIS("more-like-this", "More like this"),
        JUST_FOR_YOU("just-for-you", "Recommended for you"),
        RECENTLY_VIEWED("recently-viewed", "Recently viewed");

        private String value;
        private String text;

        RecType(String value, String text) {
            this.value = value;
            this.text = text;
        }

        private static RecType getTypeByValue(String value) {
            for (RecType recType : values()) {
                if (StringUtils.equalsAnyIgnoreCase(recType.value, value)) {
                    return recType;
                }
            }
            return null;
        }

        public String getValue() {
            return value;
        }

        public String getText() {
            return text;
        }
    }
}
