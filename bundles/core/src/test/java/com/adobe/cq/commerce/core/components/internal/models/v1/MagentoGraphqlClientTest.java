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

package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

public class MagentoGraphqlClientTest {

    private GraphqlClient graphqlClient;

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
            },
            ResourceResolverType.JCR_MOCK);
    }

    @Before
    public void setup() {
        graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(graphqlClient.execute(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(null);
    }

    private void testMagentoStoreProperty(Resource resource, boolean withStoreHeader) {
        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        MagentoGraphqlClient client = MagentoGraphqlClient.create(resource);
        client.execute("{dummy}");

        List<Header> headers = withStoreHeader ? Collections.singletonList(new BasicHeader("Store", "my-store")) : Collections.emptyList();
        RequestOptionsMatcher matcher = new RequestOptionsMatcher(headers);

        Mockito.verify(graphqlClient).execute(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.argThat(matcher));
    }

    @Test
    public void testMagentoStoreProperty() {
        // Get page which has the cq:magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver().getResource("/content/pageA"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testInheritedMagentoStoreProperty() {
        // Get page whose parent has the cq:magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver().getResource("/content/pageB/pageC"));
        testMagentoStoreProperty(resource, true);
    }

    @Test
    public void testMissingMagentoStoreProperty() {
        // Get page whose parent has the cq:magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver().getResource("/content/pageD"));
        testMagentoStoreProperty(resource, false);
    }

    @Test
    public void testError() {
        // Get page which has the cq:magentoStore property in its jcr:content node
        Resource resource = Mockito.spy(context.resourceResolver().getResource("/content/pageA"));
        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(null);

        MagentoGraphqlClient client = MagentoGraphqlClient.create(resource);
        Assert.assertNull(client);
    }

    /**
     * Matcher class used to check that the RequestOptions added by the wrapper are correct.
     */
    private static class RequestOptionsMatcher extends ArgumentMatcher<RequestOptions> {

        private List<Header> headers;

        public RequestOptionsMatcher(List<Header> headers) {
            this.headers = headers;
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
                    if (!requestOptions.getHeaders().stream().anyMatch(
                        h -> h.getName().equals(header.getName()) && h.getValue().equals(header.getValue()))) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
