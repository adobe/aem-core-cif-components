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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.*;

import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;

@Component(
    service = InvalidateCacheSupport.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    immediate = true)
public class InvalidateCacheSupport {

    public static final String INVALIDATE_WORKING_AREA = "/var/cif/cacheinvalidation";
    public static final String NODE_NAME_BASE = "cmd";
    public static final String SERVICE_USER = "cif-cache-invalidation-service";
    public static final String PROPERTIES_GRAPHQL_CLIENT_ID = "cq:graphqlClient";
    public static final String PROPERTIES_STORE_VIEW = "magentoStore";
    public static final String PROPERTIES_STORE_PATH = "storePath";
    public static final String PROPERTIES_CACHE_NAME = "cacheNames";
    public static final String PROPERTY_INVALIDATE_REQUEST_PARAMETER = "invalidateRequestParameter";
    public static final String HTML_SUFFIX = ".html";
    public static final String DISPATCHER_URL_PATTERN = "pattern";
    public static final String DISPATCHER_URL_MATCH = "match";

    private Boolean enableDispatcherCacheInvalidation;

    private String dispatcherBaseUrl;

    private Map<String, Map<String, Map<String, String>>> dispatcherUrlConfiguration;

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.enableDispatcherCacheInvalidation = Optional.ofNullable((Boolean) properties.get("enableDispatcherCacheInvalidation")).orElse(
            false);
        this.dispatcherBaseUrl = (String) properties.get("dispatcherBaseUrl");
        this.dispatcherUrlConfiguration = Optional.ofNullable((String[]) properties.get("dispatcherUrlConfiguration"))
            .map(this::getDispatcherUrlConfiguration)
            .orElse(new HashMap<>());
    }

    @Deactivate
    protected void deactivate() {
        this.enableDispatcherCacheInvalidation = false;
        this.dispatcherBaseUrl = null;
        this.dispatcherUrlConfiguration = null;
    }

    public Boolean getEnableDispatcherCacheInvalidation() {
        return enableDispatcherCacheInvalidation;
    }

    public String getDispatcherBaseUrl() {
        return dispatcherBaseUrl;
    }

    public Map<String, Map<String, String>> getDispatcherUrlConfigurationBasedOnStorePath(String storePath) {
        return dispatcherUrlConfiguration.getOrDefault(storePath, null);
    }

    private Map<String, Map<String, Map<String, String>>> getDispatcherUrlConfiguration(String[] configurations) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

        for (String config : configurations) {
            String[] parts = config.split(":");
            if (parts.length == 4) {
                String storePath = parts[0];
                String urlPathType = parts[1];
                String matchPattern = parts[2];
                String convertPattern = parts[3];

                result.computeIfAbsent(storePath, k -> new HashMap<>())
                    .computeIfAbsent(urlPathType, k -> new HashMap<>())
                    .put(DISPATCHER_URL_PATTERN, matchPattern);
                result.get(storePath).get(urlPathType).put(DISPATCHER_URL_MATCH, convertPattern);
            }
        }
        return result;
    }

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

    public String convertUrlPath(String pattern, String match, String urlPath) {
        if (pattern != null && match != null) {
            Pattern patternObj = Pattern.compile(pattern);
            Matcher matcher = patternObj.matcher(urlPath);
            if (matcher.matches()) {
                urlPath = matcher.replaceAll(match);
            }
        }
        return urlPath;
    }

    public String extractPagePath(String fullPath) {
        int jcrContentIndex = fullPath.indexOf("/jcr:content");
        return jcrContentIndex != -1 ? fullPath.substring(0, jcrContentIndex) : fullPath;
    }
}
