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

public class ProductPageWithSkuAndUrlPathTest {

    public final ProductUrlFormat subject = ProductPageWithSkuAndUrlPath.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();

        assertEquals("{{page}}.html/{{sku}}.html", subject.format(params));
    }

    @Test
    public void testFormatWithMissingUrlPath() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("foo-bar");

        assertEquals("/page/path.html/foo-bar.html", subject.format(params));
    }

    @Test
    public void testFormatWithUrlRewrites() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("foo-bar");
        params.setUrlKey("bar");
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("foo"),
            new UrlRewrite().setUrl("foo/bar")));

        assertEquals("/page/path.html/foo-bar/foo/bar.html", subject.format(params));
    }

    @Test
    public void testFormat() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku(("foo-bar"));
        params.setUrlPath("top-level-category/sub-category/next-generation-foo-bar2021");

        assertEquals("/page/path.html/foo-bar/top-level-category/sub-category/next-generation-foo-bar2021.html", subject.format(params));

        params.setVariantSku("v");

        assertEquals("/page/path.html/foo-bar/top-level-category/sub-category/next-generation-foo-bar2021.html#v", subject.format(params));
    }

    @Test
    public void testFormatWithContext() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku(("foo-bar"));
        params.setUrlKey("bar");
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("bar"),
            new UrlRewrite().setUrl("foo/bar"),
            new UrlRewrite().setUrl("foo/barfoo/bar"),
            new UrlRewrite().setUrl("foo/foobar/bar")));

        // first, most specific url_path (canonical)
        assertEquals("/page/path.html/foo-bar/foo/barfoo/bar.html", subject.format(params));

        // prefix match
        params.getCategoryUrlParams().setUrlPath("foo/foobar");

        assertEquals("/page/path.html/foo-bar/foo/foobar/bar.html", subject.format(params));

        params.getCategoryUrlParams().setUrlPath("foo");

        assertEquals("/page/path.html/foo-bar/foo/barfoo/bar.html", subject.format(params));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/top-level-category/next-generation-foo-bar2021.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertEquals("next-generation-foo-bar2021", parameters.getUrlKey());
        assertEquals("top-level-category/next-generation-foo-bar2021", parameters.getUrlPath());
        assertEquals("top-level-category", parameters.getCategoryUrlParams().getUrlPath());
        assertEquals("top-level-category", parameters.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testParseNoCategory() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/next-generation-foo-bar2021.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertEquals("next-generation-foo-bar2021", parameters.getUrlKey());
        assertEquals("next-generation-foo-bar2021", parameters.getUrlPath());
        assertNull(parameters.getCategoryUrlParams().getUrlPath());
        assertNull(parameters.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testParseWithNestedCategory() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/top-level-category/sub-category/next-generation-foo-bar2021.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertEquals("next-generation-foo-bar2021", parameters.getUrlKey());
        assertEquals("top-level-category/sub-category/next-generation-foo-bar2021", parameters.getUrlPath());
        assertEquals("top-level-category/sub-category", parameters.getCategoryUrlParams().getUrlPath());
        assertEquals("sub-category", parameters.getCategoryUrlParams().getUrlKey());
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
        assertNull(parameters.getSku());
        assertNull(parameters.getUrlKey());
    }

    @Test
    public void testParseSuffixOnlySku() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        ProductUrlFormat.Params parameters = subject.parse(pathInfo, null);

        assertEquals("/page/path", parameters.getPage());
        assertEquals("foo-bar", parameters.getSku());
        assertNull(parameters.getUrlKey());
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
        assertEquals("url-path/url-path-sub/url-key", params.getUrlPath());
        assertEquals("/page/path", params.getPage());
        assertEquals("sku", params.getSku());
        assertEquals("url-key", params.getUrlKey());
        assertEquals("url-path/url-path-sub", params.getCategoryUrlParams().getUrlPath());
        assertEquals("url-path-sub", params.getCategoryUrlParams().getUrlKey());
    }

    @Test
    public void testRetainParsableParametersWithContext() {
        ProductUrlFormat.Params params = new ProductUrlFormat.Params();
        params.setPage("/page/path");
        params.setSku("sku");
        params.setUrlRewrites(Arrays.asList(
            new UrlRewrite().setUrl("url-key"),
            new UrlRewrite().setUrl("url-path/url-key")));
        params.setUrlKey("url-key");
        params.getCategoryUrlParams().setUrlPath("url-path");

        params = subject.retainParsableParameters(params);
        assertNull(params.getVariantSku());
        assertNull(params.getVariantUrlKey());
        assertTrue(params.getUrlRewrites().isEmpty());
        assertEquals("url-path/url-key", params.getUrlPath());
        assertEquals("/page/path", params.getPage());
        assertEquals("sku", params.getSku());
        assertEquals("url-key", params.getUrlKey());
        assertEquals("url-path", params.getCategoryUrlParams().getUrlPath());
        assertEquals("url-path", params.getCategoryUrlParams().getUrlKey());
    }
}
