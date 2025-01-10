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

import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.osgi.services.HttpClientBuilderFactory;
import org.apache.jackrabbit.commons.xml.ParsingContentHandler;
import org.apache.jackrabbit.commons.xml.ToXmlContentHandler;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.MockHttpClientBuilderFactory;
import com.adobe.cq.commerce.core.testing.Utils;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.components.internal.services.CommerceLinksTransformerFactory.*;
import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CommerceLinksTransformerTest {
    private static final String TEST_PAGE = "/content/pageA";
    private static final String TEST_HTML = "rewriter/ciflinks.html";
    public static final Configuration CONFIG_DISABLED = new Configuration() {
        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    };
    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");
    private CommerceLinksTransformerFactory transformerFactory;
    private ProcessingContext mockProcessingContext;

    @Before
    public void before() throws Exception {
        // setup GraphQL client for UrlProvider
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        GraphqlClientImpl graphqlClient = spy(new GraphqlClientImpl());
        // Activate the GraphqlClientImpl with configuration
        context.registerInjectActivateService(graphqlClient, ImmutableMap.<String, Object>builder()
            .put("httpMethod", "POST")
            .put("url", "https://localhost")
            .build());
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"uid-5\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-breadcrumb-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"MTM=\"}}");

        // setup UrlRewriterTransformer
        MockSlingHttpServletRequest mockRequest = context.request();
        mockRequest.setResource(context.resourceResolver().getResource(TEST_PAGE));
        mockProcessingContext = mock(ProcessingContext.class);
        when(mockProcessingContext.getRequest()).thenReturn(mockRequest);
        transformerFactory = new CommerceLinksTransformerFactory();
        context.registerInjectActivateService(transformerFactory);
    }

    @Test
    public void testTransformerEnabled() throws Exception {
        Transformer transformer = transformerFactory.createTransformer();
        transformer.init(mockProcessingContext, null);

        // read and transform HTML
        StringWriter writer = new StringWriter();
        transformer.setContentHandler(new ToXmlContentHandler(writer));
        ParsingContentHandler parsingContentHandler = new ParsingContentHandler(transformer);
        parsingContentHandler.parse(this.getClass().getClassLoader().getResourceAsStream(TEST_HTML));

        // verify transformed HTML
        String html = writer.toString();
        Document document = Jsoup.parse(html);
        Elements anchors = document.select(ELEMENT_ANCHOR);

        assertEquals(18, anchors.size());
        checkAnchor(anchors.get(0), "uid-5", null);
        checkAnchor(anchors.get(1), "uid-5", "any");
        checkAnchor(anchors.get(2), "uid-5", "/content/category-page.html/equipment.html");
        checkAnchor(anchors.get(3), "MJ01", null);
        checkAnchor(anchors.get(4), "MJ01", "any");
        checkAnchor(anchors.get(5), "MJ01", "/content/product-page.html/beaumont-summit-kit.html");
        checkAnchorText(anchors.get(5), "Test product link with marker href.");
        checkAnchor(anchors.get(6), null, null);
        checkAnchor(anchors.get(7), null, "any");
        checkAnchor(anchors.get(8), null, MARKER_COMMERCE_LINKS);
        checkAnchor(anchors.get(9), "MJ01", null);
        checkAnchor(anchors.get(10), "MJ01", "any");
        checkAnchor(anchors.get(11), "MJ01", "/content/product-page.html/beaumont-summit-kit.html");
        checkAnchor(anchors.get(12), "MJ01", "/content/product-page.html/beaumont-summit-kit.html");
        checkAnchorTextAndTitle(anchors.get(12), "Beaumont Summit Kit", "Beaumont Summit Kit");
        checkAnchor(anchors.get(13), "uid-5", "/content/category-page.html/equipment.html");
        checkAnchorTextAndTitle(anchors.get(13), "Equipment", "Equipment");
        checkAnchorTextAndTitle(anchors.get(14), "Equipment", "My Category");
        checkAnchorText(anchors.get(15), "Equipment");
        checkAnchorText(anchors.get(16), "Tops");
        checkAnchorText(anchors.get(17), "Equipment");
    }

    @Test
    public void testTransformerDisabled() throws Exception {
        transformerFactory.activate(CONFIG_DISABLED);

        Transformer transformer = transformerFactory.createTransformer();
        transformer.init(mockProcessingContext, null);

        // read and transform HTML
        StringWriter writer = new StringWriter();
        transformer.setContentHandler(new ToXmlContentHandler(writer));
        ParsingContentHandler parsingContentHandler = new ParsingContentHandler(transformer);
        ClassLoader classLoader = this.getClass().getClassLoader();
        parsingContentHandler.parse(classLoader.getResourceAsStream(TEST_HTML));

        // verify transformed HTML
        String transformedHtml = writer.toString();

        Path filePath = Paths.get(classLoader.getResource(TEST_HTML).toURI());
        String originalHtml = new String(Files.readAllBytes(filePath));
        assertTrue(transformedHtml.endsWith(originalHtml));
    }

    private void checkAnchor(Element anchor, String commerceIdentifier, String href) {
        // id (arbitrary attributes) preserved
        assertTrue(isNotBlank(anchor.attr("id")));

        // text preserved
        assertEquals(1, anchor.childNodeSize());
        assertTrue(anchor.childNode(0) instanceof TextNode);
        assertTrue(isNotBlank(((TextNode) anchor.childNode(0)).getWholeText()));

        // href matched
        if (href == null) {
            assertTrue(isBlank(anchor.attr(ATTR_HREF)));
        } else {
            assertEquals(href, anchor.attr(ATTR_HREF));
        }

        // category uid or product sku matched
        if (commerceIdentifier != null) {
            assertTrue(commerceIdentifier.equals(anchor.attr(ATTR_CATEGORY_UID)) ^ commerceIdentifier.equals(anchor.attr(
                ATTR_PRODUCT_SKU)));
        } else {
            assertFalse(anchor.hasAttr(ATTR_CATEGORY_UID));
            assertFalse(anchor.hasAttr(ATTR_PRODUCT_SKU));
        }
    }

    private void checkAnchorText(Element anchor, String text) {
        assertEquals(1, anchor.childNodeSize());
        assertTrue(anchor.childNode(0) instanceof TextNode);
        assertEquals(text, ((TextNode) anchor.childNode(0)).getWholeText());
    }

    private void checkAnchorTextAndTitle(Element anchor, String text, String title) {
        checkAnchorText(anchor, text);
        assertEquals(title, anchor.attr(ATTR_TITLE));
    }
}
