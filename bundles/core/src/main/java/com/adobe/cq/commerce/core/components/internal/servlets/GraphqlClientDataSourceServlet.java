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

package com.adobe.cq.commerce.core.components.internal.servlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.i18n.I18n;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + GraphqlClientDataSourceServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class GraphqlClientDataSourceServlet extends SlingSafeMethodsServlet {

    public final static String RESOURCE_TYPE = "core/cif/components/page/v1/datasource/graphqlclients";

    private I18n i18n;

    private Set<String> identifiers = new ConcurrentHashSet<>();

    @Reference
    private ExpressionResolver expressionResolver;

    @Reference(
        service = GraphqlClient.class,
        bind = "bindGraphqlClient",
        unbind = "unbindGraphqlClient",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
    void bindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        identifiers.add(graphqlClient.getIdentifier());
    }

    void unbindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        identifiers.remove(graphqlClient.getIdentifier());
    }

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        i18n = new I18n(request);
        SimpleDataSource graphqlClientDataSource = new SimpleDataSource(getGraphqlClients(request).iterator());
        request.setAttribute(DataSource.class.getName(), graphqlClientDataSource);
    }

    List<Resource> getGraphqlClients(@Nonnull SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> graphqlClients = new ArrayList<>();

        final Config cfg = new Config(request.getResource().getChild(Config.DATASOURCE));
        boolean showEmptyOption = false;
        if (cfg != null) {
            ExpressionHelper expressionHelper = new ExpressionHelper(expressionResolver, request);
            showEmptyOption = expressionHelper.getBoolean(cfg.get("showEmptyOption"));
        }
        // Add empty option
        if (showEmptyOption) {
            graphqlClients.add(new GraphqlClientResource(i18n.get("Inherit", "Inherit property"), StringUtils.EMPTY, resolver));
        }
        // Add other configurations
        for (String identifier : identifiers) {
            graphqlClients.add(new GraphqlClientResource(identifier, identifier, resolver));
        }

        return graphqlClients;
    }

    protected static class GraphqlClientResource extends SyntheticResource {

        protected static final String PN_VALUE = "value";
        protected static final String PN_TEXT = "text";

        private String name;
        private String value;
        private ValueMap valueMap;

        GraphqlClientResource(String name, String value, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.name = name;
            this.value = value;
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

        protected String getText() {
            return name;
        }

        protected String getValue() {
            return value;
        }

        protected boolean getSelected() {
            return false;
        }
    }

}
