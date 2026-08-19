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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the {@link ForwardedHeadersConfig} in a form ready to use by {@link MagentoGraphqlClientImpl}: the client
 * IP pattern is pre-compiled once here rather than on every request, and an invalid pattern disables client IP
 * forwarding instead of failing GraphQL requests at runtime.
 */
@Component(service = ForwardedHeadersConfigService.class)
@Designate(ocd = ForwardedHeadersConfig.class)
public class ForwardedHeadersConfigService {

    /**
     * Reserved {@link ForwardedHeadersConfig#clientIpHeaderName()} value: read the direct TCP connection IP
     * instead of a header. Only correct with no proxy/CDN/dispatcher in front of AEM (e.g. local development).
     */
    static final String REMOTE_ADDR = "REMOTE_ADDR";

    private static final Logger LOGGER = LoggerFactory.getLogger(ForwardedHeadersConfigService.class);

    private boolean enabled;
    private Set<String> forwardedHeaderNames = Collections.emptySet();
    private boolean clientIpEnabled;
    private String clientIpHeaderName;
    private String clientIpOutboundHeaderName;
    private Pattern clientIpHeaderValuePattern;

    @Activate
    protected void activate(ForwardedHeadersConfig config) {
        this.enabled = config.enabled();

        String[] configuredNames = config.forwardedHeaderNames();
        this.forwardedHeaderNames = configuredNames != null && configuredNames.length > 0
            ? new LinkedHashSet<>(Arrays.asList(configuredNames))
            : Collections.emptySet();

        this.clientIpEnabled = config.clientIpEnabled();
        this.clientIpHeaderName = config.clientIpHeaderName();
        this.clientIpOutboundHeaderName = StringUtils.isNotBlank(config.clientIpOutboundHeaderName())
            ? config.clientIpOutboundHeaderName()
            : config.clientIpHeaderName();

        try {
            this.clientIpHeaderValuePattern = Pattern.compile(config.clientIpHeaderValuePattern());
        } catch (PatternSyntaxException e) {
            LOGGER.error("Invalid client IP header value pattern '{}', client IP forwarding is disabled",
                config.clientIpHeaderValuePattern(), e);
            this.clientIpEnabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getForwardedHeaderNames() {
        return forwardedHeaderNames;
    }

    public boolean isClientIpEnabled() {
        return clientIpEnabled;
    }

    public String getClientIpHeaderName() {
        return clientIpHeaderName;
    }

    public String getClientIpOutboundHeaderName() {
        return clientIpOutboundHeaderName;
    }

    public Pattern getClientIpHeaderValuePattern() {
        return clientIpHeaderValuePattern;
    }
}
