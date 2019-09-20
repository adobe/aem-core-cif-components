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

package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * Builds a NumberFormat instance used for formatting prices based on the given
     * locale and currency code. If the given currency code is not valid in respect to
     * ISO 4217, the default currency for the given locale is used.
     *
     * @param locale Price locale
     * @param currencyCode Additional currency code
     * @return Price formatter
     */
    public static NumberFormat buildPriceFormatter(Locale locale, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        if (currencyCode == null) {
            return formatter;
        }

        // Try to overwrite with the given currencyCode, otherwise keep using default for locale
        try {
            Currency currency = Currency.getInstance(currencyCode);
            formatter.setCurrency(currency);
        } catch (Exception err) {
            LOGGER.debug("Could not use given currency, fall back to currency from page locale");
        }

        return formatter;
    }
}
