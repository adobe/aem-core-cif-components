/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

public class ProductCarouselComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String PRODUCTCAROUSEL_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .productcarousel ";

    @Test
    public void testProductCarouselWithSampleProducts() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productcarousel.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        Elements carousels = doc.select(PRODUCTCAROUSEL_SELECTOR);

        Assert.assertTrue(carousels.size() > 0);

        Element carousel = carousels.first();

        // Verify component title
        Elements elements = carousel.select(".productcarousel__title");
        Assert.assertEquals("Summer promotions!", elements.first().html());

        // Check that the components shows 4 products
        elements = carousel.select(".productcarousel__cardscontainer > .product__card");
        Assert.assertEquals(4, elements.size());
    }

    @Test
    public void testProductCarouselWithCategoryProducts() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productcarousel.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        Elements carousels = doc.select(PRODUCTCAROUSEL_SELECTOR);

        Assert.assertTrue(carousels.size() > 1);

        Element carousel = carousels.get(1);

        // Verify component title
        Elements elements = carousel.select(".productcarousel__title");
        Assert.assertEquals("Sports selection", elements.first().html());

        // Check that the components shows 3 products
        elements = carousel.select(".productcarousel__cardscontainer > .product__card");
        Assert.assertEquals(3, elements.size());
    }

    @Test
    public void testProductCarouselWithAddToCartAndAddToWishList() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productcarousel.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        Elements carousels = doc.select(PRODUCTCAROUSEL_SELECTOR);

        Assert.assertTrue(carousels.size() > 2);

        Element carousel = carousels.get(2);

        // Check that the components shows 3 products with add to cart and add to wish list button
        Elements elements = carousel.select(".productcarousel__cardscontainer > .product__card");
        Assert.assertEquals(3, elements.size());

        for (Element element : elements) {
            Elements addToCart = element.select(".product__card-button--add-to-cart");
            Assert.assertEquals(1, addToCart.size());
            Assert.assertTrue(addToCart.first().html().contains("Add to Cart"));

            Elements addToWishList = element.select(".product__card-button--add-to-wish-list");
            Assert.assertEquals(1, addToWishList.size());
            Assert.assertTrue(addToWishList.first().html().contains("Add to Wish List"));
        }
    }
}
