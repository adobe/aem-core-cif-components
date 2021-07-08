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

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;

public class AbstractUrlFormatTest {

    @Test
    public void testGetUrlKeyReturnsUrlKeyFirst() {
        assertEquals("url_key", AbstractUrlFormat.getUrlKey(ImmutableMap.of(
            "url_key", "url_key",
            "url_path", "url_path")));
    }

    @Test
    public void testGetUrlKeyReturnsUrlPathIfNoUrlKey() {
        assertEquals("url_path", AbstractUrlFormat.getUrlKey(ImmutableMap.of(
            "url_path", "url_path")));
    }

    @Test
    public void testGetUrlKeyReturnsLastUrlPathSegmentIfNoUrlKey() {
        assertEquals("url_path", AbstractUrlFormat.getUrlKey(ImmutableMap.of(
            "url_path", "foo/bar/url_path")));
    }
}
