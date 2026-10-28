/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.it.http;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProductTeaserTemplatesIT extends ItSiteTestBase {

    private final String pagePath = "/content/productteaser-links-" + UUID.randomUUID();

    @Before
    public void setUp() throws ClientException {
        adminAuthor.createNode(pagePath, "cq:Page");
        adminAuthor.createNode(pagePath + "/jcr:content", "cq:PageContent");
        List<NameValuePair> properties = new ArrayList<>();
        properties.add(new BasicNameValuePair("sling:resourceType",
            "cif-components-it-site/components/commerce/productteaser"));
        properties.add(new BasicNameValuePair("url", "/content/product.html"));
        properties.add(new BasicNameValuePair("name", "Test Product"));
        properties.add(new BasicNameValuePair("image", "/content/dam/product.jpg"));
        properties.add(new BasicNameValuePair("imageAlt", "Test Product Image"));
        adminAuthor.setPageProperties(pagePath, properties, 200);
    }

    @After
    public void tearDown() throws ClientException {
        adminAuthor.deletePath(pagePath, 200);
    }

    @Test
    public void testDefaultTarget() throws ClientException {
        verifyLinks(null);
    }

    @Test
    public void testNewTabTarget() throws ClientException {
        List<NameValuePair> properties = new ArrayList<>();
        properties.add(new BasicNameValuePair("linkTarget", "_blank"));
        adminAuthor.setPageProperties(pagePath, properties, 200);
        verifyLinks("_blank");
    }

    private void verifyLinks(String target) throws ClientException {
        Document document = Jsoup.parse(adminAuthor.doGet(pagePath + "/jcr:content.links.html", 200).getContent());
        verifyLinks(document, "title-linked", true, target);
        verifyLinks(document, "title-not-linked", false, target);
        verifyLinks(document, "title-linked-omitted", false, target);
        Assert.assertEquals(document.select("#title-not-linked a.item__images").first().outerHtml(),
            document.select("#title-linked-omitted a.item__images").first().outerHtml());
    }

    private void verifyLinks(Document document, String id, boolean titleLinked, String target) {
        Elements images = document.select("#" + id + " a.item__images");
        Elements titles = document.select("#" + id + " a.item__name");
        Assert.assertEquals(id, 1, images.size());
        Assert.assertEquals(id, 1, titles.size());
        Element image = images.first();
        Element title = titles.first();
        for (Element link : new Element[] { image, title }) {
            Assert.assertEquals("/content/product.html", link.attr("href"));
            if (target == null) {
                Assert.assertFalse(link.hasAttr("target"));
            } else {
                Assert.assertEquals(target, link.attr("target"));
            }
        }
        Assert.assertEquals("Test Product", image.attr("title"));
        Elements imageElements = image.select("img.item__image");
        Assert.assertEquals(1, imageElements.size());
        Assert.assertEquals("/content/dam/product.jpg", imageElements.first().attr("src"));
        Assert.assertEquals("Test Product Image", imageElements.first().attr("alt"));
        Assert.assertEquals("Test Product", title.text());
        Assert.assertFalse(title.hasAttr("tabindex"));
        Assert.assertFalse(title.hasAttr("aria-hidden"));
        if (titleLinked) {
            Assert.assertEquals("-1", image.attr("tabindex"));
            Assert.assertEquals("true", image.attr("aria-hidden"));
        } else {
            Assert.assertFalse(image.hasAttr("tabindex"));
            Assert.assertFalse(image.hasAttr("aria-hidden"));
        }
    }
}
