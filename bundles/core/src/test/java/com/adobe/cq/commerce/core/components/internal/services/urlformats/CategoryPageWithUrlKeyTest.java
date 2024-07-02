/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.CategoryUrlFormat;

import static org.junit.Assert.*;

public class CategoryPageWithUrlKeyTest {

    public final CategoryUrlFormat subject = CategoryPageWithUrlKey.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();

        assertEquals("{{page}}.html/{{url_key}}.html", subject.format(params));
    }

    @Test
    public void testFormat() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlKey("foo-bar");

        assertEquals("/page/path.html/foo-bar.html", subject.format(params));
    }

    @Test
    public void testFormatWithUrlKeyAndUrlPath() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlKey("foo-bar");
        params.setUrlPath("foo-bar2");
        assertEquals("/page/path.html/foo-bar.html", subject.format(params));
    }

    @Test
    public void testFormatWithUrlPath() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlPath("foo-bar2");
        assertEquals("/page/path.html/foo-bar2.html", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        CategoryUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getUrlKey());
    }

    @Test
    public void testParseNull() {
        CategoryUrlFormat.Params parameters = subject.parse(null, null);
        assertNull(parameters.getPage());
        assertNull(parameters.getUid());
        assertNull(parameters.getUrlKey());
        assertNull(parameters.getUrlPath());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        CategoryUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertNull(parameters.getUrlKey());
    }

    @Test
    public void testRetainParsableParameters() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setPage("/page/path");
        params.setUid("uid");
        params.setUrlKey("url-key");
        params.setUrlPath("url-path/url-key");

        params = subject.retainParsableParameters(params);
        assertNull(params.getUid());
        assertNull(params.getUrlPath());
        assertEquals("/page/path", params.getPage());
        assertEquals("url-key", params.getUrlKey());
    }

    @Test
    public void testValidateRequiredParamsWhenUrlKeyIsSet() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlKey("url-key");
        assertTrue(subject.validateRequiredParams(params));
    }

    @Test
    public void testValidateRequiredParamsWhenUrlKeyIsNotSet() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUid("uid");
        assertFalse(subject.validateRequiredParams(params));
    }

    @Test
    public void testValidateRequiredParamsWhenUrlPathIsSet() {
        CategoryUrlFormat.Params params = new CategoryUrlFormat.Params();
        params.setUrlKey("urlPath");
        assertTrue(subject.validateRequiredParams(params));
    }
}
