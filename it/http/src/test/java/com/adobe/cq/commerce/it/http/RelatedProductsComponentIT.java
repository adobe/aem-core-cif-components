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

public class RelatedProductsComponentIT extends CommerceTestBase {

    // Differentiates between the HTML output of the component itself, and the tab displaying the HTML output
    private static final String RELATEDPRODUCTS_SELECTOR = CMP_EXAMPLES_DEMO_SELECTOR + " .relatedproducts";

    @Test
    public void testRelatedProductsWithSampleData() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/relatedproducts.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        Elements relatedProducts = doc.select(RELATEDPRODUCTS_SELECTOR);

        Assert.assertTrue(relatedProducts.size() > 0);

        // Verify component title
        Elements elements = relatedProducts.first().select(".productcarousel__title");
        Assert.assertEquals("Upsells for the Summit Backback!", elements.first().html());

        // Check that the components shows 2 products
        elements = relatedProducts.first().select(".productcarousel__cardscontainer > .product__card");
        Assert.assertEquals(2, elements.size());
    }

    @Test
    public void testRelatedProductsWithAddToCartAndAddToWishList() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/relatedproducts.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        Elements relatedProductsComponents = doc.select(RELATEDPRODUCTS_SELECTOR);

        Assert.assertTrue(relatedProductsComponents.size() > 1);

        Element relatedProducts = relatedProductsComponents.get(1);

        // Check that the components shows 3 products with add to cart and add to wish list button
        Elements elements = relatedProducts.select(".productcarousel__cardscontainer > .product__card");
        Assert.assertEquals(2, elements.size());

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
