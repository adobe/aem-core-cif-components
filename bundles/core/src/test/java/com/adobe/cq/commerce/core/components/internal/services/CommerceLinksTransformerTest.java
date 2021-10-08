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
    @Rule
    public final AemContext context = newAemContext("/context/jcr-content.json");
    private Transformer transformer;

    @Before
    public void before() throws Exception {
        // setup GraphQL client for UrlProvider
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        context.registerService(HttpClientBuilderFactory.class, new MockHttpClientBuilderFactory(httpClient));
        GraphqlClientImpl graphqlClient = spy(new GraphqlClientImpl());
        context.registerInjectActivateService(graphqlClient, "httpMethod", "POST");
        context.registerAdapter(Resource.class, GraphqlClient.class, graphqlClient);
        Utils.setupHttpResponse("graphql/magento-graphql-product-result.json", httpClient, HttpStatus.SC_OK,
            "{products(filter:{sku:{eq:\"MJ01\"}}");
        Utils.setupHttpResponse("graphql/magento-graphql-category-list-result.json", httpClient, HttpStatus.SC_OK,
            "{categoryList(filters:{category_uid:{eq:\"uid-5\"}}");

        // setup UrlRewriterTransformer
        MockSlingHttpServletRequest mockRequest = context.request();
        mockRequest.setResource(context.resourceResolver().getResource(TEST_PAGE));
        ProcessingContext mockProcessingContext = mock(ProcessingContext.class);
        when(mockProcessingContext.getRequest()).thenReturn(mockRequest);
        CommerceLinksTransformerFactory factory = new CommerceLinksTransformerFactory();
        context.registerInjectActivateService(factory);
        transformer = factory.createTransformer();
        transformer.init(mockProcessingContext, null);
    }

    @Test
    public void testTransformer() throws Exception {
        // read and transform HTML
        StringWriter writer = new StringWriter();
        transformer.setContentHandler(new ToXmlContentHandler(writer));
        ParsingContentHandler parsingContentHandler = new ParsingContentHandler(transformer);
        parsingContentHandler.parse(Utils.class.getClassLoader().getResourceAsStream(TEST_HTML));

        // verify transformed HTML
        String html = writer.toString();
        Document document = Jsoup.parse(html);
        Elements anchors = document.select(ELEMENT_ANCHOR);

        assertEquals(12, anchors.size());
        checkAnchor(anchors.get(0), null, null);
        checkAnchor(anchors.get(1), null, "any");
        checkAnchor(anchors.get(2), "uid-5", "/content/category-page.html/equipment.html");
        checkAnchor(anchors.get(3), null, null);
        checkAnchor(anchors.get(4), null, "any");
        checkAnchor(anchors.get(5), "MJ01", "/content/product-page.html/beaumont-summit-kit.html");
        checkAnchor(anchors.get(6), null, null);
        checkAnchor(anchors.get(7), null, "any");
        checkAnchor(anchors.get(8), null, MARKER_COMMERCE_LINKS);
        checkAnchor(anchors.get(9), null, null);
        checkAnchor(anchors.get(10), null, "any");
        checkAnchor(anchors.get(11), "MJ01", "/content/product-page.html/beaumont-summit-kit.html");
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
}
