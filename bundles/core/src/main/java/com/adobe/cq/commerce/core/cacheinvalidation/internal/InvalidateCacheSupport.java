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
    public static final String PROPERTIES_INVALIDATE_ALL = "invalidateAll";
    public static final String PROPERTIES_CACHE_NAME = "cacheNames";
    public static final String PROPERTY_INVALIDATE_REQUEST_PARAMETER = "invalidateRequestParameter";
    public static final String HTML_SUFFIX = ".html";
    public static final String DISPATCHER_BASE_URL = "dispatcherBaseUrl";
    public static final String DISPATCHER_BASE_PATH_CONFIG = "dispatcherBasePathConfiguration";
    public static final String DISPATCHER_URL_PATH_CONFIG = "dispatcherUrlPathConfiguration";

    private Boolean enableDispatcherCacheInvalidation;
    private String dispatcherBaseUrl;
    private DispatcherUrlPathConfigurationList dispatcherUrlPathConfigurationList;
    private DispatcherBasePathConfiguration dispatcherBasePathConfiguration;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Collection<ClientHolder> clients = new ArrayList<>();

    @Activate
    protected void activate(Map<String, Object> properties) {
        initializeDispatcherConfig(properties);
        initializeUrlPathConfigurations(properties);
    }

    @Deactivate
    protected void deactivate() {
        this.enableDispatcherCacheInvalidation = false;
        this.dispatcherBaseUrl = null;
        this.dispatcherUrlPathConfigurationList = null;
        this.dispatcherBasePathConfiguration = null;
    }

    private void initializeDispatcherConfig(Map<String, Object> properties) {
        this.enableDispatcherCacheInvalidation = Optional.ofNullable((Boolean) properties.get("enableDispatcherCacheInvalidation"))
            .orElse(false);
        this.dispatcherBaseUrl = (String) properties.get(DISPATCHER_BASE_URL);
        this.dispatcherBasePathConfiguration = parseBasePathConfig(properties);
    }

    private DispatcherBasePathConfiguration parseBasePathConfig(Map<String, Object> properties) {
        String config = (String) properties.get(DISPATCHER_BASE_PATH_CONFIG);
        if (config != null && !config.trim().isEmpty()) {
            String[] parts = config.split(":");
            if (parts.length == 2) {
                return new DispatcherBasePathConfiguration(parts[0], parts[1]);
            }
        }
        return DispatcherBasePathConfiguration.createDefault();
    }

    private void initializeUrlPathConfigurations(Map<String, Object> properties) {
        this.dispatcherUrlPathConfigurationList = Optional.ofNullable((String[]) properties.get(DISPATCHER_URL_PATH_CONFIG))
            .map(configs -> parseUrlPathConfigurations(configs, new String[] {
                dispatcherBasePathConfiguration.getPattern(),
                dispatcherBasePathConfiguration.getMatch()
            }))
            .orElseGet(() -> new DispatcherUrlPathConfigurationList(new HashMap<>()));
    }

    private DispatcherUrlPathConfigurationList parseUrlPathConfigurations(String[] configs, String[] basePathParts) {
        Map<String, List<PatternConfig>> configMap = new HashMap<>();

        for (String config : configs) {
            String[] parts = config.split(":");
            if (parts.length == 3) {
                String urlPathType = parts[0].replaceAll("-\\d+$", "");
                String pattern = basePathParts[0] + parts[1];

                // Count capture groups in base path pattern
                int baseGroupCount = countCaptureGroups(basePathParts[0]);
                // Adjust the match replacement indices
                String match = basePathParts[1] + adjustCaptureGroupReferences(parts[2], baseGroupCount);

                PatternConfig patternConfig = new PatternConfig(pattern, match);
                configMap.computeIfAbsent(urlPathType, k -> new ArrayList<>())
                    .add(patternConfig);
            }
        }
        return new DispatcherUrlPathConfigurationList(configMap);
    }

    public List<PatternConfig> getDispatcherUrlConfigurationForType(String urlPathType) {
        return dispatcherUrlPathConfigurationList.getPatternConfigsForType(urlPathType);
    }

    public Boolean getEnableDispatcherCacheInvalidation() {
        return enableDispatcherCacheInvalidation;
    }

    public String getDispatcherBaseUrl() {
        return dispatcherBaseUrl;
    }

    public String getDispatcherBasePathForStorePath(String storePath) {
        String pattern = dispatcherBasePathConfiguration.getPattern();
        if (pattern.isEmpty()) {
            return storePath;
        }

        if (storePath.matches(pattern)) {
            return storePath.replaceAll(pattern, dispatcherBasePathConfiguration.getMatch());
        }

        return storePath;
    }

    public List<PatternConfig> getDispatcherUrlConfigurationBasedOnType(String urlPathType) {
        if (urlPathType == null || urlPathType.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return dispatcherUrlPathConfigurationList.getPatternConfigsForType(urlPathType);
    }

    public GraphqlClient getClient(String graphqlClientId) {
        if (graphqlClientId == null || graphqlClientId.trim().isEmpty()) {
            throw new IllegalStateException("GraphqlClient ID cannot be null or empty");
        }

        for (ClientHolder clientHolder : clients) {
            GraphqlClient graphqlClient = clientHolder.graphqlClient;
            Map<String, Object> properties = clientHolder.properties;
            String identifier = (String) properties.get("identifier");
            if (graphqlClientId.equals(identifier)) {
                return graphqlClient;
            }
        }
        throw new IllegalStateException("GraphqlClient with ID '" + graphqlClientId + "' not found");
    }

    private int countCaptureGroups(String pattern) {
        return (int) pattern.chars()
            .filter(c -> c == '(')
            .count();
    }

    private String adjustCaptureGroupReferences(String match, int baseGroupCount) {
        return match.replaceAll("\\$(\\d)", "\\$" + (baseGroupCount + 1));
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
            throw new IllegalStateException("Failed to obtain ResourceResolver for service user: " + SERVICE_USER, e);
        }

        return resourceResolver;
    }

    public String convertUrlPath(String urlPath) {
        if (urlPath == null) {
            return urlPath;
        }

        // Check all patterns from dispatcherUrlPathConfigurationList
        for (Map.Entry<String, List<PatternConfig>> entry : dispatcherUrlPathConfigurationList.getConfigurations().entrySet()) {
            List<PatternConfig> typePatterns = entry.getValue();
            for (PatternConfig patternConfig : typePatterns) {
                String pattern = patternConfig.getPattern();
                String match = patternConfig.getMatch();
                if (pattern != null && match != null) {
                    Pattern patternObj = Pattern.compile(pattern);
                    Matcher matcher = patternObj.matcher(urlPath);
                    if (matcher.matches()) {
                        return matcher.replaceAll(match);
                    }
                }
            }
        }
        return urlPath;
    }
}
