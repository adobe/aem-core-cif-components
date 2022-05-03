/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.granite.license.ProductInfoProvider;
import com.adobe.granite.resourcestatus.ResourceStatus;
import com.adobe.granite.resourcestatus.ResourceStatusProvider;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.commons.status.EditorResourceStatus;

@Component(service = ResourceStatusProvider.class)
public class CatalogPageResourceStatusProvider implements ResourceStatusProvider {

    @Reference
    private PageManagerFactory pageManagerFactory;

    @Reference(
        target = "(name=cif)",
        cardinality = ReferenceCardinality.OPTIONAL,
        policy = ReferencePolicy.STATIC,
        policyOption = ReferencePolicyOption.GREEDY)
    private ProductInfoProvider cifProductInfo;

    private boolean actionsSupported;

    @Activate
    protected void activate() {
        // the action handlers require at least version 2022.04.28.1 of the Commerce AddOn
        actionsSupported = cifProductInfo != null
            && cifProductInfo.getProductInfo().getVersion().compareTo(new Version("2022.04.28.1")) >= 0;
    }

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
        EditorResourceStatus.Builder builder;

        if (isProductPage) {
            builder = new EditorResourceStatus.Builder(
                getType(),
                page.getTitle(),
                "Editing this page may affect many product pages.");
        } else if (isCategoryPage) {
            builder = new EditorResourceStatus.Builder(
                getType(),
                page.getTitle(),
                "Editing this page may affect many category pages.");
        } else {
            return Collections.emptyList();
        }

        builder.setVariant(EditorResourceStatus.Variant.WARNING);

        if (actionsSupported) {
            builder
                .addAction("open-template-page", "Open")
                .addData("template-page-path", page.getPath());
        }

        return Collections.singletonList(builder.build());
    }
}
