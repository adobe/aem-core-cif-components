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

package com.adobe.cq.commerce.core.components.models.graphql;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.scripting.sightly.Record;

import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.ProductPrices;
import com.day.cq.wcm.api.Page;
import com.shopify.graphql.support.AbstractResponse;
import com.shopify.graphql.support.CustomFieldInterface;

public class GraphqlRecordFactory {
    public interface Context {
        Page getCurrentPage();

        String getMediaBaseUrl();

        String getVariantSku();
    }

    public interface Formatter {
        String getSuffix();

        Object apply(AbstractResponse record, String name);
    }

    private Context context;
    public final Map<String, Formatter> formatters = new HashMap<>();

    public GraphqlRecordFactory(Context context) {
        this.context = context;
        setFormatter(new CategoryURLFormatter());
        setFormatter(new PriceFormatter());
        setFormatter(new CategoryImageURLFormatter());
        setFormatter(new ProductURLFormatter());
    }

    public Record recordFrom(CustomFieldInterface data) {
        return new GraphqlRecord(data);
    }

    public Record recordFrom(AbstractResponse data) {
        return new GraphqlRecord(data);
    }

    public void setFormatter(Formatter formatter) {
        if (formatter != null) {
            formatters.put(formatter.getSuffix(), formatter);
        }
    }

    private class GraphqlRecord implements Record {
        protected final AbstractResponse data;

        private GraphqlRecord(CustomFieldInterface data) {
            this(new AbstractResponse() {
                @Override
                public Object get(String field) {
                    return data.get(field);
                }

                @Override
                public boolean unwrapsToObject(String s) {
                    return false;
                }
            });
        }

        private GraphqlRecord(AbstractResponse data) {
            this.data = data;
        }

        @Override
        public Object getProperty(String name) {
            final int colon = name.indexOf(':');
            if (colon > 0 && colon < name.length()) {
                String suffix = name.substring(colon + 1);
                Formatter formatter = formatters.get(suffix);
                if (formatter != null) {
                    Object value = formatter.apply(data, name.substring(0, colon));
                    if (value != null) {
                        return value;
                    }
                }
            }

            final Object o = data.get(name);
            if (o instanceof AbstractResponse) {
                return recordFrom((AbstractResponse) o);
            } else if (o instanceof Collection) {
                final Object[] a = ((Collection) o).toArray();
                for (int i = 0; i < a.length; i++) {
                    if (a[i] instanceof AbstractResponse) {
                        a[i] = recordFrom((AbstractResponse) a[i]);
                    } else if (a[i] instanceof CustomFieldInterface) {
                        a[i] = recordFrom((CustomFieldInterface) a[i]);
                    }
                }
                return a;
            }
            return o;
        }

        @Override
        public Set<String> getPropertyNames() {
            return Collections.emptySet();
        }
    }

    public class PriceFormatter implements Formatter {

        @Override
        public String getSuffix() {
            return "price";
        }

        @Override
        public Object apply(AbstractResponse record, String name) {

            final Object o = record.get(name);
            if (o instanceof ProductPrices) {
                ProductPrices p = (ProductPrices) o;
                Double price = p.getRegularPrice().getAmount().getValue();
                String currency = p.getRegularPrice().getAmount().getCurrency().toString();
                NumberFormat priceFormatter = Utils.buildPriceFormatter(context.getCurrentPage().getLanguage(false), currency);
                return priceFormatter.format(price);
            }

            return null;

        }
    }

    class CategoryURLFormatter implements Formatter {

        @Override
        public String getSuffix() {
            return "categoryUrl";
        }

        @Override
        public Object apply(AbstractResponse record, String name) {

            final Object o = record.get(name);
            Page categoryPage = SiteNavigation.getCategoryPage(context.getCurrentPage());
            if (categoryPage == null) {
                categoryPage = context.getCurrentPage();
            }
            return String.format("%s.%s.html", categoryPage.getPath(), o);

        }
    }

    class CategoryImageURLFormatter implements Formatter {
        private static final String CATEGORY_IMAGE_FOLDER = "catalog/category/";

        @Override
        public String getSuffix() {
            return "categoryImageUrl";
        }

        @Override
        public Object apply(AbstractResponse record, String name) {
            String image = (String) record.get(name);
            String mediaBaseUrl = context.getMediaBaseUrl();
            if (StringUtils.isNotBlank(mediaBaseUrl) && StringUtils.isNotBlank(image)) {
                return mediaBaseUrl + CATEGORY_IMAGE_FOLDER + image;
            }
            return null;
        }
    }

    class ProductURLFormatter implements Formatter {

        @Override
        public String getSuffix() {
            return "productUrl";
        }

        @Override
        public Object apply(AbstractResponse record, String name) {
            final Object o = record.get(name);
            Page productPage = SiteNavigation.getProductPage(context.getCurrentPage());
            if (productPage == null) {
                productPage = context.getCurrentPage();
            }
            return SiteNavigation.toProductUrl(productPage.getPath(), String.valueOf(o), context.getVariantSku());

        }
    }
}
