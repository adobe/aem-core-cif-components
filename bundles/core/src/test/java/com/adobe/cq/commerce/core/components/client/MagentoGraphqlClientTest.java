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

package com.adobe.cq.commerce.core.components.client;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.caconfig.ContextPlugins;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.core.components.services.ComponentsConfigurationProvider;
import com.adobe.cq.commerce.graphql.client.CachingStrategy;
import com.adobe.cq.commerce.graphql.client.CachingStrategy.DataFetchingPolicy;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MagentoGraphqlClientTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient","default","magentoStore","my-store"));

    private GraphqlClient graphqlClient;

    private ComponentsConfigurationProvider configurationProvider;

    @Rule
    public final AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).plugin(ContextPlugins.CACONFIG)
        .beforeSetUp(context -> {
            ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
            Configuration serviceConfiguration = configurationAdmin.getConfiguration(
                "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy");

            Dictionary<String, Object> props = new Hashtable<>();
            props.put("configRefResourceNames", new String[] { ".", "jcr:content" });
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

    @Before
    public void setup() throws IOException {

        context.load()
            .json("/context/jcr-content.json", "/content");
        context.load()
            .json("/context/jcr-conf.json", "/conf/test-config");
        graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(graphqlClient.execute(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(null);

        configurationProvider = mock(ComponentsConfigurationProvider.class);
        Resource mockConfigurationResource = mock(Resource.class);
        when(configurationProvider.getContextConfigurationResource(anyString())).thenReturn(mockConfigurationResource);
        when(mockConfigurationResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
    }

    private void testMagentoStoreProperty(Resource resource, boolean withStoreHeader) {
        Mockito.when(resource.adaptTo(GraphqlClient.class))
            .thenReturn(graphqlClient);

        Resource mockConfigurationResource = mock(Resource.class);
        when(configurationProvider.getContextConfigurationResource(anyString())).thenReturn(mockConfigurationResource);
        when(mockConfigurationResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(configurationProvider.getContextAwareConfigurationProperties(anyString())).thenReturn(new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient","default")));

        MagentoGraphqlClient client = MagentoGraphqlClient.create(configurationProvider, resource);
        executeAndCheck(withStoreHeader, client);
    }

    private void executeAndCheck(boolean withStoreHeader, MagentoGraphqlClient client) {
        // Verify parameters with default execute() method and store property
        client.execute("{dummy}");
        List<Header> headers = withStoreHeader ? Collections.singletonList(new BasicHeader("Store", "my-store")) : Collections.emptyList();
        RequestOptionsMatcher matcher = new RequestOptionsMatcher(headers, null);
        Mockito.verify(graphqlClient)
            .execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));

        // Verify setting a custom HTTP method
        client.execute("{dummy}", HttpMethod.GET);
        matcher = new RequestOptionsMatcher(headers, HttpMethod.GET);
        Mockito.verify(graphqlClient)
            .execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));
    }

    @Test
    public void testMagentoStorePropertyWithConfigBuilder() {
        /*
         * The content for this test looks slightly different than it does in AEM:
         * In AEM there the tree structure is /conf/<config>/settings/cloudconfigs/commerce/jcr:content
         * In our test content it's /conf/<config>/settings/cloudconfigs/commerce
         * The reason is that AEM has a specific CaConfig API implementation that reads the configuration
         * data from the jcr:content node of the configuration page, something which we cannot reproduce in
         * a unit test scenario.
         */
        Page pageWithConfig = Mockito.spy(context.pageManager().getPage("/content/pageG"));
        Resource pageResource = Mockito.spy(pageWithConfig.adaptTo(Resource.class));
        when(pageWithConfig.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Resource mockConfigurationResource = mock(Resource.class);
        when(configurationProvider.getContextConfigurationResource(anyString())).thenReturn(mockConfigurationResource);
        when(mockConfigurationResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
        when(configurationProvider.getContextAwareConfigurationProperties(anyString())).thenReturn(MOCK_CONFIGURATION);

        MagentoGraphqlClient client = MagentoGraphqlClient.create(configurationProvider, pageWithConfig
            .adaptTo(Resource.class), pageWithConfig);
        Assert.assertNotNull("GraphQL client created successfully", client);
        executeAndCheck(true, client);
    }

    @Test
    public void testCachingStrategyParametersForComponents() {
        Resource resource = context.resourceResolver().getResource("/content/pageA/jcr:content/root/responsivegrid/product");
        testCachingStrategyParameters(resource);
    }

    @Test
    public void testCachingStrategyParametersForOsgiService() {
        Resource resource = new SyntheticResource(null, (String) null, "com.adobe.myosgiservice");
        testCachingStrategyParameters(resource);
    }

    private void testCachingStrategyParameters(Resource resource) {
        Page page = Mockito.spy(context.pageManager().getPage("/content/pageA"));
        Resource pageResource = Mockito.spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        when(configurationProvider.getContextAwareConfigurationProperties(anyString())).thenReturn(MOCK_CONFIGURATION);

        MagentoGraphqlClient client = MagentoGraphqlClient.create(configurationProvider, resource, page);
        client.execute("{dummy}");

        ArgumentCaptor<RequestOptions> captor = ArgumentCaptor.forClass(RequestOptions.class);
        verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), captor.capture());

        CachingStrategy cachingStrategy = captor.getValue().getCachingStrategy();
        Assert.assertEquals(resource.getResourceType(), cachingStrategy.getCacheName());
        Assert.assertEquals(DataFetchingPolicy.CACHE_FIRST, cachingStrategy.getDataFetchingPolicy());
    }

    @Test
    public void testMagentoStoreProperty() {
        // Get page which has the magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageA"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testInheritedMagentoStoreProperty() {
        // Get page whose parent has the magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageB/pageC"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testMissingMagentoStoreProperty() {
        // Get page whose parent has the magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageD"));
        testMagentoStoreProperty(resource, false);
    }

    @Test
    public void testOldMagentoStoreProperty() {
        // Get page which has the old cq:magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageE"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testNewMagentoStoreProperty() {
        // Get page which has both the new magentoStore property and old cq:magentoStore property
        // in its jcr:content node and make sure the new one is prefered
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageF"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testError() {
        // Get page which has the magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver()
            .getResource("/content/pageA"));
        Mockito.when(resource.adaptTo(GraphqlClient.class))
            .thenReturn(null);

        MagentoGraphqlClient client = MagentoGraphqlClient.create(Mockito.mock(ComponentsConfigurationProvider.class), resource);
        Assert.assertNull(client);
    }

    /**
     * Matcher class used to check that the RequestOptions added by the wrapper are correct.
     */
    private static class RequestOptionsMatcher extends ArgumentMatcher<RequestOptions> {

        private List<Header> headers;

        private HttpMethod httpMethod;

        public RequestOptionsMatcher(List<Header> headers, HttpMethod httpMethod) {
            this.headers = headers;
            this.httpMethod = httpMethod;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof RequestOptions)) {
                return false;
            }
            RequestOptions requestOptions = (RequestOptions) obj;
            try {
                // We expect a RequestOptions object with the custom Magento deserializer
                // and the same headers as the list given in the constructor

                if (requestOptions.getGson() != QueryDeserializer.getGson()) {
                    return false;
                }

                for (Header header : headers) {
                    if (!requestOptions.getHeaders()
                        .stream()
                        .anyMatch(h -> h.getName()
                            .equals(header.getName()) && h.getValue()
                                .equals(header.getValue()))) {
                        return false;
                    }
                }

                if (httpMethod != null && !httpMethod.equals(requestOptions.getHttpMethod())) {
                    return false;
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
