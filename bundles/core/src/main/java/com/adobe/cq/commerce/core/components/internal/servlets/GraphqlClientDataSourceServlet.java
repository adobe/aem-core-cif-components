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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
import org.jetbrains.annotations.NotNull;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;

@Component(
    service = { Servlet.class },
    property = {
        "sling.servlet.resourceTypes=" + GraphqlClientDataSourceServlet.RESOURCE_TYPE,
        "sling.servlet.methods=GET",
        "sling.servlet.extensions=html"
    })
public class GraphqlClientDataSourceServlet extends SlingSafeMethodsServlet {

    public final static String RESOURCE_TYPE = "core/cif/components/page/v1/datasource/graphqlclients";

    @Reference
    ConfigurationAdmin configurationAdmin;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) {
        SimpleDataSource graphqlClientDataSource = new SimpleDataSource(this.getGraphqlClients(request).iterator());
        request.setAttribute(DataSource.class.getName(), graphqlClientDataSource);
    }

    private List<Resource> getGraphqlClients(@NotNull SlingHttpServletRequest request) {
        ResourceResolver resolver = request.getResourceResolver();
        List<Resource> graphqlClients = new ArrayList<>();

        List<Configuration> configs = null;
        try {
            configs = Arrays.asList(
                configurationAdmin.listConfigurations("(service.factoryPid=com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl)"));
        } catch (Exception e) {
            return graphqlClients;
        }

        for (Configuration config : configs) {
            String identifier = (String) config.getProperties().get("identifier");
            graphqlClients.add(new GraphqlClientResource(identifier, resolver));
        }

        return graphqlClients;
    }

    private static class GraphqlClientResource extends SyntheticResource {

        protected static final String PN_VALUE = "value";
        protected static final String PN_TEXT = "text";

        private String name;
        private ValueMap valueMap;

        GraphqlClientResource(String name, ResourceResolver resourceResolver) {
            super(resourceResolver, StringUtils.EMPTY, RESOURCE_TYPE_NON_EXISTING);
            this.name = name;
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
            return name;
        }

        protected boolean getSelected() {
            return false;
        }
    }

}
