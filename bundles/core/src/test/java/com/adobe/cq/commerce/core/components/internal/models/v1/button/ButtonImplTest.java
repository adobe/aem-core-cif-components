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

package com.adobe.cq.commerce.core.components.internal.models.v1.button;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.services.MockUrlProviderConfiguration;
import com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.wcm.core.components.models.Button;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class ButtonImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext(
            (AemContextCallback) context -> {
                // Load page structure
                context.load().json(contentPath, "/content");

                UrlProviderImpl urlProvider = new UrlProviderImpl();
                urlProvider.activate(new MockUrlProviderConfiguration());
                context.registerService(UrlProvider.class, urlProvider);
            },
            ResourceResolverType.JCR_MOCK);
    }

    private static final String PAGE = "/content/pageA";
    private Button button;

    @Before
    public void setUp() throws Exception {
        GraphqlClient graphqlClient = Utils.setupGraphqlClientWithHttpResponseFrom("graphql/magento-graphql-category-list-result.json");
        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        Page page = spy(context.currentPage(PAGE));

        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        context.addModelsForClasses(ButtonImpl.class);
    }

    private void setUpTestResource(final String resourcePath) {
        context.currentResource(resourcePath);
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(context.currentResource());
    }

    @Test
    public void testGetLinkForProduct() {
        final String expResult = "/content/product-page.html/blast-mini-pump.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeProduct");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForEmptyProduct() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeProductEmpty");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals("#", result);
    }

    @Test
    public void testGetLinkForCategory() {
        final String expResult = "/content/category-page.html/equipment.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeCategory");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForEmptyCategory() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeCategoryEmpty");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals("#", result);
    }

    @Test
    public void testGetLinkForExternalLink() {
        final String expResult = "http://sample-link.com";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeExternalLink");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForEmptyExternalLink() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeExternalLinkEmpty");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals("#", result);
    }

    @Test
    public void testGetLinkForLinkTo() {
        final String expResult = "/content/venia/language-masters/en.html";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeToPage");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLinkForEmptyLinkTo() {
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonTypeToPageEmpty");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals("#", result);
    }

    @Test
    public void testDefaultLink() {
        final String expResult = "#";
        setUpTestResource("/content/pageA/jcr:content/root/responsivegrid/buttonDefaultUrl");
        button = context.request().adaptTo(Button.class);

        String result = button.getLink();
        assertEquals(expResult, result);
    }
}
