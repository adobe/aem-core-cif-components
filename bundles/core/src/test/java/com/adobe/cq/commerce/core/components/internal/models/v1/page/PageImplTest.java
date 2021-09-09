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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter.StoreConfigExporterImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.adobe.cq.wcm.core.components.models.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PageImplTest extends AbstractPageDelegatorTest {

    static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(
        ImmutableMap.of(
            "magentoGraphqlEndpoint", "/my/api/graphql",
            "magentoStore", "my-magento-store",
            "enableUIDSupport", "true",
            "cq:graphqlClient", "my-graphql-client",
            "httpHeaders", new String[] { "customHeader-1=value1", "customHeader-2=value2" }));
    static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = new AemContext((AemContextCallback) context -> {
        context.load().json("/context/jcr-content.json", "/content");
    });

    protected final String pagePath = "/content/pageH";

    /**
     * Parameterizes the testDelegation() tests inherited from {@link AbstractPageDelegatorTest}.
     *
     * @param mock
     * @return
     */
    @Override
    protected Page testDelegationCreateSubject(Page mock) {
        context.request().setAttribute(MockPage.class.getName(), mock);
        return context.request().adaptTo(Page.class);
    }

    @Before
    public void setup() throws PersistenceException {
        context.registerAdapter(Resource.class, ComponentsConfiguration.class, MOCK_CONFIGURATION_OBJECT);
        context.addModelsForClasses(MockPage.class);

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, context.currentPage(pagePath));
        slingBindings.setResource(context.currentPage().getContentResource());

        // provide a graphql client
        GraphqlClient graphqlClient = mock(GraphqlClient.class);
        when(graphqlClient.getConfiguration()).thenReturn(mock(GraphqlClientConfiguration.class));
        when(graphqlClient.getConfiguration().httpMethod()).thenReturn(HttpMethod.GET);
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);

        // set a mock resourceSuperType to inject the MockPage
        ModifiableValueMap pageContent = context.resourceResolver().getResource(pagePath + "/jcr:content")
            .adaptTo(ModifiableValueMap.class);
        pageContent.put("sling:resourceSuperType", "mock");
        pageContent.put("navRoot", Boolean.TRUE);

        context.resourceResolver().commit();
        context.currentResource(context.currentPage().getContentResource());
    }

    @Test
    public void testHtmlPageItemExtendedStoreConfig() {
        Page mock = mock(Page.class);
        HtmlPageItem existingItem1 = mock(HtmlPageItem.class);
        HtmlPageItem existingItem2 = mock(HtmlPageItem.class);
        context.request().setAttribute(MockPage.class.getName(), mock);
        when(mock.getHtmlPageItems()).thenReturn(Arrays.asList(existingItem1, existingItem2));

        Page subject = context.request().adaptTo(Page.class);
        assertNotNull(subject);

        List<HtmlPageItem> htmlPageItems = subject.getHtmlPageItems();
        assertNotNull(htmlPageItems);
        assertEquals(3, htmlPageItems.size());

        assertThat(htmlPageItems).contains(existingItem1, existingItem2);
    }

    @Test
    public void testHtmlPageItemExtendedStoreConfigOnlyOnce() {
        Page mock = mock(Page.class);
        HtmlPageItem existingItem1 = new StoreConfigHtmlPageItem(new StoreConfigExporterImpl());
        context.request().setAttribute(MockPage.class.getName(), mock);
        when(mock.getHtmlPageItems()).thenReturn(Collections.singletonList(existingItem1));

        Page subject = context.request().adaptTo(Page.class);
        assertNotNull(subject);

        List<HtmlPageItem> htmlPageItems = subject.getHtmlPageItems();
        assertNotNull(htmlPageItems);
        assertEquals(1, htmlPageItems.size());

        assertThat(htmlPageItems).contains(existingItem1);
    }

    @Test
    public void testHtmlPageItemContainsStoreConfig() throws IOException {
        Page subject = context.request().adaptTo(Page.class);
        assertNotNull(subject);

        List<HtmlPageItem> htmlPageItems = subject.getHtmlPageItems();
        assertNotNull(htmlPageItems);
        assertEquals(1, htmlPageItems.size());

        HtmlPageItem htmlPageItem = htmlPageItems.get(0);
        assertEquals(HtmlPageItem.Element.META, htmlPageItem.getElement());
        assertEquals(HtmlPageItem.Location.HEADER, htmlPageItem.getLocation());

        Map<String, String> attributes = htmlPageItem.getAttributes();
        assertEquals("store-config", attributes.get("name"));

        String content = attributes.getOrDefault("content", "{}");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expected = mapper.readTree("{"
            + "\"endpoint\": \"/my/api/graphql\","
            + "\"storeView\": \"my-magento-store\","
            + "\"graphqlMethod\": \"GET\","
            + "\"storeRootUrl\": \"/content/pageH.html\","
            + "\"headers\": {"
            + "\"customHeader-1\": \"value1\","
            + "\"customHeader-2\": \"value2\","
            + "\"Store\": \"my-magento-store\""
            + "}"
            + "}");
        JsonNode actual = mapper.readTree(new StringReader(content));
        assertEquals(expected, actual);
    }
}
