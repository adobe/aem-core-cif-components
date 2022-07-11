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

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.Page;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.adobe.cq.wcm.core.components.models.datalayer.builder.DataLayerBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;

abstract class AbstractPageDelegator implements Page {

    protected abstract Page getDelegate();

    @Override
    public String getLanguage() {
        return getDelegate().getLanguage();
    }

    @Override
    public Calendar getLastModifiedDate() {
        return getDelegate().getLastModifiedDate();
    }

    @Override
    public String[] getKeywords() {
        return getDelegate().getKeywords();
    }

    @Override
    public String getDesignPath() {
        return getDelegate().getDesignPath();
    }

    @Override
    public String getStaticDesignPath() {
        return getDelegate().getStaticDesignPath();
    }

    @Override
    @Deprecated
    public Map<String, String> getFavicons() {
        return getDelegate().getFavicons();
    }

    @Override
    public String getTitle() {
        return getDelegate().getTitle();
    }

    @Override
    public String getBrandSlug() {
        return getDelegate().getBrandSlug();
    }

    @Override
    public String[] getClientLibCategories() {
        return getDelegate().getClientLibCategories();
    }

    @Override
    public String[] getClientLibCategoriesJsBody() {
        return getDelegate().getClientLibCategoriesJsBody();
    }

    @Override
    public String[] getClientLibCategoriesJsHead() {
        return getDelegate().getClientLibCategoriesJsHead();
    }

    @Override
    public String getTemplateName() {
        return getDelegate().getTemplateName();
    }

    @Override
    public String getAppResourcesPath() {
        return getDelegate().getAppResourcesPath();
    }

    @Override
    public String getCssClassNames() {
        return getDelegate().getCssClassNames();
    }

    @Override
    public NavigationItem getRedirectTarget() {
        return getDelegate().getRedirectTarget();
    }

    @Override
    public boolean hasCloudconfigSupport() {
        return getDelegate().hasCloudconfigSupport();
    }

    @Override
    public Set<String> getComponentsResourceTypes() {
        return getDelegate().getComponentsResourceTypes();
    }

    @Override
    public String[] getExportedItemsOrder() {
        return getDelegate().getExportedItemsOrder();
    }

    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        return getDelegate().getExportedItems();
    }

    @Override
    public String getExportedType() {
        return getDelegate().getExportedType();
    }

    @Override
    public String getMainContentSelector() {
        return getDelegate().getMainContentSelector();
    }

    @Override
    public List<HtmlPageItem> getHtmlPageItems() {
        return getDelegate().getHtmlPageItems();
    }

    @Override
    public String getId() {
        return getDelegate().getId();
    }

    @Override
    public ComponentData getData() {
        ComponentData data = getDelegate().getData();
        if (data == null) {
            return null;
        }

        return DataLayerBuilder.extending(data).asPage().withType(this::getExportedType).build();
    }

    @Override
    public String getAppliedCssClasses() {
        return getDelegate().getAppliedCssClasses();
    }

    @Override
    public String getDescription() {
        return getDelegate().getDescription();
    }

    @Override
    @JsonIgnore
    public String getCanonicalLink() {
        return getDelegate().getCanonicalLink();
    }

    @Override
    @JsonIgnore
    public Map<Locale, String> getAlternateLanguageLinks() {
        return getDelegate().getAlternateLanguageLinks();
    }

    @Override
    @JsonIgnore
    public List<String> getRobotsTags() {
        return getDelegate().getRobotsTags();
    }
}
