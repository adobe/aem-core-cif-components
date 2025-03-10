/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface DeniedHttpHeaders {
    /**
     * A list of HTTP headers that cannot be overridden when configuring a list of custom HTTP headers
     */
    Set<String> DENYLIST = DeniedHeadersProvider.getDeniedHeaderNames();
}

class DeniedHeadersProvider {
    static Set<String> getDeniedHeaderNames() {
        String[] headers = new String[] {
            org.apache.http.HttpHeaders.ACCEPT,
            org.apache.http.HttpHeaders.ACCEPT_CHARSET,
            org.apache.http.HttpHeaders.ACCEPT_ENCODING,
            org.apache.http.HttpHeaders.ACCEPT_LANGUAGE,
            org.apache.http.HttpHeaders.ACCEPT_RANGES,
            org.apache.http.HttpHeaders.AGE,
            org.apache.http.HttpHeaders.ALLOW,
            org.apache.http.HttpHeaders.AUTHORIZATION,
            org.apache.http.HttpHeaders.CACHE_CONTROL,
            org.apache.http.HttpHeaders.CONNECTION,
            org.apache.http.HttpHeaders.CONTENT_ENCODING,
            org.apache.http.HttpHeaders.CONTENT_LANGUAGE,
            org.apache.http.HttpHeaders.CONTENT_LENGTH,
            org.apache.http.HttpHeaders.CONTENT_LOCATION,
            org.apache.http.HttpHeaders.CONTENT_MD5,
            org.apache.http.HttpHeaders.CONTENT_RANGE,
            org.apache.http.HttpHeaders.CONTENT_TYPE,
            org.apache.http.HttpHeaders.DATE,
            org.apache.http.HttpHeaders.DAV,
            org.apache.http.HttpHeaders.DEPTH,
            org.apache.http.HttpHeaders.DESTINATION,
            org.apache.http.HttpHeaders.ETAG,
            org.apache.http.HttpHeaders.EXPECT,
            org.apache.http.HttpHeaders.EXPIRES,
            org.apache.http.HttpHeaders.FROM,
            org.apache.http.HttpHeaders.HOST,
            org.apache.http.HttpHeaders.IF,
            org.apache.http.HttpHeaders.IF_MATCH,
            org.apache.http.HttpHeaders.IF_MODIFIED_SINCE,
            org.apache.http.HttpHeaders.IF_NONE_MATCH,
            org.apache.http.HttpHeaders.IF_RANGE,
            org.apache.http.HttpHeaders.IF_UNMODIFIED_SINCE,
            org.apache.http.HttpHeaders.LAST_MODIFIED,
            org.apache.http.HttpHeaders.LOCATION,
            org.apache.http.HttpHeaders.LOCK_TOKEN,
            org.apache.http.HttpHeaders.MAX_FORWARDS,
            org.apache.http.HttpHeaders.OVERWRITE,
            org.apache.http.HttpHeaders.PRAGMA,
            org.apache.http.HttpHeaders.PROXY_AUTHENTICATE,
            org.apache.http.HttpHeaders.PROXY_AUTHORIZATION,
            org.apache.http.HttpHeaders.RANGE,
            org.apache.http.HttpHeaders.REFERER,
            org.apache.http.HttpHeaders.RETRY_AFTER,
            org.apache.http.HttpHeaders.SERVER,
            org.apache.http.HttpHeaders.STATUS_URI,
            org.apache.http.HttpHeaders.TE,
            org.apache.http.HttpHeaders.TIMEOUT,
            org.apache.http.HttpHeaders.TRAILER,
            org.apache.http.HttpHeaders.TRANSFER_ENCODING,
            org.apache.http.HttpHeaders.UPGRADE,
            org.apache.http.HttpHeaders.USER_AGENT,
            org.apache.http.HttpHeaders.VARY,
            org.apache.http.HttpHeaders.VIA,
            org.apache.http.HttpHeaders.WARNING,
            org.apache.http.HttpHeaders.WWW_AUTHENTICATE,
            "Store",
            "Preview-Version"
        };
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(headers)));
    }
}
