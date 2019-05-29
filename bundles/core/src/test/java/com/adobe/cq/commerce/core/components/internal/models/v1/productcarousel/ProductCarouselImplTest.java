/*
 * Copyright 2019 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel;

import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productlist.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.internal.util.reflection.Whitebox;

public class ProductCarouselImplTest {
    
    public static final String PRODUCTLIST = "/graphql/magento-graphql-productcarousel.json";
    
    private ProductCarousel slingModel;
    private List<ProductInterface> productsList;
    
    @Mock
    private String[] productSkuList;
    
    @Before
    public void setUp() throws Exception {
        
        this.slingModel = new ProductCarouselImpl();
        Whitebox.setInternalState(this.slingModel, "productSkuList",productSkuList);
        
        Page productPage = mock(Page.class);
        when(productPage.getLanguage(false)).thenReturn(Locale.US);
        when(productPage.getPath()).thenReturn("/content/test-product-page");
        Whitebox.setInternalState(this.slingModel, "productPage", productPage);
        
        String json = IOUtils.toString(this.getClass()
            .getResourceAsStream("/graphql/magento-graphql-productcarousel.json"), StandardCharsets.UTF_8);
        Query productQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        productsList  = productQuery.getProducts().getItems();
        Whitebox.setInternalState(this.slingModel, "productList", productsList);
    }

    @Test
    public void getProducts() {
        
        Collection<ProductListItem> products = this.slingModel.getProducts();
        NumberFormat priceFormatter = NumberFormat.getCurrencyInstance(Locale.US);
        
        List<ProductListItem> results = products.stream().collect(Collectors.toList());
        for (int i = 0; i < results.size(); i++) {
            
            ProductInterface productInterface = productsList.get(i);
            ProductListItem item = results.get(i);

            Assert.assertEquals(productInterface.getName(), item.getTitle());
            Assert.assertEquals(productInterface.getSku(), item.getSKU());
            Assert.assertEquals(productInterface.getUrlKey(), item.getSlug());
            Assert.assertEquals(String.format("/content/test-product-page.%s.html", productInterface.getUrlKey()),
                item.getURL());
            Assert.assertEquals(productInterface.getPrice().getRegularPrice().getAmount().getValue(),
                item.getPrice(), 0);
            Assert.assertEquals(productInterface.getPrice().getRegularPrice().getAmount().getCurrency().toString(),
                item.getCurrency());
            priceFormatter.setCurrency(Currency.getInstance(productInterface.getPrice().getRegularPrice().getAmount().getCurrency().toString()));
            Assert.assertEquals(priceFormatter.format(productInterface.getPrice().getRegularPrice().getAmount().getValue()),
                item.getFormattedPrice());
            Assert.assertTrue(StringUtils.endsWith(item.getImageURL(), productInterface.getThumbnail().getUrl()));
        }
    }
}

