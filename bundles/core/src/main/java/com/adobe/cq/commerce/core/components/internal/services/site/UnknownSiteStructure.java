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
package com.adobe.cq.commerce.core.components.internal.services.site;

import java.util.Collections;
import java.util.List;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.day.cq.wcm.api.Page;

public class UnknownSiteStructure implements SiteStructure {

    public static final SiteStructure INSTANCE = new UnknownSiteStructure();

    private UnknownSiteStructure() {
        super();
    }

    @Override
    public Entry getLandingPage() {
        return null;
    }

    @Override
    public Entry getSearchResultsPage() {
        return null;
    }

    @Override
    public List<Entry> getProductPages() {
        return Collections.emptyList();
    }

    @Override
    public List<Entry> getCategoryPages() {
        return Collections.emptyList();
    }

    @Override
    public Entry getEntry(Page page) {
        return null;
    }

    @Override
    public boolean isCatalogPage(Page page) {
        return false;
    }

    @Override
    public boolean isProductPage(Page page) {
        return false;
    }

    @Override
    public boolean isCategoryPage(Page page) {
        return false;
    }
}
