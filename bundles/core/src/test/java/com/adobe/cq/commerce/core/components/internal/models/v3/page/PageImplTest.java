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
package com.adobe.cq.commerce.core.components.internal.models.v3.page;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.junit.Before;

public class PageImplTest extends com.adobe.cq.commerce.core.components.internal.models.v2.page.PageImplTest {

    @Before
    public void setup() throws PersistenceException {
        super.setup();
        // set a mock resourceSuperType to inject the MockPage
        ModifiableValueMap pageContent = context.resourceResolver().getResource(pagePath + "/jcr:content")
            .adaptTo(ModifiableValueMap.class);
        pageContent.put("sling:resourceType", PageImpl.RESOURCE_TYPE);

        context.resourceResolver().commit();
    }
}
