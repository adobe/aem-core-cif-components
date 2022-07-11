/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.magento.graphql.DownloadableProduct;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.adobe.cq.commerce.magento.graphql.VirtualProduct;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.day.cq.wcm.commons.policy.ContentPolicyStyle;
import com.drew.lang.annotations.NotNull;
import com.drew.lang.annotations.Nullable;

public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns the {@link Style}/{@link ContentPolicy} of a given content {@link Resource} as ValueMap. It tries to get the
     * {@link ContentPolicy} first and if it fails, it tries to get the {@link Style}.
     *
     * @param request
     * @param contentResource
     * @return
     */
    @NotNull
    public static ValueMap getStyleProperties(@Nullable SlingHttpServletRequest request, Resource contentResource) {
        ContentPolicyManager contentPolicyManager = contentResource.getResourceResolver().adaptTo(ContentPolicyManager.class);
        if (contentPolicyManager != null) {
            ContentPolicy policy = contentPolicyManager.getPolicy(contentResource, request);
            if (policy != null) {
                return new ContentPolicyStyle(policy, null);
            }
        }

        Designer designer = contentResource.getResourceResolver().adaptTo(Designer.class);
        if (designer != null) {
            Style style = designer.getStyle(contentResource);
            if (style != null) {
                return style;
            }
        }

        return ValueMap.EMPTY;
    }

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

    /**
     * Returns true if add to cart is possible for the the given product.
     *
     * @param product a ProductInterface instance
     *
     * @return {@code true} if the product can be added to the shopping cart, {@code false} otherwise
     */
    public static boolean isShoppableProduct(ProductInterface product) {
        return product instanceof SimpleProduct ||
            product instanceof VirtualProduct ||
            product instanceof DownloadableProduct;
    }

    /**
     * Returns the string parameter representing a link target or {@code null} if the parameter equals {@code _self}.
     * Since {@code _self} is the default value of the {@code linkTarget} property in the edit dialogs we use this method
     * to omit the target attribute on links when the value is {@code _self}.
     *
     * @param linkTarget a link target value
     * @return {@code linkTarget} or {@code null} if {@code linkTarget} equals {@code _self}
     */
    public static String normalizeLinkTarget(String linkTarget) {
        return "_self".equals(linkTarget) ? null : linkTarget;
    }
}
