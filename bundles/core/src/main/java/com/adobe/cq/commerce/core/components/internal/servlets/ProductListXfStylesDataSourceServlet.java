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
package com.adobe.cq.commerce.core.components.internal.servlets;

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

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + ProductListXfStylesDataSourceServlet.RESOURCE_TYPE_V1,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class ProductListXfStylesDataSourceServlet extends SlingSafeMethodsServlet {
    public static final String RESOURCE_TYPE_V1 = "core/wcm/components/commons/datasources/productlistxfstyles/v1";
    public static final String PN_FRAGMENT_STYLES = "fragmentStyles";
    public static final String PN_FRAGMENT_STYLE_CLASS = "fragmentStyleCssClass";
    public static final String PN_FRAGMENT_STYLE_NAME = "fragmentStyleName";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
        throws ServletException, IOException {
        SimpleDataSource xfStylesDataSource = new SimpleDataSource(
            getXfStyles(request).iterator());
        request.setAttribute(DataSource.class.getName(), xfStylesDataSource);
    }

    private List<Resource> getXfStyles(SlingHttpServletRequest request) {
        List<Resource> xfStyles = new ArrayList<>();
        ResourceResolver resolver = request.getResourceResolver();
        Resource contentResource = resolver.getResource((String) request.getAttribute(Value.CONTENTPATH_ATTRIBUTE));

        if (contentResource != null) {
            contentResource = contentResource.getParent().getParent();
        }

        ContentPolicyManager policyManager = resolver.adaptTo(ContentPolicyManager.class);
        if (contentResource != null && policyManager != null) {
            ContentPolicy policy = policyManager.getPolicy(contentResource);
            if (policy != null) {
                String policyPath = policy.getPath();
                Resource policyResource = resolver.getResource(policyPath);
                Resource fragmentStylesResource = policyResource.getChild(PN_FRAGMENT_STYLES);
                if (fragmentStylesResource != null) {
                    fragmentStylesResource.getChildren().forEach(fs -> {
                        ValueMap vm = fs.getValueMap();
                        String cssClassName = vm.get(PN_FRAGMENT_STYLE_CLASS, String.class);
                        String styleName = vm.get(PN_FRAGMENT_STYLE_NAME, String.class);
                        xfStyles.add(new XfStyleResource(cssClassName, styleName, resolver));
                    });
                }
            }
        }
        return xfStyles;
    }

    /**
     * Synthetic resource for a product list experience fragment style that can be
     * used by a
     * Granite form select field.
     */
    static class XfStyleResource extends SyntheticResource {
        public static final String PN_VALUE = "value";
        public static final String PN_TEXT = "text";

        private final String value;
        private final String text;
        private ValueMap valueMap;

        XfStyleResource(String value, String text, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.value = value;
            this.text = text;
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
            return text;
        }

        public String getValue() {
            return value;
        }
    }
}
