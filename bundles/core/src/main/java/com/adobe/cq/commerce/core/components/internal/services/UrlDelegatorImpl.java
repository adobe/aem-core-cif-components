/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.core.components.internal.services;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.components.services.UrlDelegator;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.day.cq.wcm.api.Page;

@Component(
    immediate = true,
    service = UrlDelegator.class)
public class UrlDelegatorImpl implements UrlDelegator, UrlProvider {

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private List<UrlProvider> urlProviderList;

    private UrlProvider getUrlProvider(SlingHttpServletRequest request) {
        for (UrlProvider provider : urlProviderList) {
            if (provider.shouldProcess(request)) {
                return provider;
            }
        }
        return null;
    }

    @Override
    public String toProductUrl(SlingHttpServletRequest request, @Nullable Page page, Map<String, String> params) {
        UrlProvider provider = getUrlProvider(request);
        return provider.toProductUrl(request, page, params);
    }

    @Override
    public String toCategoryUrl(SlingHttpServletRequest request, Page page, Map<String, String> params) {
        return getUrlProvider(request).toCategoryUrl(request, page, params);
    }

    @Override
    public Pair<ProductIdentifierType, String> getProductIdentifier(SlingHttpServletRequest request) {
        return getUrlProvider(request).getProductIdentifier(request);
    }

    @Override
    public Pair<CategoryIdentifierType, String> getCategoryIdentifier(SlingHttpServletRequest request) {
        return getUrlProvider(request).getCategoryIdentifier(request);
    }

    @Override
    public boolean shouldProcess(SlingHttpServletRequest request) {
        // this will never be in the urlProvider list, and should never be used
        return false;
    }
}
