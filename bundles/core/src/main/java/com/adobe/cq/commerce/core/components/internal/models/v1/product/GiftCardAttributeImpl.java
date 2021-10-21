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
package com.adobe.cq.commerce.core.components.internal.models.v1.product;

import java.util.List;
import java.util.Map;

import com.adobe.cq.commerce.core.components.models.product.GiftCardAttribute;
import com.adobe.cq.commerce.core.components.models.product.GiftCardOption;
import com.adobe.cq.commerce.magento.graphql.GiftCardTypeEnum;

public class GiftCardAttributeImpl implements GiftCardAttribute {

    private GiftCardTypeEnum giftCardType;

    private Boolean allowOpenAmount;

    private Double openAmountMin;

    private Double openAmountMax;

    private Map<String, GiftCardOption> giftCardOptions;

    private List<Double> giftCardAmount;

    @Override
    public List<Double> getGiftCardAmount() {
        return giftCardAmount;
    }

    public void setGiftCardAmount(List<Double> giftCardAmount) {
        this.giftCardAmount = giftCardAmount;
    }

    @Override
    public Boolean getAllowOpenAmount() {
        return allowOpenAmount;
    }

    public void setAllowOpenAmount(Boolean allowOpenAmount) {
        this.allowOpenAmount = allowOpenAmount;
    }

    @Override
    public Double getOpenAmountMin() {
        return openAmountMin;
    }

    public void setOpenAmountMin(Double openAmountMin) {
        this.openAmountMin = openAmountMin;
    }

    @Override
    public Double getOpenAmountMax() {
        return openAmountMax;
    }

    public void setOpenAmountMax(Double openAmountMax) {
        this.openAmountMax = openAmountMax;
    }

    @Override
    public Map<String, GiftCardOption> getGiftCardOptions() {
        return giftCardOptions;
    }

    public void setGiftCardOptions(Map<String, GiftCardOption> giftCardOptions) {
        this.giftCardOptions = giftCardOptions;
    }

    @Override
    public GiftCardTypeEnum getGiftCardType() {
        return giftCardType;
    }

    public void setGiftCardType(GiftCardTypeEnum giftCardType) {
        this.giftCardType = giftCardType;
    }
}