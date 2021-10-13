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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * Sling rewriter for transforming commerce links to PDP or PLP links according to the UrlProvider configuration.
 *
 * A commerce link has a {@code href} with the value {@code #CommerceLinks} and is tagged with a commerce attribute
 * {@code data-category-uid} or {@code data-product-sku}. The commerce attribute is preserved on the link where the
 * {@code href} was successfully transformed and it's removed where the transformation was not performed. If a link
 * has both commerce attributes then {@code data-product-sku} is honored and {@code data-category-uid} is removed.
 */
@Component(
    immediate = true,
    service = TransformerFactory.class,
    property = {
        "pipeline.mode=global",
        "pipeline.type=commercelinks",
        "service.ranking=-500"
    })
@Designate(ocd = CommerceLinksTransformerFactory.Configuration.class)
public class CommerceLinksTransformerFactory implements TransformerFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceLinksTransformerFactory.class);

    @ObjectClassDefinition(name = "Adobe CQ Commerce Links Transformer")
    @interface Configuration {
        @AttributeDefinition(
            name = "Enabled",
            description = "If enabled, links edited with the Commerce Links RTE plugin are transformed to real links.")
        boolean isEnabled() default true;
    }

    static final String MARKER_COMMERCE_LINKS = "#CommerceLinks";
    static final String ATTR_CATEGORY_UID = "data-category-uid";
    static final String ATTR_PRODUCT_SKU = "data-product-sku";
    static final String ATTR_HREF = "href";
    static final String ELEMENT_ANCHOR = "a";

    @Reference
    private UrlProvider urlProvider;
    private boolean enabled;

    @Activate
    @Modified
    protected void activate(Configuration config) {
        enabled = config.isEnabled();
        if (enabled) {
            LOGGER.info("Commerce links transformer enabled.");
        } else {
            LOGGER.info("Commerce links transformer disabled.");
        }
    }

    @Override
    public Transformer createTransformer() {
        return enabled ? new CommerceLinksTransformer() : new DefaultTransformer();
    }

    class CommerceLinksTransformer extends DefaultTransformer {
        private SlingHttpServletRequest request;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            this.request = context.getRequest();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!ELEMENT_ANCHOR.equals(localName)) {
                super.startElement(uri, localName, qName, attributes);
                return;
            }

            String href = StringUtils.trim(attributes.getValue(ATTR_HREF));
            if (!MARKER_COMMERCE_LINKS.equals(href)) {
                // remove commerce attributes if any
                AttributesImpl attributesImpl = new AttributesImpl(attributes);
                int index = attributesImpl.getIndex(ATTR_CATEGORY_UID);
                if (index > -1) {
                    attributesImpl.removeAttribute(index);
                    attributes = attributesImpl;
                }
                index = attributesImpl.getIndex(ATTR_PRODUCT_SKU);
                if (index > -1) {
                    attributesImpl.removeAttribute(index);
                    attributes = attributesImpl;
                }
                super.startElement(uri, localName, qName, attributes);
                return;
            }

            String productSku = attributes.getValue(ATTR_PRODUCT_SKU);
            AttributesImpl attributesImpl = new AttributesImpl(attributes);
            if (StringUtils.isNotBlank(productSku)) {
                // if there is both product and category attribute on a link then product attribute is honored
                Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                Page productPage = SiteNavigation.getProductPage(currentPage);
                String newHref = urlProvider.toProductUrl(request, productPage, productSku);
                attributesImpl.setValue(attributes.getIndex(ATTR_HREF), newHref);
                // remove category attribute if exists (on a malformed URL)
                int index = attributesImpl.getIndex(ATTR_CATEGORY_UID);
                if (index > -1) {
                    attributesImpl.removeAttribute(index);
                }
            } else {
                String categoryUid = attributes.getValue(ATTR_CATEGORY_UID);
                if (StringUtils.isNotBlank(categoryUid)) {
                    Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                    Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
                    String newHref = urlProvider.toCategoryUrl(request, categoryPage, categoryUid);
                    attributesImpl.setValue(attributes.getIndex(ATTR_HREF), newHref);
                }
            }

            super.startElement(uri, localName, qName, attributesImpl);
        }
    }
}
