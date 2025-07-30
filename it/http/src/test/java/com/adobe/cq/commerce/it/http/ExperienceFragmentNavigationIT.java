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

public class ExperienceFragmentNavigationIT extends CommerceTestBase {

    private static final String EXPERIENCE_FRAGMENT_PATH = "/content/experience-fragments/venia/us/en/site/header/master.html";
    private static final String NAVIGATION_GROUP_SELECTOR = ".cmp-navigation__group";
    private static final String NAVIGATION_ITEM_LINK_SELECTOR = ".cmp-navigation__item-link";

    @Test
    public void testExperienceFragmentNavigation() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(EXPERIENCE_FRAGMENT_PATH, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that the main navigation group exists
        Elements navigationGroups = doc.select(NAVIGATION_GROUP_SELECTOR);
        Assert.assertFalse("Navigation group should exist", navigationGroups.isEmpty());

        // Get the main navigation group (first one)
        Element mainNavigationGroup = navigationGroups.first();
        Assert.assertNotNull("Main navigation group should not be null", mainNavigationGroup);
        Assert.assertTrue("Main navigation group should have cmp-navigation__group class",
            mainNavigationGroup.hasClass("cmp-navigation__group"));

        // Get all level-0 navigation items
        Elements level0Items = doc.select(".cmp-navigation__item--level-0");
        Assert.assertFalse("Level 0 navigation items should exist", level0Items.isEmpty());

        for (Element level0Item : level0Items) {
            Element link = level0Item.select(NAVIGATION_ITEM_LINK_SELECTOR).first();
            if (link != null) {
                // Validate the link has proper href
                String href = link.attr("href");
                Assert.assertNotNull("Navigation link href should not be null", href);
                Assert.assertFalse("Navigation link href should not be empty", href.trim().isEmpty());
                Assert.assertTrue("Navigation link href should start with /content", href.startsWith("/content"));

                // Validate the link has text content
                String linkText = link.text();
                Assert.assertNotNull("Navigation link should have text content", linkText);
                Assert.assertFalse("Navigation link text should not be empty", linkText.trim().isEmpty());
            }
        }
    }
}