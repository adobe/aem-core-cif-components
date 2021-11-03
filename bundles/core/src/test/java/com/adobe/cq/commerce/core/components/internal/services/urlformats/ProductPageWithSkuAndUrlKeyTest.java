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

import com.adobe.cq.commerce.core.components.services.urls.ProductPageUrlFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProductPageWithSkuAndUrlKeyTest {

    public final ProductPageUrlFormat subject = ProductPageWithSkuAndUrlKey.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        ProductPageUrlFormat.Params params = new ProductPageUrlFormat.Params();

        assertEquals("{{page}}.html/{{sku}}.html", subject.format(params));
    }

    @Test
    public void testFormat() {
        ProductPageUrlFormat.Params params = new ProductPageUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("foo-bar");
        params.setUrlKey("next-generation-foo-bar2021");

        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2021.html", subject.format(params));

        params.setVariantSku("variant");

        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2021.html#variant", subject.format(params));
    }

    @Test
    public void testFormatWithUrlKeyAndUrlPath() {
        ProductPageUrlFormat.Params params = new ProductPageUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("foo-bar");
        params.setUrlKey("next-generation-foo-bar2021");
        params.setUrlPath("next-generation-foo-bar2022");

        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2021.html", subject.format(params));
    }

    @Test
    public void testFormatWithUrlPath() {
        ProductPageUrlFormat.Params params = new ProductPageUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("foo-bar");
        params.setUrlPath("next-generation-foo-bar2022");

        assertEquals("/page/path.html/foo-bar/next-generation-foo-bar2022.html", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/next-generation-foo-bar2021.html");
        ProductPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertEquals("next-generation-foo-bar2021", parameters.getUrlKey());
    }

    @Test
    public void testParseNull() {
        ProductPageUrlFormat.Params parameters = subject.parse(null, null);
        assertNull(parameters.getPage());
        assertNull(parameters.getSku());
        assertNull(parameters.getUrlKey());
        assertNull(parameters.getUrlPath());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        ProductPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertNull(parameters.getSku());
        assertNull(parameters.getUrlKey());
    }

    @Test
    public void testParseSuffixNoSlash() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        ProductPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertNull(parameters.getUrlKey());
    }
}
