/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.osgi.services.HttpClientBuilderFactory;

import static org.mockito.Mockito.mock;

public class MockHttpClientBuilderFactory implements HttpClientBuilderFactory {

    public final CloseableHttpClient client;

    public MockHttpClientBuilderFactory() {
        this(mock(CloseableHttpClient.class));
    }

    public MockHttpClientBuilderFactory(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public HttpClientBuilder newBuilder() {
        return new MockHttpClientBuilder();
    }

    private class MockHttpClientBuilder extends HttpClientBuilder {
        @Override
        public CloseableHttpClient build() {
            return client;
        }
    }
}
