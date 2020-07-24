/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.CommerceClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;

import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

public class CommerceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CommerceTestBase.class);

    public static final String GRAPHQL_CLIENT_BUNDLE = "com.adobe.commerce.cif.graphql-client";
    public static final String GRAPHQL_CLIENT_FACTORY_PID = "com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl";

    protected static final String COMMERCE_LIBRARY_PATH = "/content/core-components-examples/library/commerce";
    protected static final String CMP_EXAMPLES_DEMO_SELECTOR = ".cmp-examples-demo__top";

    private static final String CONFIGURATION_CONSOLE_URL = "/system/console/configMgr";

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    protected static CQClient adminAuthor;

    @BeforeClass
    public static void init() throws ClientException, InterruptedException, TimeoutException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CommerceClient.class);

        // This configures the GraphQL client for the CIF components library
        GraphqlOSGiConfig graphqlOsgiConfig = new GraphqlOSGiConfig()
            .withIdentifier("examples")
            .withUrl("http://localhost:4502/apps/cif-components-examples/graphql")
            .withHttpMethod("GET")
            .withAcceptSelfSignedCertificates(true)
            .withAllowHttpProtocol(true);

        updateOSGiConfiguration(adminAuthor, graphqlOsgiConfig.build(), GRAPHQL_CLIENT_BUNDLE, GRAPHQL_CLIENT_FACTORY_PID);
        updateSlingAuthenticatorOSGiConfig(adminAuthor);
    }

    /**
     * Fetches the PID of a service based on the factory PID.
     * 
     * @param osgiClient
     * @return The PID of the first configuration found for factory PID.
     * @throws ClientException
     */
    private static String getConfigurationPid(OsgiConsoleClient osgiClient, String factoryPID) throws ClientException {
        SlingHttpResponse resp = osgiClient.doGet(CONFIGURATION_CONSOLE_URL + "/*.json");
        JsonNode json = JsonUtils.getJsonNodeFromString(resp.getContent());
        Iterator<JsonNode> it = json.getElements();
        while (it.hasNext()) {
            JsonNode config = it.next();
            JsonNode factoryId = config.get("factoryPid");
            if (factoryId != null && factoryPID.equals(factoryId.getTextValue())) {
                return config.get("pid").getTextValue();
            }
        }
        return null;
    }

    protected static void updateOSGiConfiguration(CQClient client, Map<String, Object> config, String bundle, String factoryPID)
        throws ClientException,
        TimeoutException, InterruptedException {
        final OsgiConsoleClient osgiClient = client.adaptTo(OsgiConsoleClient.class);
        Polling polling = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    String state = osgiClient.getBundleState(bundle);
                    LOG.info("Bundle {} state is now {}", bundle, state);
                    Assert.assertEquals("Active", state);
                    return true;
                } catch (AssertionError e) {
                    return false;
                } catch (ClientException cex) {
                    LOG.error(cex.getMessage(), cex);
                    return false;
                }
            }
        });

        // Check that the bundle has started
        polling.poll(30000, 1000);

        LOG.info("Creating configuration. {}", config);
        String configurationPid = getConfigurationPid(osgiClient, factoryPID);
        osgiClient.waitEditConfiguration(30, configurationPid, null, config, SC_MOVED_TEMPORARILY);

        // Wait for bundle to restart
        polling.poll(30000, 1000);

        // Wait a bit more so that other bundles can restart
        Thread.sleep(2000);
    }

    protected static void updateSlingAuthenticatorOSGiConfig(CQClient client) throws InterruptedException, ClientException,
        TimeoutException {

        // We keep all the parameters from the default config, we only add the path for the GraphQL servlet

        Map<String, Object> config = new HashMap<>();
        config.put("auth.sudo.cookie", "sling.sudo");
        config.put("auth.sudo.parameter", "sudo");
        config.put("auth.annonymous", "false");
        config.put("sling.auth.requirements", new String[] {
            "+/",
            "-/libs/granite/core/content/login",
            "-/etc.clientlibs",
            "-/etc/clientlibs/granite",
            "-/libs/dam/remoteassets/content/loginerror",
            "-/apps/cif-components-examples/graphql" // We have to add this path so that the GraphQL servlet is reachable
        });
        config.put("sling.auth.anonymous.user", "");
        config.put("sling.auth.anonymous.password", "unmodified");
        config.put("auth.http", "preemptive");
        config.put("auth.http.realm", "Sling+(Development)");
        config.put("auth.uri.suffix", "/j_security_check");

        final OsgiConsoleClient osgiClient = client.adaptTo(OsgiConsoleClient.class);
        osgiClient.waitEditConfiguration(30, "org.apache.sling.engine.impl.auth.SlingAuthenticator", null, config, SC_MOVED_TEMPORARILY);

        // Wait a bit more so that other bundles can restart
        Thread.sleep(2000);
    }
}
