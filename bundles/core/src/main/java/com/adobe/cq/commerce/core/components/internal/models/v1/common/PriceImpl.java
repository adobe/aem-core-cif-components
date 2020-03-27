/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.magento.graphql.PriceRange;

public class PriceImpl implements Price {

    private NumberFormat priceFormatter;

    private Locale locale;

    private String currency;

    private Double regularPriceMin;
    private Double regularPriceMax;

    private Double finalPriceMin;
    private Double finalPriceMax;

    private Double discountAmountMin;
    private Double discountAmountMax;

    private Double discountPercentMin;
    private Double discountPercentMax;

    private Boolean isDiscounted;
    private Boolean isRange;
    private boolean isStartPrice;

    public PriceImpl(PriceRange range, Locale locale) {
        this(range, locale, false);
    }

    public PriceImpl(PriceRange range, Locale locale, boolean isStartPrice) {
        this.locale = locale;
        this.isStartPrice = isStartPrice;
        this.currency = range.getMinimumPrice().getFinalPrice().getCurrency().toString();

        this.regularPriceMin = range.getMinimumPrice().getRegularPrice().getValue();
        this.finalPriceMin = range.getMinimumPrice().getFinalPrice().getValue();
        this.discountAmountMin = range.getMinimumPrice().getDiscount().getAmountOff();
        this.discountPercentMin = range.getMinimumPrice().getDiscount().getPercentOff();

        if (range.getMaximumPrice() != null) {
            this.regularPriceMax = range.getMaximumPrice().getRegularPrice().getValue();
            this.finalPriceMax = range.getMaximumPrice().getFinalPrice().getValue();
            this.discountAmountMax = range.getMaximumPrice().getDiscount().getAmountOff();
            this.discountPercentMax = range.getMaximumPrice().getDiscount().getPercentOff();
        }
    }

    private NumberFormat getPriceFormatter() {
        if (priceFormatter == null) {
            priceFormatter = Utils.buildPriceFormatter(locale, currency);
        }
        return priceFormatter;
    }

    @Override
    public Boolean isRange() {
        if (isRange == null) {
            isRange = finalPriceMin != null && finalPriceMax != null && new BigDecimal(finalPriceMin).compareTo(new BigDecimal(
                finalPriceMax)) != 0;
        }
        return isRange;
    }

    @Override
    public Boolean isDiscounted() {
        if (isDiscounted == null) {
            // discountAmountMin > 0
            isDiscounted = discountAmountMin != null && BigDecimal.ZERO.compareTo(new BigDecimal(discountAmountMin)) < 0;
        }

        return isDiscounted;
    }

    @Override
    public boolean isStartPrice() {
        return isStartPrice;
    }

    @Override
    public String getCurrency() {
        return currency;
    }

    @Override
    public Double getRegularPrice() {
        return regularPriceMin;
    }

    @Override
    public String getFormattedRegularPrice() {
        return getPriceFormatter().format(regularPriceMin);
    }

    @Override
    public Double getFinalPrice() {
        return finalPriceMin;
    }

    @Override
    public String getFormattedFinalPrice() {
        return getPriceFormatter().format(finalPriceMin);
    }

    @Override
    public Double getDiscountAmount() {
        return discountAmountMin;
    }

    @Override
    public String getFormattedDiscountAmount() {
        return getPriceFormatter().format(discountAmountMin);
    }

    @Override
    public Double getDiscountPercent() {
        return discountPercentMin;
    }

    @Override
    public Double getRegularPriceMax() {
        return isRange() ? regularPriceMax : Double.NaN;
    }

    @Override
    public String getFormattedRegularPriceMax() {
        return isRange() ? getPriceFormatter().format(regularPriceMax) : "";
    }

    @Override
    public Double getFinalPriceMax() {
        return isRange() ? finalPriceMax : Double.NaN;
    }

    @Override
    public String getFormattedFinalPriceMax() {
        return isRange() ? getPriceFormatter().format(finalPriceMax) : "";
    }

    @Override
    public Double getDiscountAmountMax() {
        return isRange() ? discountAmountMax : Double.NaN;
    }

    @Override
    public String getFormattedDiscountAmountMax() {
        return isRange() ? getPriceFormatter().format(discountAmountMax) : "";
    }

    @Override
    public Double getDiscountPercentMax() {
        return isRange() ? discountPercentMax : Double.NaN;
    }

}
