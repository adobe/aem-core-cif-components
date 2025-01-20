/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.io.IOException;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.servlethelpers.MockRequestPathInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.services.ComponentsConfiguration;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.buildAemContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductImplAssetsTest {

    private static final ValueMap MOCK_CONFIGURATION = new ValueMapDecorator(ImmutableMap.of("cq:graphqlClient", "default", "magentoStore",
        "my-store", "enableUIDSupport", "true"));
    private static final ComponentsConfiguration MOCK_CONFIGURATION_OBJECT = new ComponentsConfiguration(MOCK_CONFIGURATION);
    private static final String PAGE = "/content/pageA";
    private static final String PRODUCT = "/content/pageA/jcr:content/root/responsivegrid/product";

    @Rule
    public final AemContext context = buildAemContext("/context/jcr-content.json")
        .<AemContext>afterSetUp(context -> {
            context.registerAdapter(Resource.class, ComponentsConfiguration.class,
                (Function<Resource, ComponentsConfiguration>) input -> !input.getPath().contains("pageB") ? MOCK_CONFIGURATION_OBJECT
                    : ComponentsConfiguration.EMPTY);
        })
        .build();

    private ProductImpl productModel;

    @Test
    public void testProductNoAsset() throws Exception {
        setUp("graphql/magento-graphql-product-no-asset-result.json");

        productModel = context.request().adaptTo(ProductImpl.class);

        Assert.assertNotNull(productModel);
        Assert.assertNotNull(productModel.getAssets());

        // returns empty list for no asset
        Assert.assertTrue(productModel.getAssets().isEmpty());
    }

    @Test
    public void testProductIncompleteAssets() throws Exception {
        setUp("graphql/magento-graphql-product-incomplete-asset-result.json");

        productModel = context.request().adaptTo(ProductImpl.class);

        Assert.assertNotNull(productModel);
        List<Asset> assets = productModel.getAssets();
        Assert.assertNotNull(assets);
        Assert.assertEquals(3, assets.size());

        Assert.assertEquals("a1", assets.get(0).getLabel());
        Assert.assertEquals("a2", assets.get(1).getLabel());

        // order asset without position to the end
        Assert.assertEquals("a4", assets.get(2).getLabel());
    }

    private void setUp(String graphqlResponse) throws IOException {
        Page page = context.currentPage(PAGE);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));

        context.currentResource(PRODUCT);
        Resource productResource = Mockito.spy(context.resourceResolver().getResource(PRODUCT));

        GraphqlClient graphqlClient = new GraphqlClientImpl();
        Utils.registerGraphqlClient(context, graphqlClient, null);

        Utils.setupHttpResponse(graphqlResponse, httpClient, 200, "{products(filter:{url_key");
        Utils.setupHttpResponse(graphqlResponse, httpClient, 200, "{products(filter:{sku");

        context.registerAdapter(Resource.class, GraphqlClient.class, (Function<Resource, GraphqlClient>) input -> input.getValueMap().get(
            "cq:graphqlClient", String.class) != null ? graphqlClient : null);

        MockRequestPathInfo requestPathInfo = (MockRequestPathInfo) context.request().getRequestPathInfo();
        requestPathInfo.setSuffix("/no-asset.html");

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(productResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, productResource.getValueMap());

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyBoolean())).then(i -> i.getArgumentAt(1, Boolean.class));
        slingBindings.put("currentStyle", style);

        SightlyWCMMode wcmMode = mock(SightlyWCMMode.class);
        when(wcmMode.isDisabled()).thenReturn(false);
        slingBindings.put("wcmmode", wcmMode);
    }
}
