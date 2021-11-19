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
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductCarouselImplEmptyTest {

    private static final String PAGE = "/content/pageA";
    private static final String PRODUCTCAROUSEL = "/content/pageA/jcr:content/root/responsivegrid/productcarousel";

    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");

    private Resource carouselResource;
    private ProductCarouselImpl productCarousel;
    private SlingBindings slingBindings;

    @Before
    public void setUp() {
        carouselResource = Mockito.spy(context.resourceResolver().getResource(PRODUCTCAROUSEL));
        // GraphQL client is not available
        Mockito.when(carouselResource.adaptTo(GraphqlClient.class)).thenReturn(null);
        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(carouselResource);
        Page page = context.currentPage(PAGE);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        Style style = mock(Style.class);
        when(style.get(Mockito.anyString(), Mockito.anyInt())).then(i -> i.getArgumentAt(1, Object.class));
        slingBindings.put("currentStyle", style);
    }

    @Test
    public void getProductsEmptyNoConfig() {
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertEquals(0, items.size());
        Assert.assertNull(productCarousel.getProductsRetriever());
    }

    @Test
    public void getEmptyProductIdentifiers() {
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);
        List<ProductListItem> identifiers = productCarousel.getProductIdentifiers();

        Assert.assertEquals(0, identifiers.size());
    }

    @Test
    public void getProductsEmpty() {
        String[] productSkuArray = (String[]) carouselResource.getValueMap().get("product"); // The HTL script uses an alias here
        slingBindings.put("productSkuList", productSkuArray);
        productCarousel = context.request().adaptTo(ProductCarouselImpl.class);

        List<ProductListItem> items = productCarousel.getProducts();
        Assert.assertEquals(0, items.size());
        Assert.assertNull(productCarousel.getProductsRetriever());
    }
}
