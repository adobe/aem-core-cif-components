/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
package com.adobe.cq.commerce.core.components.internal.models.v1.categorylist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.day.cq.wcm.api.Page;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FeaturedCateogoryListImplTest {

    FeaturedCateogoryListImpl slingModel;
    private static final String TEST_PRODUCT_PAGE_URL = "/content/test-category-page";
    private CategoryTree queryCategory;
    List<CategoryInterface> categories = new ArrayList<CategoryInterface>();

    @Before
    public void setup() throws Exception {
        this.slingModel = new FeaturedCateogoryListImpl();
        String json = IOUtils.toString(this.getClass()
            .getResourceAsStream("/graphql/magento-graphql-singlecategory-result.json"), StandardCharsets.UTF_8);
        Query rootQuery = QueryDeserializer.getGson().fromJson(json, Query.class);
        CategoryTree category = rootQuery.getCategory();
        category.setPath(TEST_PRODUCT_PAGE_URL + ".3.html");
        this.queryCategory = category;
        this.categories.add(category);
        Whitebox.setInternalState(this.slingModel, "categories", categories);
        Page categoryPage = mock(Page.class);
        when(categoryPage.getLanguage(false)).thenReturn(Locale.US);
        when(categoryPage.getPath()).thenReturn("/content/test-category-page");
        Whitebox.setInternalState(this.slingModel, "categoryPage", categoryPage);
    }

    @Test
    public void verifyCategory() {
        Assert.assertNotNull(categories);
        Assert.assertEquals(categories.get(0).getName(), slingModel.getCategories().get(0).getName());
        Assert.assertEquals(categories.get(0).getImage(), slingModel.getCategories().get(0).getImage());
        // Assert.assertEquals(categories.get(0).getPath(), slingModel.getCategories().get(0).getImage());
        Assert.assertEquals(String.format("%s.%s.html", TEST_PRODUCT_PAGE_URL, categories.get(0).getId()), slingModel.getCategories().get(0)
            .getPath());

    }
}
