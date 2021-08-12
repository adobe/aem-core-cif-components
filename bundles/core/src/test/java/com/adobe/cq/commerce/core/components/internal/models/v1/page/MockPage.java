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
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.mockito.Mockito;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.Page;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = Page.class,
    resourceType = "mock")
public class MockPage implements Page {

    private final Page delegate;

    public MockPage(SlingHttpServletRequest request) {
        Page mock = (Page) request.getAttribute(MockPage.class.getName());
        delegate = mock != null ? mock : Mockito.mock(Page.class);
    }

    @Override
    public String getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public Calendar getLastModifiedDate() {
        return delegate.getLastModifiedDate();
    }

    @Override
    @JsonIgnore
    public String[] getKeywords() {
        return delegate.getKeywords();
    }

    @Override
    public String getDesignPath() {
        return delegate.getDesignPath();
    }

    @Override
    public String getStaticDesignPath() {
        return delegate.getStaticDesignPath();
    }

    @Override
    @Deprecated
    public Map<String, String> getFavicons() {
        return delegate.getFavicons();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public String getBrandSlug() {
        return delegate.getBrandSlug();
    }

    @Override
    @JsonIgnore
    public String[] getClientLibCategories() {
        return delegate.getClientLibCategories();
    }

    @Override
    @JsonIgnore
    public String[] getClientLibCategoriesJsBody() {
        return delegate.getClientLibCategoriesJsBody();
    }

    @Override
    @JsonIgnore
    public String[] getClientLibCategoriesJsHead() {
        return delegate.getClientLibCategoriesJsHead();
    }

    @Override
    public String getTemplateName() {
        return delegate.getTemplateName();
    }

    @Override
    public String getAppResourcesPath() {
        return delegate.getAppResourcesPath();
    }

    @Override
    public String getCssClassNames() {
        return delegate.getCssClassNames();
    }

    @Override
    public NavigationItem getRedirectTarget() {
        return delegate.getRedirectTarget();
    }

    @Override
    public boolean hasCloudconfigSupport() {
        return delegate.hasCloudconfigSupport();
    }

    @Override
    public Set<String> getComponentsResourceTypes() {
        return delegate.getComponentsResourceTypes();
    }

    @Override
    public String[] getExportedItemsOrder() {
        return delegate.getExportedItemsOrder();
    }

    @Override
    public Map<String, ? extends ComponentExporter> getExportedItems() {
        return delegate.getExportedItems();
    }

    @Override
    public String getExportedType() {
        return delegate.getExportedType();
    }

    @Override
    public String getMainContentSelector() {
        return delegate.getMainContentSelector();
    }

    @Override
    public List<HtmlPageItem> getHtmlPageItems() {
        return delegate.getHtmlPageItems();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public ComponentData getData() {
        return delegate.getData();
    }

    @Override
    @JsonProperty("appliedCssClassNames")
    public String getAppliedCssClasses() {
        return delegate.getAppliedCssClasses();
    }
}
