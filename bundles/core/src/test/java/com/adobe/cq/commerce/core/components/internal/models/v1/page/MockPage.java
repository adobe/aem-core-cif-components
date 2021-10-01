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
import org.mockito.Mockito;

import com.adobe.cq.wcm.core.components.models.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Page.class,
    resourceType = "mock")
public class MockPage extends AbstractPageDelegator {

    private final Page delegate;

    public MockPage(SlingHttpServletRequest request) {
        Page mock = (Page) request.getAttribute(MockPage.class.getName());
        delegate = mock != null ? mock : Mockito.mock(Page.class);
    }

    @Override
    protected Page getDelegate() {
        return delegate;
    }
}
