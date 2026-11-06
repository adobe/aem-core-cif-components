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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.junit.Test;

/**
 * Verifies the request filter's status codes and upstream WCM mode initialization on a running AEM instance.
 */
public class CatalogPageBackendErrorIT extends CommerceTestBase {

    private static final String PRODUCT_PAGE = COMMERCE_LIBRARY_PATH + "/product/sample-product";
    private static final String CATEGORY_PAGE = COMMERCE_LIBRARY_PATH + "/productlist/sample-productlist";
    private static final String EXAMPLES_CLIENT_PID = GRAPHQL_CLIENT_FACTORY_PID + "~examples";

    @Test
    public void testHealthyCatalogResponses() throws Exception {
        adminAuthor.doGet(PRODUCT_PAGE + ".html/unknown-product.html?wcmmode=disabled", 404);
        adminAuthor.doGet(CATEGORY_PAGE + ".html/outdoor.html?wcmmode=disabled&page=999", 200);
    }

    @Test
    public void testUnreachableBackendAndAuthorModes() throws Exception {
        OsgiConsoleClient osgiClient = adminAuthor.adaptTo(OsgiConsoleClient.class);
        Map<String, Object> original = new HashMap<>(osgiClient.getConfiguration(EXAMPLES_CLIENT_PID));
        Map<String, Object> unreachable = new HashMap<>(original);
        unreachable.put("url", "http://127.0.0.1:1/graphql");
        unreachable.put("allowHttpProtocol", "true");
        // Unique identifiers avoid previously cached healthy or missing-entity responses.
        String suffix = "/cif-it-backend-outage-" + UUID.randomUUID() + ".html";
        try {
            updateOSGiConfiguration(adminAuthor, unreachable, GRAPHQL_CLIENT_BUNDLE, EXAMPLES_CLIENT_PID);
            adminAuthor.doGet(PRODUCT_PAGE + ".html" + suffix + "?wcmmode=disabled", 503);
            adminAuthor.doGet(CATEGORY_PAGE + ".html" + suffix + "?wcmmode=disabled", 503);
            adminAuthor.doGet(PRODUCT_PAGE + ".model.json" + suffix + "?wcmmode=disabled", 503);
            adminAuthor.doGet(CATEGORY_PAGE + ".model.json" + suffix + "?wcmmode=disabled", 503);

            // These requests also verify that WCMMode is already initialized before the -6000 filter runs.
            adminAuthor.doGet(PRODUCT_PAGE + ".html" + suffix + "?wcmmode=edit", 200);
            adminAuthor.doGet(PRODUCT_PAGE + ".html" + suffix + "?wcmmode=preview", 200);
            adminAuthor.doGet(CATEGORY_PAGE + ".html" + suffix + "?wcmmode=edit", 200);
            adminAuthor.doGet(CATEGORY_PAGE + ".html" + suffix + "?wcmmode=preview", 200);
        } finally {
            updateOSGiConfiguration(adminAuthor, original, GRAPHQL_CLIENT_BUNDLE, EXAMPLES_CLIENT_PID);
        }
    }
}
