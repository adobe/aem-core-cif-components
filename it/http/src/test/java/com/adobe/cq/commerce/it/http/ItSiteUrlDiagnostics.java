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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CQClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TEMP (SITES-40396): remove this class once UrlProvider / product URL root cause is fixed.
 * Diagnostics for IT site product URL / {@code UrlProviderImpl} behaviour in CI.
 */
final class ItSiteUrlDiagnostics {

    private static final Logger LOG = LoggerFactory.getLogger(ItSiteUrlDiagnostics.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String URL_PROVIDER_PID = "com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl";

    private static final String GRAPHQL_CLIENT_CONFIG_ID = "com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~default";

    private static final Pattern PRODUCT_PAGE_SUFFIX = Pattern.compile(
        "product-page\\.html/([^#?]+)");

    private ItSiteUrlDiagnostics() {}

    static void logOsgiConfiguration(CQClient client) {
        logConfigMgrJson(client, URL_PROVIDER_PID, "UrlProviderImpl");
        logConfigMgrJson(client, GRAPHQL_CLIENT_CONFIG_ID, "GraphqlClientImpl~default");
        logConfigMgrListing(client, "UrlProvider");
        logConfigMgrListing(client, "GraphqlClientImpl");
    }

    /**
     * Logs every product card {@code href} on the category page plus a short classification of the URL shape.
     */
    static void logCategoryPageProductUrls(CQClient client, String categoryPageUrl, String highlightSku,
        String expectedCategoryUrlPath) {
        try {
            SlingHttpResponse response = client.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());
            Elements items = doc.select(".productcollection__item[data-product-sku]");
            LOG.info("CIF IT URL debug: category page {} — {} product card(s), highlight sku={}",
                categoryPageUrl, items.size(), highlightSku);
            LOG.info("CIF IT URL debug: expected IT site category url_path context={}", expectedCategoryUrlPath);

            int limit = Math.min(items.size(), 8);
            for (int i = 0; i < limit; i++) {
                Element item = items.get(i);
                String sku = item.attr("data-product-sku");
                String href = item.attr("href");
                LOG.info("CIF IT URL debug:   [{}] sku={} href={} shape={}",
                    i, sku, href, classifyProductHref(href, expectedCategoryUrlPath));
            }
            if (items.size() > limit) {
                LOG.info("CIF IT URL debug:   … {} more product card(s) omitted", items.size() - limit);
            }

            Elements highlight = doc.select(".productcollection__item[data-product-sku=" + highlightSku + "]");
            if (!highlight.isEmpty()) {
                String href = highlight.first().attr("href");
                LOG.info("CIF IT URL debug: highlight product {} href={} shape={}",
                    highlightSku, href, classifyProductHref(href, expectedCategoryUrlPath));
            } else {
                LOG.warn("CIF IT URL debug: highlight sku {} not found on {}", highlightSku, categoryPageUrl);
            }
        } catch (ClientException e) {
            LOG.warn("CIF IT URL debug: failed to load category page {}: {}", categoryPageUrl, e.getMessage());
        }
    }

    static String classifyProductHref(String href, String expectedCategoryUrlPath) {
        if (StringUtils.isBlank(href)) {
            return "missing-href";
        }
        Matcher m = PRODUCT_PAGE_SUFFIX.matcher(href);
        if (!m.find()) {
            return "no-product-page-suffix";
        }
        String suffix = m.group(1);
        if (suffix.endsWith(".html")) {
            suffix = suffix.substring(0, suffix.length() - ".html".length());
        }
        String[] segments = suffix.split("/");
        if (segments.length == 1) {
            return "short-form (single segment, typical url_key)";
        }
        if (segments.length == 2 && expectedCategoryUrlPath != null) {
            String leafCategory = StringUtils.substringAfterLast(expectedCategoryUrlPath, "/");
            if (segments[0].equals(leafCategory) || expectedCategoryUrlPath.endsWith("/" + segments[0])) {
                return "category+url_key shape (2 segments)";
            }
        }
        if (segments.length >= 2) {
            return "multi-segment (" + segments.length + " segments, typical url_path)";
        }
        return "unclassified (" + segments.length + " segments)";
    }

    private static void logConfigMgrJson(CQClient client, String configId, String label) {
        String path = "/system/console/configMgr/" + configId + ".json";
        try {
            SlingHttpResponse response = client.doGet(path, 200);
            String body = response.getContent();
            LOG.info("CIF IT OSGi debug [{}] GET {}: {}", label, path, summarizeConfigJson(body));
        } catch (ClientException e) {
            LOG.warn("CIF IT OSGi debug [{}] GET {} failed: {}", label, path, e.getMessage());
        }
    }

    private static void logConfigMgrListing(CQClient client, String pidContains) {
        try {
            SlingHttpResponse response = client.doGet("/system/console/configMgr.json", 200);
            JsonNode root = MAPPER.readTree(response.getContent());
            List<String> matches = new ArrayList<>();
            collectMatchingConfigIds(root, pidContains, matches);
            if (matches.isEmpty()) {
                LOG.info("CIF IT OSGi debug: no configMgr.json entries containing '{}'", pidContains);
            } else {
                LOG.info("CIF IT OSGi debug: configMgr entries containing '{}': {}", pidContains, matches);
                for (String configId : matches) {
                    if (!configId.contains(pidContains)) {
                        continue;
                    }
                    logConfigMgrJson(client, configId, configId);
                }
            }
        } catch (Exception e) {
            LOG.warn("CIF IT OSGi debug: could not list configMgr.json for '{}': {}", pidContains, e.getMessage());
        }
    }

    private static void collectMatchingConfigIds(JsonNode node, String pidContains, List<String> out) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            JsonNode pid = node.get("pid");
            if (pid != null && pid.isTextual() && pid.asText().contains(pidContains)) {
                out.add(pid.asText());
            }
            JsonNode id = node.get("id");
            if (id != null && id.isTextual() && id.asText().contains(pidContains)) {
                out.add(id.asText());
            }
            Iterator<String> fields = node.fieldNames();
            while (fields.hasNext()) {
                collectMatchingConfigIds(node.get(fields.next()), pidContains, out);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectMatchingConfigIds(child, pidContains, out);
            }
        }
    }

    private static String summarizeConfigJson(String body) {
        if (StringUtils.isBlank(body)) {
            return "(empty)";
        }
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode properties = root.get("properties");
            if (properties != null && properties.isArray()) {
                StringBuilder sb = new StringBuilder("{");
                for (JsonNode prop : properties) {
                    String name = prop.has("name") ? prop.get("name").asText() : "?";
                    if (!isInterestingUrlProperty(name)) {
                        continue;
                    }
                    String value = prop.has("value") ? prop.get("value").asText() : "";
                    if (sb.length() > 1) {
                        sb.append(", ");
                    }
                    sb.append(name).append('=').append(value);
                }
                sb.append("}");
                return sb.toString();
            }
            return StringUtils.abbreviate(body, 500);
        } catch (Exception e) {
            return StringUtils.abbreviate(body, 500);
        }
    }

    private static boolean isInterestingUrlProperty(String name) {
        return "productPageUrlFormat".equals(name)
            || "categoryPageUrlFormat".equals(name)
            || "enableContextAwareProductUrls".equals(name)
            || "url".equals(name)
            || "httpMethod".equals(name)
            || "identifier".equals(name);
    }
}
