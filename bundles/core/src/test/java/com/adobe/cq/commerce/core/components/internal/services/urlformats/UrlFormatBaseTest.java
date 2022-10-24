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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class UrlFormatBaseTest {

    @Test
    public void testGetUrlKeyReturnsUrlKeyFirst() {
        assertEquals("url_key", UrlFormatBase.getUrlKey("url_path", "url_key"));
    }

    @Test
    public void testGetUrlKeyReturnsUrlPathIfNoUrlKey() {
        assertEquals("url_path", UrlFormatBase.getUrlKey("url_path", null));
    }

    @Test
    public void testGetUrlKeyReturnsLastUrlPathSegmentIfNoUrlKey() {
        assertEquals("url_path", UrlFormatBase.getUrlKey("foo/bar/url_path", null));
    }

    @Test
    public void testGetUrlKeyReturnsNull() {
        assertNull(UrlFormatBase.getUrlKey(null, null));
    }

    @Test
    public void testRemoveJcrContentReturnsNull() {
        assertNull(UrlFormatBase.removeJcrContent(null));
    }

    @Test
    public void testRemoveJcrContentReturnsParentOfJcrContentResource() {
        assertEquals("/parent/path", UrlFormatBase.removeJcrContent("/parent/path/jcr:content"));
    }

    @Test
    public void testRemoveJcrContentReturnsPathIfNotJcrContentNode() {
        assertEquals("/parent/path/jcr:content/child/path", UrlFormatBase.removeJcrContent("/parent/path/jcr:content/child/path"));
        assertEquals("/parent/path", UrlFormatBase.removeJcrContent("/parent/path/"));
    }

    @Test
    public void testSelectUrlPathReturnsUrlPathIfKnown() {
        assertEquals(
            "foobar/top",
            UrlFormatBase.selectUrlPath(
                "foobar/top",
                Arrays.asList("foobar", "foobar/top"),
                "top"));
    }

    @Test
    public void testSelectUrlPathReturnsFirstLongestOption() {
        assertEquals(
            "top/2nd/urlKey",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("top", "top/urlKey", "top/2nd/urlKey", "other", "other/urlKey", "other/2nd/urlKey"),
                "urlKey"));
    }

    @Test
    public void testSelectUrlPathReturnsUrlPathInContext() {
        // prefix match, no context url key
        assertEquals(
            "bar/foo/top",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("foobar", "foobar/top", "bar", "bar/top", "bar/foo/top"),
                "top",
                null,
                "bar/foo"));
    }

    @Test
    public void testSelectUrlPathReturnsUrlPathNoContext() {
        // no match, neither contextUrlPath nor contextUrlKey
        assertEquals(
            "foobar/asdf/top",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("foobar", "foobar/top", "foobar/asdf/top", "bar", "bar/top", "bar/foo/top"),
                "top",
                null,
                "another-bar"));
    }

    @Test
    public void testSelectUrlPathReturnsUrlPathOutOfContext() {
        // prefix match partial, no context url key
        assertEquals(
            "bar/foo/top",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("foobar", "foobar/top", "bar", "bar/top", "bar/foo/top"),
                "top",
                null,
                "bar/another-foo"));
    }

    @Test
    public void testSelectUrlPathReturnsUrlPathInContextWithUrlKey() {
        // no prefix match, but url key matches
        assertEquals(
            "foobar/bar/top",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("top", "foobar/top", "foobar/foo/top", "foobar/bar/top"),
                "top",
                "bar",
                null));
    }

    @Test
    public void testSelectUrlPathReturnsUrlKeyIfNoMatch() {
        assertEquals(
            "noKey",
            UrlFormatBase.selectUrlPath(
                null,
                Arrays.asList("top", "top/urlKey", "top/2nd/urlKey"),
                "noKey"));
    }

    @Test
    public void testSelectUrlPathDoesNotThrowOnNullAlternative() {
        assertEquals(
            "urlKey",
            UrlFormatBase.selectUrlPath(
                null,
                Collections.singletonList(null),
                "urlKey"));
    }
}
