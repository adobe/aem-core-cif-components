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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.testing.mock.caconfig.ContextPlugins;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.internal.services.ComponentsConfigurationAdapterFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.mockito.Mockito.*;

public class InvalidateCacheImplTest {

    private static final String TEST_PATH_WITH_VALID_DATA = InvalidateCacheSupport.INVALIDATE_WORKING_AREA + "/testWithValidData";

    private static final String TEST_INVALID_STORE_PATH = "/invalid/path";
    private static final String TEST_STORE_PATH = "/content/venia/us/en";

    private static final String TEST_CONFIG_COMMERCE_PATH = "/conf/venia";
    private static final String TEST_GRAPHQL_CLIENT = "default";

    @Rule
    public final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).plugin(ContextPlugins.CACONFIG)
        .beforeSetUp(context -> {
            ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
            Configuration serviceConfiguration = configurationAdmin.getConfiguration(
                "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy");

            Dictionary<String, Object> props = new Hashtable<>();
            props.put("configRefResourceNames", new String[] { ".x", "jcr:content" });
            props.put("configRefPropertyNames", "cq:conf");
            serviceConfiguration.update(props);

            serviceConfiguration = configurationAdmin.getConfiguration(
                "org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy");
            props = new Hashtable<>();
            props.put("configPath", "/conf");
            serviceConfiguration.update(props);

            serviceConfiguration = configurationAdmin.getConfiguration("org.apache.sling.caconfig.impl.ConfigurationResolverImpl");
            props = new Hashtable<>();
            props.put("configBucketNames", new String[] { "settings" });
            serviceConfiguration.update(props);
        }).build();

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private InvalidateCacheRegistry invalidateCacheRegistry;

    private InvalidateCacheImpl invalidateCache;

    @Mock
    private Logger logger;

    @Mock
    private InvalidationStrategies invalidationStrategies;

    @Mock
    private StrategyInfo strategyInfo;

    @Mock
    private CacheInvalidationStrategy mockStrategy;

    @Mock
    private InvalidationStrategies productSkusStrategies;

    @Mock
    private InvalidationStrategies categoryUidsStrategies;

    @Mock
    private InvalidationStrategies regexPatternsStrategies;

    @Mock
    private StrategyInfo productSkusStrategyInfo;

    @Mock
    private StrategyInfo categoryUidsStrategyInfo;

    @Mock
    private StrategyInfo regexPatternsStrategyInfo;

    @Mock
    private CacheInvalidationStrategy productSkusStrategy;

    @Mock
    private GraphqlClient client;

    @Mock
    private CacheInvalidationStrategy categoryUidsStrategy;

    @Mock
    private CacheInvalidationStrategy regexPatternsStrategy;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        context.registerService(InvalidateCacheSupport.class, invalidateCacheSupport);
        context.registerService(InvalidateCacheRegistry.class, invalidateCacheRegistry);
        context.load().json("/invalidate-cache/jcr-conf-valid-commerce-store.json", TEST_CONFIG_COMMERCE_PATH);
        context.load().json("/invalidate-cache/jcr-cif-cache-invalidate.json", InvalidateCacheSupport.INVALIDATE_WORKING_AREA);
        context.load().json("/invalidate-cache/jcr-storepath-page.json", "/content/venia");

        invalidateCache = context.registerInjectActivateService(new InvalidateCacheImpl());
        ServiceUserMapped serviceUserMapped = Mockito.mock(ServiceUserMapped.class);
        context.registerService(ServiceUserMapped.class, serviceUserMapped, ImmutableMap.of(ServiceUserMapped.SUBSERVICENAME,
            "cif-components-configuration"));

        ComponentsConfigurationAdapterFactory factory = new ComponentsConfigurationAdapterFactory();
        context.registerInjectActivateService(factory);
        // Use the AemContext to get the ResourceResolver
        ResourceResolver resourceResolver = spy(context.resourceResolver());
        doNothing().when(resourceResolver).close();
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(resourceResolver);
        when(invalidateCacheSupport.getClient(any())).thenReturn(Mockito.mock(GraphqlClient.class));

        // Mock the InvalidateCacheRegistry to return expected invalidateTypes
        Set<String> invalidateTypes = new HashSet<>(Arrays.asList("productSkus", "categoryUids", "regexPatterns"));
        when(invalidateCacheRegistry.getInvalidationTypes()).thenReturn(invalidateTypes);

        // Mock strategies for productSkus
        when(invalidateCacheRegistry.getInvalidationStrategies("productSkus")).thenReturn(productSkusStrategies);
        when(productSkusStrategies.getStrategies(false)).thenReturn(Collections.singletonList(productSkusStrategyInfo));
        when(productSkusStrategyInfo.getStrategy()).thenReturn(productSkusStrategy);
        when(productSkusStrategy.getPatterns(any(String[].class))).thenReturn(Collections.singletonList("\"sku\":\\s*\""));

        // Mock strategies for categoryUids
        when(invalidateCacheRegistry.getInvalidationStrategies("categoryUids")).thenReturn(categoryUidsStrategies);
        when(categoryUidsStrategies.getStrategies(false)).thenReturn(Collections.singletonList(categoryUidsStrategyInfo));
        when(categoryUidsStrategyInfo.getStrategy()).thenReturn(categoryUidsStrategy);
        when(categoryUidsStrategy.getPatterns(any(String[].class))).thenReturn(Collections.singletonList(
            "\"uid\"\\s*:\\s*\\{\"id\"\\s*:\\s*\""));

        // Mock strategies for regexPatterns
        when(invalidateCacheRegistry.getInvalidationStrategies("regexPatterns")).thenReturn(regexPatternsStrategies);
        when(regexPatternsStrategies.getStrategies(false)).thenReturn(Collections.singletonList(regexPatternsStrategyInfo));
        when(regexPatternsStrategyInfo.getStrategy()).thenReturn(regexPatternsStrategy);
        when(regexPatternsStrategy.getPatterns(any(String[].class))).thenReturn(null);

        setLoggerField();
    }

    private void setLoggerField() {
        try {
            Field loggerField = InvalidateCacheImpl.class.getDeclaredField("LOGGER");
            loggerField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(loggerField, loggerField.getModifiers() & ~Modifier.FINAL);
            loggerField.set(null, logger);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set logger field", e);
        }
    }

    private void setComponentConfiguration() {
        Resource resource = context.resourceResolver().getResource(TEST_STORE_PATH);
        ComponentsConfiguration componentsConfiguration = resource.adaptTo(ComponentsConfiguration.class);
        when(invalidateCacheSupport.getCommerceProperties(any(), eq(TEST_STORE_PATH))).thenReturn(componentsConfiguration);
    }

    @Test
    public void testResourceResolverNotFound() {
        when(invalidateCacheSupport.getServiceUserResourceResolver()).thenReturn(null);
        invalidateCache.invalidateCache(TEST_INVALID_STORE_PATH);
        verify(logger).error(eq("Error processing JCR event: {}"), eq(null), any(NullPointerException.class));
    }

    @Test
    public void testResourceNotFound() {
        invalidateCache.invalidateCache(TEST_INVALID_STORE_PATH);
        verify(logger).debug("Resource not found at path: {}", TEST_INVALID_STORE_PATH);
    }

    @Test
    public void testCommercePropertiesNotFound() {
        invalidateCache.invalidateCache(TEST_PATH_WITH_VALID_DATA);
        verify(logger).debug("Commerce data not found at path: {}", TEST_PATH_WITH_VALID_DATA);
    }

    @Test
    public void testCommercePropertiesFound() {
        setComponentConfiguration();
        invalidateCache.invalidateCache(TEST_PATH_WITH_VALID_DATA);
        verify(logger, never()).debug("Commerce data not found at path: {}", TEST_STORE_PATH);
    }

    @Test
    public void testGraphqlClientIdNotFoundCommerceProperties() {
        ValueMap mockConfiguration = new ValueMapDecorator(ImmutableMap.of("magentoStore",
            "my-store", "enableUIDSupport", "true"));
        ComponentsConfiguration componentsConfiguration = new ComponentsConfiguration(mockConfiguration);
        when(invalidateCacheSupport.getCommerceProperties(any(), eq(TEST_STORE_PATH))).thenReturn(componentsConfiguration);

        invalidateCache.invalidateCache(TEST_PATH_WITH_VALID_DATA);
        verify(logger).debug("GraphQL client ID not found in commerce properties");
    }

    @Test
    public void testGraphqlClientNotFound() {
        setComponentConfiguration();
        when(invalidateCacheSupport.getClient(any())).thenReturn(null);
        invalidateCache.invalidateCache(TEST_PATH_WITH_VALID_DATA);
        verify(logger).debug("GraphQL client not found for ID: {}", TEST_GRAPHQL_CLIENT);
    }

    @Test
    public void testInvalidateCache() {
        setComponentConfiguration();

        // Execute the invalidateCache method
        invalidateCache.invalidateCache(TEST_PATH_WITH_VALID_DATA);

        // Verify interactions for productSkus
        verify(invalidateCacheRegistry).getInvalidationStrategies("productSkus");
        verify(productSkusStrategies).getStrategies(false);
        verify(productSkusStrategyInfo).getStrategy();
        verify(productSkusStrategy).getPatterns(any(String[].class));

        // Verify interactions for categoryUids
        verify(invalidateCacheRegistry).getInvalidationStrategies("categoryUids");
        verify(categoryUidsStrategies).getStrategies(false);
        verify(categoryUidsStrategyInfo).getStrategy();
        verify(categoryUidsStrategy).getPatterns(any(String[].class));

        // Verify interactions for regexPatterns
        verify(invalidateCacheRegistry).getInvalidationStrategies("regexPatterns");
        verify(regexPatternsStrategies).getStrategies(false);
        verify(regexPatternsStrategyInfo).getStrategy();
        verify(regexPatternsStrategy).getPatterns(any(String[].class));
    }

    @Test
    public void testSuccessfulFullCacheInvalidation() throws Exception {
        doNothing().when(client).invalidateCache(anyString(), any(String[].class), any(String[].class));

        Method invalidateFullCache = InvalidateCacheImpl.class.getDeclaredMethod("invalidateFullCache", GraphqlClient.class, String.class);
        invalidateFullCache.setAccessible(true);
        invalidateFullCache.invoke(invalidateCache, client, "default");

        verify(logger).debug("Successfully performed full cache invalidation for store view: {}", "default");
    }

    @Test
    public void testNoCachePatternsGenerated() throws Exception {
        Map<String, String[]> dynamicProperties = new HashMap<>();
        dynamicProperties.put("invalidationType", new String[0]);

        Method invalidateCacheByType = InvalidateCacheImpl.class.getDeclaredMethod("invalidateCacheByType", GraphqlClient.class,
            String.class, Map.class);
        invalidateCacheByType.setAccessible(true);
        invalidateCacheByType.invoke(invalidateCache, client, "default", dynamicProperties);

        verify(logger).debug("No cache patterns generated for invalidationType: {}", "invalidationType");
    }

}
