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

import java.util.Arrays;
import java.util.Collections;

import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.services.urls.ProductUrlFormat;
import com.adobe.cq.commerce.magento.graphql.UrlRewrite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductPageWithUrlPathTest {

    public final ProductUrlFormat subject = ProductPageWithUrlPath.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();

        assertEquals("{{page}}.html/{{url_path}}.html", subject.format(params));
    }

    @Test
    public void testFormat() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlPath("foo-bar");

        assertEquals("/page/path.html/foo-bar.html", subject.format(params));

        params.setVariantSku("variant");

        assertEquals("/page/path.html/foo-bar.html#variant", subject.format(params));
    }

    @Test
    public void testFormatWithUrlRewrites() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setUrlKey("bar");
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("foo"),
            new UrlRewrite().setUrl("foo/bar")));

        assertEquals("/page/path.html/foo/bar.html", subject.format(params));

        params.setVariantSku("variant");

        assertEquals("/page/path.html/foo/bar.html#variant", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/foo-bar-product.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar/foo-bar-product", parameters.getUrlPath());
        assertEquals("foo-bar-product", parameters.getUrlKey());
        assertEquals("foo-bar", parameters.getCategoryUrlParams().getUrlPath());
        assertEquals("foo-bar", parameters.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testParseNoCategory() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getUrlKey());
        assertEquals("foo-bar", parameters.getUrlPath());
        assertNull(parameters.getCategoryUrlParams().getUrlPath());
        assertNull(parameters.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testParseWithNestedCategory() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/sub/category/deep.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar/sub/category/deep", parameters.getUrlPath());
        assertEquals("deep", parameters.getUrlKey());
        assertEquals("foo-bar/sub/category", parameters.getCategoryUrlParams().getUrlPath());
        assertEquals("category", parameters.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testParseNull() {
        ProductUrlFormat.Params parameters = subject.parse(null, null);
        assertNull(parameters.getPage());
        assertNull(parameters.getSku());
        assertNull(parameters.getUrlKey());
        assertNull(parameters.getUrlPath());
        assertNull(parameters.getCategoryUrlParams().getUrlKey());
        assertNull(parameters.getCategoryUrlParams().getUrlPath());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertNull(parameters.getUrlPath());
    }

    @Test
    public void testRetainParsableParameters() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("sku");
        params.setVariantSku("variant-sku");
        params.setUrlRewrites(Collections.singletonList(new UrlRewrite().setUrl("url-rewrites")));
        params.setUrlKey("url-key");
        params.setVariantUrlKey("variant-url-key");
        params.setUrlPath("url-path/url-path-sub/url-key");

        params = subject.retainParsableParameters(params);
        assertNull(params.getVariantSku());
        assertNull(params.getVariantUrlKey());
        assertTrue(params.getUrlRewrites().isEmpty());
        assertNull(params.getSku());
        assertEquals("/page/path", params.getPage());
        assertEquals("url-key", params.getUrlKey());
        assertEquals("url-path/url-path-sub/url-key", params.getUrlPath());
        assertEquals("url-path/url-path-sub", params.getCategoryUrlParams().getUrlPath());
        assertEquals("url-path-sub", params.getCategoryUrlParams().getUrlKey());
    }
}
