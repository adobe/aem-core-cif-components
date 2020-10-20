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

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StoreConfigExporterIT extends CommerceTestBase {

    private Element body;

    @Before
    public void loadPage() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(COMMERCE_LIBRARY_PATH + "/productteaser.html", 200);
        Document doc = Jsoup.parse(response.getContent());
        body = doc.select("body").first();
    }

    @Test
    public void testStoreView() {
        Assert.assertEquals("default", body.dataset().get("store-view"));
    }

    @Test
    public void testGraphqlEndpoint() {
        Assert.assertEquals("/apps/cif-components-examples/graphql", body.dataset().get("graphql-endpoint"));
    }

    @Test
    public void testGraphqlMethod() {
        Assert.assertEquals("GET", body.dataset().get("graphql-method"));
    }

}
