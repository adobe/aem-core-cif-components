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
package com.adobe.cq.commerce.mcp.internal.tools;

import java.util.Collections;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.CfParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.PageParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.XfParams;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetCategoryAssociatedContentToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsAssociatedContentForCategoryUid() throws Exception {
        Page landingPage = mock(Page.class);
        when(landingPage.getPath()).thenReturn("/content/venia/us/en");

        Page xfPage = mock(Page.class);
        when(xfPage.getPath()).thenReturn("/content/experience-fragments/venia/us/en/tops-promo/master");
        when(xfPage.getTitle()).thenReturn("Tops Promo");
        when(xfPage.getContentResource()).thenReturn(null);

        Page contentPage = mock(Page.class);
        when(contentPage.getPath()).thenReturn("/content/venia/us/en/tops-landing");
        when(contentPage.getTitle()).thenReturn("Tops Landing");

        AssociatedContentService associatedContentService = mock(AssociatedContentService.class);

        AssociatedContentQuery<Page> xfQuery = mock(AssociatedContentQuery.class);
        when(xfQuery.withLimit(20)).thenReturn(xfQuery);
        when(xfQuery.execute()).thenReturn(Collections.singletonList(xfPage).iterator());
        when(associatedContentService.listCategoryExperienceFragments(any(), any(XfParams.class))).thenReturn(xfQuery);

        AssociatedContentQuery<Page> pageQuery = mock(AssociatedContentQuery.class);
        when(pageQuery.withLimit(20)).thenReturn(pageQuery);
        when(pageQuery.execute()).thenReturn(Collections.singletonList(contentPage).iterator());
        when(associatedContentService.listCategoryContentPages(any(), any(PageParams.class))).thenReturn(pageQuery);

        AssociatedContentQuery<ContentFragment> cfQuery = mock(AssociatedContentQuery.class);
        when(cfQuery.withLimit(20)).thenReturn(cfQuery);
        when(cfQuery.execute()).thenReturn(Collections.<ContentFragment>emptyList().iterator());
        when(associatedContentService.listCategoryContentFragments(any(), any(CfParams.class))).thenReturn(cfQuery);

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(mock(ResourceResolver.class));

        GetCategoryAssociatedContentTool tool = new GetCategoryAssociatedContentTool();
        tool.associatedContentService = associatedContentService;

        JsonNode out = tool.call(ctx, mapper.createObjectNode()
            .put("categoryUid", "MjA=")
            .put("contentFragmentModel", "/conf/venia/settings/dam/cfm/models/category")
            .put("linkElement", "categoryUid"));
        assertEquals("MjA=", out.get("categoryUid").asText());
        assertEquals("get_category_associated_content", tool.name());
        assertEquals(1, out.get("experienceFragments").size());
        assertEquals("Tops Promo", out.get("experienceFragments").get(0).get("title").asText());
        assertEquals(1, out.get("contentPages").size());
    }
}
