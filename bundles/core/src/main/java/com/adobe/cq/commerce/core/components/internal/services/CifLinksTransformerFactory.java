/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.rewriter.DefaultTransformer;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * Sling rewriter for transforming links tagged with (@code data-category-uid} or {@code data-product-sku} to
 * PDP or PLP links according to the UrlProvider configuration.
 */
@Component(
    immediate = true,
    service = TransformerFactory.class,
    property = {
        "pipeline.type=ciflinks"
    })
public class CifLinksTransformerFactory implements TransformerFactory {
    static final String ATTR_CATEGORY_UID = "data-category-uid";
    static final String ATTR_PRODUCT_SKU = "data-product-sku";
    public static final String ATTR_HREF = "href";
    public static final String ELEMENT_ANCHOR = "a";

    @Reference
    UrlProvider urlProvider;

    @Override
    public Transformer createTransformer() {
        return new CifLinksTransformer();
    }

    class CifLinksTransformer extends DefaultTransformer {
        private SlingHttpServletRequest request;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            this.request = context.getRequest();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (ELEMENT_ANCHOR.equals(localName)) {
                String categoryUid = attributes.getValue(ATTR_CATEGORY_UID);
                String newHref = null;
                if (StringUtils.isNotBlank(categoryUid)) {
                    Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                    Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
                    newHref = urlProvider.toCategoryUrl(request, categoryPage, categoryUid);
                }
                String productSku = attributes.getValue(ATTR_PRODUCT_SKU);
                if (StringUtils.isNotBlank(productSku)) {
                    Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                    Page productPage = SiteNavigation.getProductPage(currentPage);
                    newHref = urlProvider.toProductUrl(request, productPage, productSku);
                }

                if (newHref != null) {
                    String href = attributes.getValue(ATTR_HREF);
                    AttributesImpl attributesImpl;
                    if (StringUtils.isNotBlank(href)) {
                        attributesImpl = new AttributesImpl(attributes);
                        attributesImpl.setValue(attributes.getIndex(ATTR_HREF), newHref);
                    } else {
                        attributesImpl = new AttributesImpl(attributes);
                        attributesImpl.addAttribute("", ATTR_HREF, ATTR_HREF, "CDATA", newHref);
                    }
                    super.startElement(uri, localName, qName, attributesImpl);
                } else {
                    super.startElement(uri, localName, qName, attributes);
                }
            } else {
                super.startElement(uri, localName, qName, attributes);
            }
        }
    }
}
