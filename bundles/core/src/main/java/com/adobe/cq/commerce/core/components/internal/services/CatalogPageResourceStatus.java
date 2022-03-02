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
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.granite.resourcestatus.ResourceStatus;
import com.adobe.granite.resourcestatus.ResourceStatusProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.commons.status.EditorResourceStatus;

@Component(service = ResourceStatusProvider.class)
public class CatalogPageResourceStatus implements ResourceStatusProvider {

    @Reference
    private PageManagerFactory pageManagerFactory;

    @NotNull
    @Override
    public String getType() {
        return "catalog-page";
    }

    @Nullable
    @Override
    public List<ResourceStatus> getStatuses(Resource resource) {
        PageManager pageManager = pageManagerFactory.getPageManager(resource.getResourceResolver());
        Page page = pageManager.getPage(resource.getPath());

        if (page == null) {
            page = pageManager.getContainingPage(resource);
        }

        if (page == null) {
            return Collections.emptyList();
        }

        boolean isProductPage = SiteNavigation.isProductPage(page);
        boolean isCategoryPage = SiteNavigation.isCategoryPage(page);

        if (isProductPage) {
            return Collections.singletonList(new EditorResourceStatus.Builder(
                getType(),
                page.getTitle(),
                "Editing this page will affect all the product pages using it.")
                    .setVariant(EditorResourceStatus.Variant.WARNING)
                    .addAction("create-specific-product-page", "Create Specific Page")
                    .setPriority(1500)
                    .build());
        }

        if (isCategoryPage) {
            return Collections.singletonList(new EditorResourceStatus.Builder(
                getType(),
                page.getTitle(),
                "Editing this page will affect all the category pages using it.")
                    .setVariant(EditorResourceStatus.Variant.WARNING)
                    .addAction("create-specific-category-page", "Create Specific Page")
                    .setPriority(1500)
                    .build());
        }

        return Collections.emptyList();
    }
}
