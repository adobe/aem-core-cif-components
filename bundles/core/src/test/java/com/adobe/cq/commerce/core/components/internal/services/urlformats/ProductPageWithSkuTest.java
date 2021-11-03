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

public class ProductPageWithSkuTest {

    public final ProductPageUrlFormat subject = ProductPageWithSku.INSTANCE;

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
        assertEquals("/page/path.html/foo-bar.html", subject.format(params));

        params.setVariantSku("variant");

        assertEquals("/page/path.html/foo-bar.html#variant", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        ProductPageUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
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
    }
}
