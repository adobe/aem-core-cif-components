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
 ******************************************************************************/
package com.adobe.cq.commerce.core.components.internal.models.v1.teaser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaser;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.day.cq.wcm.api.Page;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommerceTeaserImplTest {

    private CommerceTeaser slingModel;

    final private String productPath = "/content/test-product-page";
    final private String categoryPath = "/content/test-category-page";
    final private String currentPath = "/content/venia/teaserTest";

    @Before
    public void setup() {
        CommerceTeaserImpl cifTeaser = new CommerceTeaserImpl();

        Page productPage = mock(Page.class);
        Page categoryPage = mock(Page.class);
        Page currentPage = mock(Page.class);

        when(productPage.getPath()).thenReturn(productPath);
        when(categoryPage.getPath()).thenReturn(categoryPath);
        when(currentPage.getPath()).thenReturn(currentPath);

        List<Resource> actionResources = new ArrayList<>();

        // Action node 1 - productSlug configured, category left blank, text set to some value
        actionResources.add(getActionNodeResource("278", null, "My Product"));

        // Action node 2 - productSlug left blank, category configured, text set to some value
        actionResources.add(getActionNodeResource(null, "30", "My Category"));

        // Action node 3 - productSlug configured, category also configured, text set to some value
        actionResources.add(getActionNodeResource("278", "30", "My Category"));

        // Action node 4 - productSlug left blank, category also left blank, text set to some value
        actionResources.add(getActionNodeResource(null, null, "This Page"));

        Resource mockedResource = mock(Resource.class);
        Map<String, Object> actionProperties = new HashMap<>();

        actionProperties.put(CommerceTeaser.PN_ACTIONS_ENABLED, true);// Setting action enabled TRUE

        when(mockedResource.getValueMap()).thenReturn(new ValueMapDecorator(actionProperties));

        Resource mockedChildResource = mock(Resource.class);

        when(mockedChildResource.getChildren()).thenReturn(actionResources);
        when(mockedResource.getChild(CommerceTeaser.NN_ACTIONS)).thenReturn(mockedChildResource);

        Whitebox.setInternalState(cifTeaser, "resource", mockedResource);
        Whitebox.setInternalState(cifTeaser, "categoryPage", categoryPage);
        Whitebox.setInternalState(cifTeaser, "productPage", productPage);
        Whitebox.setInternalState(cifTeaser, "currentPage", currentPage);
        // Whitebox.setInternalState(cifTeaser, "actionsEnabled", true);

        cifTeaser.setActionsEnabled();
        cifTeaser.populateActions();
        this.slingModel = cifTeaser;

    }

    Resource getActionNodeResource(String productSlug, String categoryId, String text) {
        Resource actionResource = mock(Resource.class);

        Map<String, Object> actionProperties = new HashMap<>();

        actionProperties.put(CommerceTeaser.PN_ACTION_PRODUCT_SLUG, productSlug);
        actionProperties.put(CommerceTeaser.PN_ACTION_CATEGORY_ID, categoryId);
        actionProperties.put(CommerceTeaser.PN_ACTION_TEXT, text);

        ValueMapDecorator vMD = new ValueMapDecorator(actionProperties);

        when(actionResource.getValueMap()).thenReturn(vMD);

        return actionResource;
    }

    @Test
    public void verifyActions() {

        List<ListItem> actionItems = this.slingModel.getActions();

        Assert.assertTrue(this.slingModel.isActionsEnabled());
        Assert.assertTrue(actionItems.size() == 4);

        Assert.assertTrue(actionItems.get(0).getURL().equalsIgnoreCase(this.productPath + ".278.html"));
        Assert.assertTrue(actionItems.get(0).getTitle().equalsIgnoreCase("My Product"));

        Assert.assertTrue(actionItems.get(1).getURL().equalsIgnoreCase(this.categoryPath + ".30.html"));
        Assert.assertTrue(actionItems.get(1).getTitle().equalsIgnoreCase("My Category"));

        Assert.assertTrue(actionItems.get(2).getURL().equalsIgnoreCase(this.categoryPath + ".30.html"));
        Assert.assertTrue(actionItems.get(2).getTitle().equalsIgnoreCase("My Category"));

        Assert.assertTrue(actionItems.get(3).getURL().equalsIgnoreCase(this.currentPath + ".html"));
        Assert.assertTrue(actionItems.get(3).getTitle().equalsIgnoreCase("This Page"));

    }
}
