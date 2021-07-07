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

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProductPageWithUrlKeyTest {

    public final UrlFormat subject = ProductPageWithUrlKey.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        assertEquals("{{page}}.html/{{url_key}}.html", subject.format(Collections.emptyMap()));
    }

    @Test
    public void testFormat() {
        assertEquals("/page/path.html/foo-bar.html", subject.format(ImmutableMap.of(
            "page", "/page/path",
            "url_key", "foo-bar")));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        Map<String, String> parameters = subject.parse(pathInfo);

        assertEquals("/page/path", parameters.get("page"));
        assertEquals("foo-bar", parameters.get("url_key"));
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        Map<String, String> parameters = subject.parse(pathInfo);

        assertEquals("/page/path", parameters.get("page"));
        assertNull(parameters.get("url_key"));
    }
}
