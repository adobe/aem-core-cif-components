/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.UrlFormat;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductPageWithSkuAndUrlKeyTest {

    public final UrlFormat subject = ProductPageWithSkuAndUrlKey.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        assertEquals("{{page}}.html/{{sku}}/{{url_key}}.html", subject.format(Collections.emptyMap()));
    }

    @Test
    public void testFormat() {
        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2021.html", subject.format(ImmutableMap.of(
            "page", "/page/path",
            "sku", "foo-bar",
            "url_key", "next-generation-foo-bar2021")));
    }

    @Test
    public void testFormatWithUrlKeyAndUrlPath() {
        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2021.html", subject.format(ImmutableMap.of(
            "page", "/page/path",
            "sku", "foo-bar",
            "url_key", "next-generation-foo-bar2021",
            "url_path", "next-generation-foo-bar2022")));
    }

    @Test
    public void testFormatWithUrlPath() {
        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2022.html", subject.format(ImmutableMap.of(
            "page", "/page/path",
            "sku", "foo-bar",
            "url_path", "next-generation-foo-bar2022")));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/next-generation-foo-bar2021.html");
        Map<String, String> parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.get("page"));
        assertEquals("foo-bar", parameters.get("sku"));
        assertEquals("next-generation-foo-bar2021", parameters.get("url_key"));
    }

    @Test
    public void testParseNull() {
        Map<String, String> parameters = subject.parse(null, null);
        assertTrue(parameters.isEmpty());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        Map<String, String> parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.get("page"));
        assertNull(parameters.get("sku"));
        assertNull(parameters.get("url_key"));
    }

    @Test
    public void testParseSuffixNoSlash() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        Map<String, String> parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.get("page"));
        assertEquals("foo-bar", parameters.get("sku"));
        assertNull(parameters.get("url_key"));
    }
}
