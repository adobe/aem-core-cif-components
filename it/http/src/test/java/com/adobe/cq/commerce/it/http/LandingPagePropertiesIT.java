/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.it.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.CommerceClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;

import static org.apache.http.HttpStatus.SC_OK;

public class LandingPagePropertiesIT {
    private static final String PAGE_PROPERTIES_URL = "/mnt/overlay/wcm/core/content/sites/properties.html";
    private static final String LANDING_PAGE_RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";
    private final String LANDING_PAGE_PATH = "/content/test-landing-page";

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();
    private static CQClient adminAuthor;

    @BeforeClass
    public static void init() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CommerceClient.class);
    }

    @Before
    public void setUp() throws Exception {
        // Create landing page
        adminAuthor.createNode(LANDING_PAGE_PATH, "cq:Page");
        adminAuthor.createNode(LANDING_PAGE_PATH + "/jcr:content", "cq:PageContent");
        List<NameValuePair> props = new ArrayList<>();
        props.add(new BasicNameValuePair("sling:resourceType", LANDING_PAGE_RESOURCE_TYPE));
        props.add(new BasicNameValuePair("jcr:title", "Test Landing Page"));
        props.add(new BasicNameValuePair("navRoot@TypeHint", "Boolean"));
        props.add(new BasicNameValuePair("navRoot", "true"));
        adminAuthor.setPageProperties(LANDING_PAGE_PATH, props, SC_OK);
    }

    @Test
    public void testLandingPageProperties() throws Exception {
        SlingHttpResponse response = adminAuthor.doGet(PAGE_PROPERTIES_URL + "?item=" + LANDING_PAGE_PATH, SC_OK);
        Document doc = Jsoup.parse(response.getContent());

        // Check commerce tab exits
        Elements elements = doc.select("coral-tab:contains(Commerce)");
        Assert.assertEquals(1, elements.size());

        // Check that commerce pages section is not displayed
        elements = doc.select("coral-panel h3:contains(Commerce Pages)");
        Assert.assertEquals(1, elements.size());
    }

    @After
    public void tearDown() throws Exception {
        // delete landing page
        adminAuthor.deletePath(LANDING_PAGE_PATH, SC_OK);
    }
}
