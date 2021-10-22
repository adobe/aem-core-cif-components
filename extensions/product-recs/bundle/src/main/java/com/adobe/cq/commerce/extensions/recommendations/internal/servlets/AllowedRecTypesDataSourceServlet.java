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
import java.util.HashMap;
import java.util.List;

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
        if (contentResource != null && policyManager != null) {
            ContentPolicy policy = policyManager.getPolicy(contentResource);
            if (policy != null) {
                ValueMap props = policy.getProperties();
                if (props != null) {
                    // String[] headingElements = props.get(PN_ALLOWED_HEADING_ELEMENTS, String[].class);
                    String[] allowedTypeValues = props.get(PN_ALLOWED_TYPES, String[].class);
                    if (allowedTypeValues != null && allowedTypeValues.length > 0) {
                        for (String allowedTypeValue : allowedTypeValues) {
                            allowedRecTypes.add(new RecTypeResource(allowedTypeValue, resolver));
                        }
                    }
                }
            }
        }
        return allowedRecTypes;
    }

    /**
     * Synthetic resource for a product recommendation type that can be used by a Granite form select field.
     */
    private static class RecTypeResource extends SyntheticResource {
        public static final String PN_VALUE = "value";
        public static final String PN_TEXT = "text";
        // public static final String PN_SELECTED = "selected";

        private final String value;
        private final String text;
        private ValueMap valueMap;

        RecTypeResource(String value, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.value = value;

            RecType recType = RecType.getTypeByValue(value);
            this.text = recType.getText();
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
            // valueMap.put(PN_SELECTED, getSelected());
        }

        public String getText() {
            return text;
        }

        public String getValue() {
            return value;
        }

        /*
         * public boolean getSelected() {
         * return false;
         * }
         */
    }

    /**
     * Enum with all available product recommendation types.
     */
    private enum RecType {
        mostViewed("most-viewed", "Most viewed"),
        mostPurchased("most-purchased", "Most purchased"),
        conversionRatePurchase("purchase-session-conversion-rate", "Conversion rate (purchase)"),
        mostAddedToCart("most-added-to-cart", "Most added to cart"),
        conversionRateAddToCart("add-to-cart-conversion-rate", "Conversion rate (add-to-cart)"),
        trending("trending", "Trending"),
        viewedViewed("viewed-viewed", "Viewed this, viewed that"),
        viewedBought("viewed-bought", "Viewed this, bought that"),
        boughtBought("bought-bought", "Bought this, bought that"),
        moreLikeThis("more-like-this", "More like this"),
        justForYou("just-for-you", "Recommended for you"),
        recentlyViewed("recently-viewed", "Recently viewed");

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
