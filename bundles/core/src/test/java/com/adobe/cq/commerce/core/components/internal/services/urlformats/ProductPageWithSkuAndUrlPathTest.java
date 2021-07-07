package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;
import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProductPageWithSkuAndUrlPathTest {

    public final UrlFormat subject = ProductPageWithSkuAndUrlPath.INSTANCE;

    @Test
    public void testFormatWithMissingParameters() {
        assertEquals("{{page}}.html/{{sku}}/{{url_path}}.html", subject.format(Collections.emptyMap()));
    }

    @Test
    public void testFormat() {
        assertEquals("/page/path.html/foo-bar/top-level-category/sub-category/next-generation-foo-bar2021.html",
            subject.format(ImmutableMap.of(
                "page", "/page/path",
                "sku", "foo-bar",
                "url_path", "top-level-category/sub-category/next-generation-foo-bar2021")));
    }

    @Test
    public void testParse() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar/top-level-category/sub-category/next-generation-foo-bar2021.html");
        Map<String, String> parameters = subject.parse(pathInfo);

        assertEquals("/page/path", parameters.get("page"));
        assertEquals("foo-bar", parameters.get("sku"));
        assertEquals("next-generation-foo-bar2021", parameters.get("url_key"));
        assertEquals("top-level-category/sub-category/next-generation-foo-bar2021", parameters.get("url_path"));
    }

    @Test
    public void testParseNull() {
        Map<String, String> parameters = subject.parse(null);
        assertTrue(parameters.isEmpty());
    }

    @Test
    public void testParseNoSuffix() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        Map<String, String> parameters = subject.parse(pathInfo);

        assertEquals("/page/path", parameters.get("page"));
        assertNull(parameters.get("sku"));
        assertNull(parameters.get("url_key"));
    }

    @Test
    public void testParseSuffixNoSku() {
        MockRequestPathInfo pathInfo = new MockRequestPathInfo();
        pathInfo.setResourcePath("/page/path");
        pathInfo.setSuffix("/foo-bar.html");
        Map<String, String> parameters = subject.parse(pathInfo);

        assertEquals("/page/path", parameters.get("page"));
        assertEquals("foo-bar", parameters.get("sku"));
        assertNull(parameters.get("url_key"));
        assertNull(parameters.get("url_path"));
    }
}
