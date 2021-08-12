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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import com.adobe.cq.commerce.core.components.internal.models.v1.storeconfigexporter.StoreConfigExporterImpl;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.adobe.cq.wcm.core.components.models.Page;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { PageImpl.class, Page.class },
    resourceType = PageImpl.RESOURCE_TYPE)
public class PageImpl extends AbstractPageDelegator implements Page {

    public static final String RESOURCE_TYPE = "core/cif/components/structure/page/v1/page";

    @Self
    @Via(type = ResourceSuperType.class)
    private Page page;

    @Self
    private StoreConfigExporterImpl storeConfigExporter;

    private List<HtmlPageItem> htmlPageItems;

    @Override
    protected Page getDelegate() {
        return page;
    }

    @Override
    public List<HtmlPageItem> getHtmlPageItems() {
        if (htmlPageItems == null) {
            HtmlPageItem storeConfigItem = new StoreConfigHtmlPageItem(storeConfigExporter);
            List<HtmlPageItem> existingItems = page.getHtmlPageItems();

            if (existingItems == null || existingItems.isEmpty()) {
                htmlPageItems = Collections.singletonList(storeConfigItem);
            } else {
                htmlPageItems = new ArrayList<>(existingItems.size() + 1);
                htmlPageItems.addAll(existingItems);
                htmlPageItems.add(storeConfigItem);
            }
        }
        return htmlPageItems;
    }
}
