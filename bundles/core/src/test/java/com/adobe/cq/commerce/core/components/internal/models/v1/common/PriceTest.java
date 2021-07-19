/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PriceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PriceRange priceRange;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Money money;

    @Before
    public void setUp() {
        when(money.getCurrency()).thenReturn(CurrencyEnum.USD);

        when(priceRange.getMinimumPrice().getFinalPrice()).thenReturn(money);
        when(priceRange.getMinimumPrice().getRegularPrice()).thenReturn(money);

    }

    @Test
    public void testNotEmptyPrices() {
        when(money.getValue()).thenReturn(12.34);
        when(priceRange.getMinimumPrice().getDiscount().getAmountOff()).thenReturn(23.45);

        Price price = new PriceImpl(priceRange, Locale.US, false);

        Assert.assertFalse(price.isEmpty());

        Assert.assertEquals("$12.34", price.getFormattedRegularPrice());
        Assert.assertEquals("$12.34", price.getFormattedFinalPrice());
        Assert.assertEquals("$23.45", price.getFormattedDiscountAmount());
    }

    @Test
    public void testEmptyPrices() {
        when(money.getValue()).thenReturn(null);
        when(priceRange.getMinimumPrice().getDiscount().getAmountOff()).thenReturn(null);

        Price price = new PriceImpl(priceRange, Locale.US, false);

        Assert.assertTrue(price.isEmpty());

        Assert.assertEquals("", price.getFormattedRegularPrice());
        Assert.assertEquals("", price.getFormattedFinalPrice());
        Assert.assertEquals("", price.getFormattedDiscountAmount());
    }
}
