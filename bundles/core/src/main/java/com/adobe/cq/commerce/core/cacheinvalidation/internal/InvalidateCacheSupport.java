/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.*;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

@Component(
    service = InvalidateCacheSupport.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    immediate = true)
public class InvalidateCacheSupport {

    public static final String INVALIDATE_WORKING_AREA = "/var/cif/cacheInvalidation";
    public static final String NODE_NAME_BASE = "cmd";
    public static final String SERVICE_USER = "cif-cache-invalidation-service";
    public static final String PROPERTIES_GRAPHQL_CLIENT_ID = "cq:graphqlClient";
    public static final String PROPERTIES_STORE_VIEW = "magentoStore";
    public static final String PROPERTIES_STORE_PATH = "storePath";
    public static final String PROPERTIES_CACHE_NAME = "cacheNames";
    public static final String PROPERTIES_PRODUCT_SKUS = "productSkus";
    public static final String PROPERTIES_CATEGORY_UIDS = "categoryUids";
    public static final String PROPERTIES_REGEX_PATTERNS = "regexPatterns";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

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

    void unbindGraphqlClient(GraphqlClient graphqlClient) {
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
            throw new ResourceNotFoundException("Failed to get resource at path: " + path, e);
        }
    }

    public ResourceResolver getServiceUserResourceResolver() {
        ResourceResolver resourceResolver = null;
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, SERVICE_USER);

        try {
            // Get the service user ResourceResolver
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(param);
        } catch (LoginException e) {
            throw new IllegalStateException("Successfully obtained ResourceResolver for service user: " + SERVICE_USER + e);
        }

        return resourceResolver;
    }
}
