/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.core.components.internal.client;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Single configuration covering all forwarding of incoming request headers to the outbound Commerce GraphQL
 * request: a master switch, an arbitrary list of headers forwarded as-is, and a dedicated client IP section with
 * its own switch and fields (source header/pattern/outbound name), since the client IP needs more than a plain
 * name to be forwarded correctly. Every header forwarded through this configuration, generic or client IP, is
 * excluded from the GraphQL response cache key, since it carries per-request metadata that does not influence the
 * response. Headers on the internal denylist (Authorization, Cookie, Host, Content-Length, etc.) are never
 * forwarded, even if configured here.
 */
@ObjectClassDefinition(name = "CIF Forwarded Request Headers Configuration")
public @interface ForwardedHeadersConfig {

    @AttributeDefinition(
        name = "Enabled",
        description = "Master switch for all header forwarding configured below. Disable to turn everything off without "
            + "clearing the individual fields.")
    boolean enabled() default false;

    @AttributeDefinition(
        name = "Forwarded header names",
        description = "Names of incoming request headers whose current value should be forwarded as-is, under the same name, on "
            + "the outbound GraphQL request to Commerce (e.g. a tracing/correlation id header).")
    String[] forwardedHeaderNames() default {};

    @AttributeDefinition(
        name = "Enable client IP forwarding",
        description = "Forwards the end-user IP using the dedicated fields below, in addition to any generic headers above. Only "
            + "enable this once the CDN/dispatcher in front of AEM is confirmed to set the source header from the actual client "
            + "connection, not from an unvalidated client-supplied value.")
    boolean clientIpEnabled() default false;

    @AttributeDefinition(
        name = "Client IP header name",
        description = "Incoming request header set by the CDN/edge/dispatcher that carries the original client IP. Defaults to "
            + "the standard 'X-Forwarded-For', already populated by the AEMaaCS managed CDN and by common on-premise "
            + "dispatcher/reverse-proxy setups. Different CDNs may use a dedicated header instead, e.g. 'CF-Connecting-IP' "
            + "(Cloudflare), 'True-Client-IP' (Akamai), 'Fastly-Client-IP' (Fastly). The reserved value 'REMOTE_ADDR' reads the "
            + "direct TCP connection IP instead of a header: only correct when AEM is reached with no proxy/CDN/dispatcher in "
            + "between (e.g. local development), since behind any proxy this would instead resolve to that proxy's own IP.")
    String clientIpHeaderName() default "X-Forwarded-For";

    @AttributeDefinition(
        name = "Client IP outbound header name",
        description = "Header name used to forward the client IP on the outbound Commerce request, so it never collides with a "
            + "header name already used for another purpose between AEM and Commerce.")
    String clientIpOutboundHeaderName() default "X-Adobe-Client-IP";

    @AttributeDefinition(
        name = "Client IP value extraction pattern",
        description = "Regex with a single capturing group used to extract the client IP from the header value above. The "
            + "default pattern takes the leftmost token, which works for a plain single-IP header (e.g. 'CF-Connecting-IP') as "
            + "well as a multi-hop 'X-Forwarded-For' chain ('client, proxy1, proxy2'). Use 'for=\"?\\[?([0-9a-fA-F:.]+)' for the "
            + "standards-based 'Forwarded' header (RFC 7239), or '^([0-9a-fA-F:.]+):\\d+$' to strip a trailing port, e.g. "
            + "CloudFront's 'CloudFront-Viewer-Address'.")
    String clientIpHeaderValuePattern() default "^\\s*([0-9a-fA-F:.]+)";
}
