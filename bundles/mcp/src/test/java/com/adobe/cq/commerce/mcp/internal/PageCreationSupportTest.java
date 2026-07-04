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
package com.adobe.cq.commerce.mcp.internal;

import org.apache.sling.api.resource.Resource;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PageCreationSupportTest {

    @Rule
    public final AemContext context = new AemContext();

    @Test
    public void returnsExistingParentUnderContent() {
        context.build().resource("/content/site/en", "jcr:primaryType", "cq:Page").commit();
        Resource parent = PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/content/site/en");
        assertEquals("/content/site/en", parent.getPath());
    }

    @Test
    public void rejectsBlankParent() {
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "  "));
    }

    @Test
    public void rejectsParentNotUnderContent() {
        context.build().resource("/conf/site/en", "jcr:primaryType", "cq:Page").commit();
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/conf/site/en"));
    }

    @Test
    public void rejectsMissingParent() {
        assertThrows(IllegalArgumentException.class,
            () -> PageCreationSupport.validatePageParent(context.resourceResolver(), "parent", "/content/does/not/exist"));
    }
}
