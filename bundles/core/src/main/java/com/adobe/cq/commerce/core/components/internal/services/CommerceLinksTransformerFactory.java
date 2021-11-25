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
import org.jetbrains.annotations.Nullable;
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

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.UrlRewriteQuery;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

/**
 * Sling rewriter for transforming commerce links to PDP or PLP links according to the UrlProvider configuration.
 *
 * A commerce link has a {@code href} with the value {@code #CommerceLinks} and is tagged with a commerce attribute
 * {@code data-category-uid} or {@code data-product-sku}. The commerce attributes are preserved on the links.
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
    static final String ATTR_REPLACE_TEXT = "data-replace-text";
    static final String ATTR_TITLE = "title";
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
        private boolean ignoreContent;
        private int elementsDepth;
        private String linkText;

        @Override
        public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
            this.request = context.getRequest();
            ignoreContent = false;
            elementsDepth = 0;
            linkText = null;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (ignoreContent) {
                elementsDepth++;
                return;
            }

            if (!ELEMENT_ANCHOR.equals(localName)) {
                super.startElement(uri, localName, qName, attributes);
                return;
            }

            String href = StringUtils.trim(attributes.getValue(ATTR_HREF));
            if (!MARKER_COMMERCE_LINKS.equals(href)) {
                super.startElement(uri, localName, qName, attributes);
                return;
            }

            boolean replaceText = Boolean.parseBoolean(attributes.getValue(ATTR_REPLACE_TEXT));
            String newHref = null;
            String productSku = attributes.getValue(ATTR_PRODUCT_SKU);
            if (StringUtils.isNotBlank(productSku)) {
                // if there is both product and category attribute on a link then product attribute is honored
                Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                Page productPage = SiteNavigation.getProductPage(currentPage);
                if (replaceText) {
                    newHref = prepareProductInfo(productSku, productPage);
                } else {
                    newHref = urlProvider.toProductUrl(request, productPage, productSku);
                }
            } else {
                String categoryUid = attributes.getValue(ATTR_CATEGORY_UID);
                if (StringUtils.isNotBlank(categoryUid)) {
                    Page currentPage = request.getResourceResolver().adaptTo(PageManager.class).getContainingPage(request.getResource());
                    Page categoryPage = SiteNavigation.getCategoryPage(currentPage);
                    if (replaceText) {
                        newHref = prepareCategoryInfo(categoryUid, categoryPage);
                    } else {
                        newHref = urlProvider.toCategoryUrl(request, categoryPage, categoryUid);
                    }
                }
            }

            if (StringUtils.isNotBlank(newHref)) {
                AttributesImpl newAttributes = new AttributesImpl(attributes);
                newAttributes.setValue(attributes.getIndex(ATTR_HREF), newHref);
                if (StringUtils.isNotBlank(linkText)) {
                    String title = attributes.getValue(ATTR_TITLE);
                    if (title == null) {
                        // set title to linkText
                        newAttributes.addAttribute("", ATTR_TITLE, ATTR_TITLE, "CDATA", linkText);
                    }
                }
                super.startElement(uri, localName, qName, newAttributes);
                if (StringUtils.isNotBlank(linkText)) {
                    char[] chars = linkText.toCharArray();
                    // insert linkText as new content
                    super.characters(chars, 0, chars.length);
                    // ignore all content of current element
                    ignoreContent = true;
                    elementsDepth = 0;
                }
            } else {
                super.startElement(uri, localName, qName, attributes);
            }
        }

        @Override
        public void endElement(String s, String s1, String s2) throws SAXException {
            if (ignoreContent) {
                if (elementsDepth > 0) {
                    elementsDepth--;
                } else {
                    ignoreContent = false;
                }
            }

            if (!ignoreContent) {
                super.endElement(s, s1, s2);
            }
        }

        @Override
        public void characters(char[] ac, int i, int j) throws SAXException {
            if (!ignoreContent) {
                super.characters(ac, i, j);
            }
        }

        @Nullable
        private String prepareProductInfo(String productSku, Page productPage) {
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient == null) {
                LOGGER.debug("GraphQL client not found for {}", request.getResource().getPath());
                return null;
            }

            AbstractProductRetriever productRetriever = new AbstractProductRetriever(magentoGraphqlClient) {
                @Override
                protected ProductInterfaceQueryDefinition generateProductQuery() {
                    return q -> q
                        .name()
                        .urlKey()
                        .urlPath()
                        .urlRewrites(UrlRewriteQuery::url);
                }
            };
            productRetriever.setIdentifier(productSku);
            ProductInterface product = productRetriever.fetchProduct();
            if (product == null) {
                LOGGER.debug("Product not found for SKU {}.", productSku);
                return null;
            }

            // set link text to product name
            linkText = product.getName();

            ProductUrlFormat.Params params = new ProductUrlFormat.Params();
            params.setSku(productSku);
            params.setUrlKey(product.getUrlKey());
            params.setUrlPath(product.getUrlPath());
            params.setUrlRewrites(product.getUrlRewrites());

            return urlProvider.toProductUrl(request, productPage, params);
        }

        @Nullable
        private String prepareCategoryInfo(String categoryUid, Page categoryPage) {
            MagentoGraphqlClient magentoGraphqlClient = request.adaptTo(MagentoGraphqlClient.class);
            if (magentoGraphqlClient == null) {
                LOGGER.debug("GraphQL client not found for {}", request.getResource().getPath());
                return null;
            }

            AbstractCategoryRetriever categoryRetriever = new AbstractCategoryRetriever(magentoGraphqlClient) {
                @Override
                protected CategoryTreeQueryDefinition generateCategoryQuery() {
                    return q -> q
                        .name()
                        .urlPath()
                        .urlKey();
                }
            };
            categoryRetriever.setIdentifier(categoryUid);
            CategoryInterface category = categoryRetriever.fetchCategory();
            if (category == null) {
                LOGGER.debug("Category not found for UID {}.", categoryUid);
                return null;
            }

            // set link text to category name
            linkText = category.getName();

            CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
            params.setUid(categoryUid);
            params.setUrlKey(category.getUrlKey());
            params.setUrlPath(category.getUrlPath());

            return urlProvider.toCategoryUrl(request, categoryPage, params);
        }
    }
}
