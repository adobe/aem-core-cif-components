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

import com.adobe.cq.commerce.core.components.services.urls.CategoryPageUrlFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CategoryPageWithUrlPathTest {

    public final CategoryPageUrlFormat subject = CategoryPageWithUrlPath.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        CategoryPageUrlFormat.Params params = new CategoryPageUrlFormat.Params();

        assertEquals("{{page}}.html/{{url_path}}.html", subject.format(params));
    }

    @Test
    public void testFormat() {
        CategoryPageUrlFormat.Params params = new CategoryPageUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlPath("foo-bar");

        assertEquals("/page/path.html/foo-bar.html", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/foobar.html");
        CategoryPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foobar", parameters.getUrlKey());
        assertEquals("foo-bar/foobar", parameters.getUrlPath());
    }

    @Test
    public void testParseNull() {
        CategoryPageUrlFormat.Params parameters = subject.parse(null, null);
        assertNull(parameters.getPage());
        assertNull(parameters.getUid());
        assertNull(parameters.getUrlKey());
        assertNull(parameters.getUrlPath());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        CategoryPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertNull(parameters.getUrlPath());
    }
}
