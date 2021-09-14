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
package com.adobe.cq.commerce.core.testing;

import org.apache.sling.api.SlingHttpServletRequest;

import com.adobe.cq.wcm.core.components.services.link.PathProcessor;

/**
 * @deprecated we should start using the WCM Core Components aem mocks plugin
 * @see https://github.com/adobe/aem-core-wcm-components/tree/master/testing/aem-mock-plugin
 */
@Deprecated
public class MockPathProcessor implements PathProcessor {
    @Override
    public boolean accepts(String s, SlingHttpServletRequest slingHttpServletRequest) {
        return true;
    }

    @Override
    public String sanitize(String s, SlingHttpServletRequest slingHttpServletRequest) {
        return s;
    }

    @Override
    public String map(String s, SlingHttpServletRequest slingHttpServletRequest) {
        return s;
    }

    @Override
    public String externalize(String s, SlingHttpServletRequest slingHttpServletRequest) {
        return s;
    }
}
