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

package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.Locale;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.wcm.api.Page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductToProductListItemConverterTest {

    @Mock
    private Page productPage;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private Resource resource;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProductInterface productInterface;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PriceRange priceRange;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Money money;

    @InjectMocks
    private ProductToProductListItemConverter converterUnderTest;

    private static final String URL = "http://store.com";
    private static final String IMAGE_URL = "http://image.com";
    private static final String PRODUCT_NAME = "name";
    private static final Locale PAGE_LOCALE = Locale.GERMANY;

    @Before
    public void setUp() {
        when(productInterface.getPriceRange()).thenReturn(priceRange);
        when(productInterface.getUrlKey()).thenReturn(URL);
        when(productInterface.getName()).thenReturn(PRODUCT_NAME);
        when(productInterface.getSmallImage().getUrl()).thenReturn(IMAGE_URL);

        when(money.getCurrency()).thenReturn(CurrencyEnum.BDT);

        when(priceRange.getMinimumPrice().getFinalPrice()).thenReturn(money);
        when(priceRange.getMinimumPrice().getRegularPrice()).thenReturn(money);
        when(priceRange.getMinimumPrice().getFinalPrice()).thenReturn(money);

        when(productPage.getLanguage(false)).thenReturn(PAGE_LOCALE);
        when(productPage.getContentResource()).thenReturn(resource);

        when(resource.getResourceType()).thenReturn("commerce/mock");
        when(resource.getPath()).thenReturn("/mock/resource/path");

    }

    @Test
    public void testProductInterfaceConverted() {
        final ProductListItem result = converterUnderTest.apply(productInterface);

        verify(productPage).getLanguage(false);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ProductListItem.class);
    }

}
