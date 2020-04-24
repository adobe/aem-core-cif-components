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

package com.adobe.cq.commerce.core.search.internal.services;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SearchFilterServiceImplTest {

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");
                context.registerInjectActivateService(new FilterAttributeMetadataCacheImpl());
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/pageA";

    @Mock
    HttpClient httpClient;

    SearchFilterServiceImpl searchFilterServiceUnderTest;
    Resource resource;

    @Before
    public void setup() throws IOException {
        searchFilterServiceUnderTest = context.registerInjectActivateService(new SearchFilterServiceImpl());

        context.currentPage(PAGE);
        context.currentResource(PAGE);
        resource = Mockito.spy(context.resourceResolver().getResource(PAGE));

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Whitebox.setInternalState(graphqlClient, "gson", QueryDeserializer.getGson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);
        Whitebox.setInternalState(graphqlClient, "httpMethod", HttpMethod.POST);

        Utils.setupHttpResponse("graphql/magento-graphql-introspection-result.json", httpClient, HttpStatus.SC_OK, "{__type");
        Utils.setupHttpResponse("graphql/magento-graphql-attributes-result.json", httpClient, HttpStatus.SC_OK, "{customAttributeMetadata");

        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);
    }

    @Test
    public void testRetrieveMetadata() {

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(resource);

        assertThat(filterAttributeMetadata).hasSize(29);

        // Range type
        FilterAttributeMetadata price = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("price")).findFirst()
            .get();
        assertThat(price.getFilterInputType()).isEqualTo("FilterRangeTypeInput");
        assertThat(price.getAttributeType()).isEqualTo("Float");
        assertThat(price.getAttributeInputType()).isEqualTo("price");

        // Equal type for string
        FilterAttributeMetadata material = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("material")).findFirst()
            .get();
        assertThat(material.getFilterInputType()).isEqualTo("FilterEqualTypeInput");
        assertThat(material.getAttributeType()).isEqualTo("String");
        assertThat(material.getAttributeInputType()).isEqualTo("multiselect");

        // Equal type for int/boolean
        FilterAttributeMetadata newAttr = filterAttributeMetadata.stream().filter(f -> f.getAttributeCode().equals("new")).findFirst()
            .get();
        assertThat(newAttr.getFilterInputType()).isEqualTo("FilterEqualTypeInput");
        assertThat(newAttr.getAttributeType()).isEqualTo("Int");
        assertThat(newAttr.getAttributeInputType()).isEqualTo("boolean");
    }

    @Test
    public void testFilterQueriesReturnNull() {
        // We want to make sure that components will not fail if the __type and/or customAttributeMetadata fields are null
        // For example, 3rd-party integrations might not support this immediately

        GraphqlClient graphqlClient = Mockito.mock(GraphqlClient.class);
        Mockito.when(resource.adaptTo(GraphqlClient.class)).thenReturn(graphqlClient);

        Query query = new Query();
        GraphqlResponse<Object, Object> response = new GraphqlResponse<Object, Object>();
        response.setData(query);
        when(graphqlClient.execute(any(), any(), any(), any())).thenReturn(response);

        final List<FilterAttributeMetadata> filterAttributeMetadata = searchFilterServiceUnderTest
            .retrieveCurrentlyAvailableCommerceFilters(resource);
        assertThat(filterAttributeMetadata).hasSize(0);
    }

}
