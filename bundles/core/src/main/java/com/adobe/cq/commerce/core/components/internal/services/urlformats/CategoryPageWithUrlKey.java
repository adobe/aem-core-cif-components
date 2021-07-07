package com.adobe.cq.commerce.core.components.internal.services.urlformats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;

import com.adobe.cq.commerce.core.components.internal.services.UrlFormat;
import com.google.common.collect.Sets;

import static com.adobe.cq.commerce.core.components.services.UrlProvider.PAGE_PARAM;
import static com.adobe.cq.commerce.core.components.services.UrlProvider.URL_KEY_PARAM;

public class CategoryPageWithUrlKey extends AbstractUrlFormat {
    public static final UrlFormat INSTANCE = new CategoryPageWithUrlKey();
    public static final String PATTERN = "{{page}}.html/{{url_key}}.html";

    private CategoryPageWithUrlKey() {
        super();
    }

    @Override
    public String format(Map<String, String> parameters) {
        return parameters.getOrDefault(PAGE_PARAM, "{{" + PAGE_PARAM + "}}") + HTML_EXTENSION + "/" +
            parameters.getOrDefault(URL_KEY_PARAM, "{{" + URL_KEY_PARAM + "}}") + HTML_EXTENSION;
    }

    @Override
    public Map<String, String> parse(RequestPathInfo requestPathInfo) {
        if (requestPathInfo == null) {
            return Collections.emptyMap();
        }

        return new HashMap<String, String>() {
            {
                put(PAGE_PARAM, requestPathInfo.getResourcePath());
                String suffix = StringUtils.removeStart(StringUtils.removeEnd(requestPathInfo.getSuffix(), HTML_EXTENSION), "/");
                if (StringUtils.isNotBlank(suffix)) {
                    put(URL_KEY_PARAM, suffix);
                }
            }
        };
    }

    @Override
    public Set<String> getParameterNames() {
        return Sets.newHashSet(PAGE_PARAM, URL_KEY_PARAM);
    }
}
