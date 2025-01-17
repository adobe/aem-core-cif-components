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
package com.adobe.cq.commerce.core.components.internal.models.v2.button;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.wcm.core.components.models.Button;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ButtonImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    private static final String PAGE = "/content/pageA";
    private Button button;

    @Before
    public void setUp() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        GraphqlClient graphqlClient = spy(new GraphqlClientImpl());
        Utils.activateGraphqlClient(context, graphqlClient, null);
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK, "{categoryList");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");

        Page page = spy(context.currentPage(PAGE));

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PAGE_MANAGER, page.getPageManager());
        context.registerService(BindingsValuesProvider.class,
            bindings -> bindings.put(WCMBindingsConstants.NAME_PAGE_MANAGER, page.getPageManager()));

        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);
    }

    private void setUpTestResource(final String resourcePath) {
        context.currentResource(resourcePath);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.currentResource());
    }

    @Test
    public void testGetLinkForProduct() {
        final String expResult = "/content/product-page.html/beaumont-summit-kit.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeProduct");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }

    @Test
    public void testGetLinkForEmptyProduct() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeProductEmpty");
        button = context.request().adaptTo(Button.class);

        assertEquals("#", button.getLink());
    }

    @Test
    public void testGetLinkForCategory() {
        final String expResult = "/content/category-page.html/equipment.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeCategory");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }

    @Test
    public void testGetLinkForEmptyCategory() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeCategoryEmpty");
        button = context.request().adaptTo(Button.class);

        assertEquals("#", button.getLink());
    }

    @Test
    public void testGetLinkForExternalLink() {
        final String expResult = "http://sample-link.com";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeExternalLink");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }

    @Test
    public void testGetLinkForEmptyExternalLink() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeExternalLinkEmpty");
        button = context.request().adaptTo(Button.class);

        assertEquals("#", button.getLink());
    }

    @Test
    public void testGetLinkForLinkTo() {
        final String expResult = "/content/venia/language-masters/en.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeToPage");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }

    @Test
    public void testGetLinkForEmptyLinkTo() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeToPageEmpty");
        button = context.request().adaptTo(Button.class);

        assertEquals("#", button.getLink());
    }

    @Test
    public void testDefaultLink() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2DefaultUrl");
        button = context.request().adaptTo(Button.class);

        assertEquals("#", button.getLink());
    }

    @Test
    public void testInvalidLinkType() {
        final String expResult = "#";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2InvalidLinkType");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }

    @Test
    public void testGetLinkForCategoryWhenSelectionIdIsUrlPath() {
        final String expResult = "/content/category-page.html/equipment.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/button2TypeCategoryWithSelectionIdUrlPath");
        button = context.request().adaptTo(Button.class);

        assertEquals(expResult, button.getLink());
    }
}
