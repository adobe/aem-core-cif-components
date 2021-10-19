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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

import com.adobe.cq.commerce.core.components.models.page.PageMetadata;

import static org.mockito.Mockito.mock;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = PageMetadata.class,
    resourceType = "mock")
public class MockPageMetadata implements PageMetadata {

    private final PageMetadata delegate;

    public MockPageMetadata(SlingHttpServletRequest request) {
        PageMetadata mock = (PageMetadata) request.getAttribute(MockPageMetadata.class.getName());
        delegate = mock != null ? mock : mock(PageMetadata.class);
    }

    @Override
    public String getMetaDescription() {
        return delegate.getMetaDescription();
    }

    @Override
    public String getMetaKeywords() {
        return delegate.getMetaKeywords();
    }

    @Override
    public String getMetaTitle() {
        return delegate.getMetaTitle();
    }

    @Override
    public String getCanonicalUrl() {
        return delegate.getCanonicalUrl();
    }
}
