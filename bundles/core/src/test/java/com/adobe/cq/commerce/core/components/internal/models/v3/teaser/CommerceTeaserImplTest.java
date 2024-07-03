/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v3.teaser;

import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.internal.services.SpecificPageStrategy;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaserActionItem;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.MockPathProcessor;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.services.link.PathProcessor;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.components.ComponentManager;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CommerceTeaserImplTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    private static final String PRODUCT_PAGE = "/content/product-page";
    private static final String PRODUCT_SPECIFIC_PAGE = PRODUCT_PAGE + "/product-specific-page";
    private static final String CATEGORY_PAGE = "/content/category-page";
    private static final String PAGE = "/content/pageA";
    private static final String TEASER = "/content/pageA/jcr:content/root/responsivegrid/commerceteaser3";
    private static final String TEASER_WITH_URL_PATH_FOR_CATEGORY = "/content/pageA/jcr:content/root/responsivegrid/commerceteaserwithcategoryselectionasurlpath";

    private Resource commerceTeaserResource;
    private CommerceTeaser commerceTeaser;

    @Before
    public void setup() throws Exception {
        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        GraphqlClient graphqlClient = spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient);

        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:");
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);
    }

    private void setupTest(String componentPath) throws Exception {
        // Mock resource and resolver
        commerceTeaserResource = spy(context.resourceResolver().getResource(componentPath));
        ResourceResolver resolver = spy(commerceTeaserResource.getResourceResolver());
        when(commerceTeaserResource.getResourceResolver()).thenReturn(resolver);
        when(commerceTeaserResource.getResourceSuperType()).thenReturn("core/wcm/components/teaser/v1/teaser");
        when(commerceTeaserResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        Page page = spy(context.currentPage(PAGE));
        context.currentResource(componentPath);
        context.currentResource(commerceTeaserResource);
        context.registerService(PathProcessor.class, new MockPathProcessor());
        Resource pageResource = spy(page.adaptTo(Resource.class));
        when(page.adaptTo(Resource.class)).thenReturn(pageResource);
        when(pageResource.adaptTo(ComponentsConfiguration.class)).thenReturn(MOCK_CONFIGURATION_OBJECT);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(commerceTeaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PAGE_MANAGER, page.getPageManager());
        ComponentManager componentManager = commerceTeaserResource.getResourceResolver().adaptTo(ComponentManager.class);
        slingBindings.put(WCMBindingsConstants.NAME_COMPONENT, componentManager.getComponent(componentPath));
        Style style = mock(Style.class);
        when(style.get(anyString(), anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);

        // TODO: CIF-2469
        // With a newer version of OSGI mock we could re-inject the reference into the existing UrlProviderImpl
        // context.registerInjectActivateService(new SpecificPageStrategy(), "generateSpecificPageUrls", true);
        SpecificPageStrategy specificPageStrategy = context.getService(SpecificPageStrategy.class);
        Whitebox.setInternalState(specificPageStrategy, "generateSpecificPageUrls", true);
    }

    @Test
    public void verifyProperties() throws Exception {
        setupTest(TEASER);
        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);
        Assert.assertNotNull("The CommerceTeaser object is null", commerceTeaser);

        Assert.assertEquals("The pre-title is correct", "Pretitle", commerceTeaser.getPretitle());
        Assert.assertEquals("The title is correct", "Title", commerceTeaser.getTitle());
        Assert.assertEquals("The description is correct", "Description", commerceTeaser.getDescription());
        Assert.assertEquals("The id is correct", "id", commerceTeaser.getId());
        Assert.assertNull("The link URL is null", commerceTeaser.getLinkURL());
        Assert.assertNull(commerceTeaser.getData());
        Assert.assertEquals("The exported resource type is correct", "core/cif/components/content/teaser/v3/teaser",
            commerceTeaser.getExportedType());
    }

    @Test
    public void verifyActions() throws Exception {
        setupTest(TEASER);
        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);
        List<ListItem> actionItems = commerceTeaser.getActions();

        Assert.assertTrue(commerceTeaser.isActionsEnabled());
        Assert.assertTrue(actionItems.size() == 5);

        // Product slug is configured and there is a dedicated specific subpage for that product
        Assert.assertEquals(PRODUCT_SPECIFIC_PAGE + ".html/beaumont-summit-kit.html", actionItems.get(0).getLink().getURL());
        Assert.assertEquals("A product", actionItems.get(0).getTitle());
        Assert.assertEquals("MJ01", ((CommerceTeaserActionItem) actionItems
            .get(0)).getEntityIdentifier().getValue());

        // Category id is configured
        Assert.assertEquals(CATEGORY_PAGE + ".html/equipment.html", actionItems.get(1).getLink().getURL());
        Assert.assertEquals("A category", actionItems.get(1).getTitle());
        Assert.assertEquals("uid-5",
            ((CommerceTeaserActionItem) actionItems.get(1)).getEntityIdentifier().getValue());

        // Both are configured, category links "wins"
        Assert.assertEquals(CATEGORY_PAGE + ".html/equipment.html", actionItems.get(2).getLink().getURL());
        Assert.assertEquals("A category", actionItems.get(2).getTitle());
        Assert.assertEquals("uid-5", ((CommerceTeaserActionItem) actionItems.get(2))
            .getEntityIdentifier().getValue());

        // Some text is entered, current page is used
        Assert.assertEquals(PAGE + ".html", actionItems.get(3).getLink().getURL());
        Assert.assertEquals("Some text", actionItems.get(3).getTitle());

        // Link is configured
        Assert.assertEquals("/content/page", actionItems.get(4).getLink().getURL());
        Assert.assertEquals("A page", actionItems.get(4).getTitle());
    }

    @Test
    public void verifyNoActionsConfigured() throws Exception {
        setupTest(TEASER);
        context.resourceResolver().delete(commerceTeaserResource.getChild("actions"));
        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);

        List<ListItem> actionItems = commerceTeaser.getActions();
        Assert.assertTrue(actionItems.isEmpty());
    }

    @Test
    public void verifyJsonExport() throws Exception {
        setupTest(TEASER);
        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);
        Utils.testJSONExport(commerceTeaser, "/exporter/commerce-teaser-v3.json");
    }

    @Test
    public void verifyActionsWhenCategoryIdentifierSetAsUrlPath() throws Exception {
        setupTest(TEASER_WITH_URL_PATH_FOR_CATEGORY);
        commerceTeaser = context.request().adaptTo(CommerceTeaserImpl.class);
        List<ListItem> actionItems = commerceTeaser.getActions();

        Assert.assertTrue(commerceTeaser.isActionsEnabled());
        Assert.assertTrue(actionItems.size() == 5);

        // Product slug is configured and there is a dedicated specific subpage for that product
        Assert.assertEquals(PRODUCT_SPECIFIC_PAGE + ".html/beaumont-summit-kit.html", actionItems.get(0).getURL());
        Assert.assertEquals("A product", actionItems.get(0).getTitle());
        Assert.assertEquals("MJ01", ((CommerceTeaserActionItem) actionItems
            .get(0)).getEntityIdentifier().getValue());

        // Category id is configured
        Assert.assertEquals(CATEGORY_PAGE + ".html/equipment.html", actionItems.get(1).getURL());
        Assert.assertEquals("A category", actionItems.get(1).getTitle());
        Assert.assertEquals("equipment",
            ((CommerceTeaserActionItem) actionItems.get(1)).getEntityIdentifier().getValue());

        // Both are configured, category links "wins"
        Assert.assertEquals(CATEGORY_PAGE + ".html/equipment.html", actionItems.get(2).getURL());
        Assert.assertEquals("A category", actionItems.get(2).getTitle());
        Assert.assertEquals("equipment", ((CommerceTeaserActionItem) actionItems.get(2))
            .getEntityIdentifier().getValue());

        // Some text is entered, current page is used
        Assert.assertEquals(PAGE + ".html", actionItems.get(3).getURL());
        Assert.assertEquals("Some text", actionItems.get(3).getTitle());

        // Link is configured
        Assert.assertEquals("/content/page", actionItems.get(4).getURL());
        Assert.assertEquals("A page", actionItems.get(4).getTitle());
    }
}
