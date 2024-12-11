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

package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

@Component(service = InvalidateCacheSupport.class, immediate = true)
public class InvalidateCacheSupport {

    public static final String INVALIDATE_WORKING_AREA = "/var/cif";
    public static final String NODE_NAME_BASE = "invalidate_entry";
    public static final String SERVICE_USER = "cif-flush";
    public static final String PROPERTIES_GRAPHQL_CLIENT_ID = "cq:catalogIdentifier";
    public static final String PROPERTIES_STORE_VIEW = "magentoStore";
    public static final String PROPERTIES_TYPE = "type";
    public static final String PROPERTIES_STORE_PATH = "storePath";
    public static final String PROPERTIES_INVALID_CACHE_ENTRIES = "invalidCacheEntries";
    public static final String PROPERTIES_ATTRIBUTE = "attribute";
    public static final String PROPERTIES_LIST_OF_CACHE_TO_SEARCH = "listOfCacheToSearch";
    public static final String TYPE_SKU = "skus";
    public static final String TYPE_CATEGORY = "categories";
    public static final String TYPE_UUIDS = "uuids";
    public static final String TYPE_ClEAR_SPECIFIC_CACHE = "clearSpecificCache";
    public static final String TYPE_ATTRIBUTE = "attribute";
    public static final String TYPE_CLEAR_ALL = "clearAll";

    @Reference
    private ServiceUserService serviceUserService;

    private final Collection<ClientHolder> clients = new ArrayList<>();

    public GraphqlClient getClient(String graphqlClientId) {
        if (graphqlClientId != null && !graphqlClientId.isEmpty()) {
            for (ClientHolder clientHolder : clients) {
                GraphqlClient graphqlClient = clientHolder.graphqlClient;
                Map<String, Object> properties = clientHolder.properties;
                String identifier = (String) properties.get("identifier");
                if (identifier.equals(graphqlClientId)) {
                    return graphqlClient;
                }
            }
        }
        throw new IllegalStateException("GraphqlClient with ID '" + graphqlClientId + "' not found");
    }

    @Reference(
        service = GraphqlClient.class,
        bind = "bindGraphqlClient",
        unbind = "unbindGraphqlClient",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
    void bindGraphqlClient(GraphqlClient graphqlClient, Map<String, Object> properties) {
        clients.add(new ClientHolder(graphqlClient, properties));
    }

    void unbindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        clients.removeIf(holder -> holder.graphqlClient.equals(graphqlClient));
    }

    private static class ClientHolder {
        private final GraphqlClient graphqlClient;
        private final Map<String, Object> properties;

        ClientHolder(GraphqlClient graphqlClient, Map<String, Object> properties) {
            this.graphqlClient = graphqlClient;
            this.properties = properties;
        }
    }

    public ComponentsConfiguration getCommerceProperties(ResourceResolver resourceResolver, String storePath) {
        Resource resourceStorePath = getResource(resourceResolver, storePath);
        return resourceStorePath != null ? resourceStorePath.adaptTo(ComponentsConfiguration.class) : null;
    }

    public Resource getResource(ResourceResolver resourceResolver, String path) {
        try {
            return resourceResolver.getResource(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResourceResolver getResourceResolver() throws LoginException {
        return serviceUserService.getServiceUserResourceResolver(SERVICE_USER);
    }
}
