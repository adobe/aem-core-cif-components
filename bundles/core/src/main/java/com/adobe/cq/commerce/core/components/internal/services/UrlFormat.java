/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public interface UrlFormat {

    Map<String, UrlFormat> DEFAULT_PRODUCTURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(ProductPageWithSku.PATTERN, ProductPageWithSku.INSTANCE);
            put(ProductPageWithUrlKey.PATTERN, ProductPageWithUrlKey.INSTANCE);
        }
    };

    Map<String, UrlFormat> DEFAULT_CATEGORYURL_FORMATS = new HashMap<String, UrlFormat>() {
        {
            put(CategoryPageWithUrlPath.PATTERN, CategoryPageWithUrlPath.INSTANCE);
        }
    };

    /**
     * Formats an URL with the given parameters.
     * 
     * @param parameters
     * @return
     */
    String format(Map<String, String> parameters);

    /**
     * Parses a givven request URI using the internal configured pattern.
     * 
     * @param requestUri
     * @return
     */
    Map<String, String> parse(String requestUri);

    abstract class AbstractUrlFormat implements UrlFormat {

        private static final String TEMPLATE_PREFIX = "{{";
        private static final String TEMPLATE_SUFFIX = "}}";

        abstract String getPattern();

        @Override
        public String format(Map<String, String> parameters) {
            StringSubstitutor sub = new StringSubstitutor(parameters, TEMPLATE_PREFIX, TEMPLATE_SUFFIX);
            String url = sub.replace(getPattern());
            url = StringUtils.substringBeforeLast(url, "#" + TEMPLATE_PREFIX); // remove anchor if it hasn't been substituted
            return url;
        }

        @Override
        public Map<String, String> parse(String requestUri) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    class ProductPageWithSku extends AbstractUrlFormat {
        public static final ProductPageWithSku INSTANCE = new ProductPageWithSku();
        public static final String PATTERN = "{{page}}.html/{{sku}}.html#{{variant_sku}}";

        @Override
        String getPattern() {
            return PATTERN;
        }
    }

    class ProductPageWithUrlKey extends AbstractUrlFormat {
        public static final ProductPageWithUrlKey INSTANCE = new ProductPageWithUrlKey();
        public static final String PATTERN = "{{page}}.html/{{url_key}}.html#{{variant_sku}}";

        @Override
        String getPattern() {
            return PATTERN;
        }
    }

    class CategoryPageWithUrlPath extends AbstractUrlFormat {
        public static final CategoryPageWithUrlPath INSTANCE = new CategoryPageWithUrlPath();
        public static final String PATTERN = "{{page}}.html/{{url_path}}.html";

        @Override
        String getPattern() {
            return PATTERN;
        }
    }

    static class StringSubstitutor {
        private final String[] searchList;
        private final String[] replacementList;

        public StringSubstitutor(Map<String, String> params, String prefix, String suffix) {
            replacementList = params.values().toArray(new String[0]);
            searchList = params.keySet().toArray(new String[0]);
            if (StringUtils.isNotBlank(prefix) && StringUtils.isNotBlank(suffix)) {
                for (int i = 0; i < searchList.length; ++i) {
                    searchList[i] = prefix + searchList[i] + suffix;
                }
            }
        }

        public String replace(String source) {
            return StringUtils.replaceEach(source, searchList, replacementList);
        }
    }
}
