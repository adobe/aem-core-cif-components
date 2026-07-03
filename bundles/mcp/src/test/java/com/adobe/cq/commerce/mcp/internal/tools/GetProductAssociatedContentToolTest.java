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

import java.util.Arrays;
import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.AssetParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.CfParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.PageParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.XfParams;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetProductAssociatedContentToolTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void returnsAssociatedContentForSku() throws Exception {
        Page landingPage = mock(Page.class);
        when(landingPage.getPath()).thenReturn("/content/venia/us/en");

        Page xfPage = mock(Page.class);
        when(xfPage.getPath()).thenReturn("/content/experience-fragments/venia/us/en/promo/master");
        when(xfPage.getTitle()).thenReturn("Promo XF");
        when(xfPage.getContentResource()).thenReturn(null);

        Page contentPage = mock(Page.class);
        when(contentPage.getPath()).thenReturn("/content/venia/us/en/lookbook");
        when(contentPage.getTitle()).thenReturn("Lookbook");

        ContentFragment cf = mock(ContentFragment.class);
        when(cf.getName()).thenReturn("bellona-details");
        when(cf.getTitle()).thenReturn("Bellona Details");
        Resource cfResource = mock(Resource.class);
        when(cfResource.getPath()).thenReturn("/content/dam/venia/products/bellona-details");
        when(cf.adaptTo(Resource.class)).thenReturn(cfResource);

        Resource assetResource = mock(Resource.class);
        when(assetResource.getPath()).thenReturn("/content/dam/venia/products/bellona-hero.jpg");
        when(assetResource.getValueMap()).thenReturn(ValueMap.EMPTY);
        when(assetResource.getChild("jcr:content")).thenReturn(null);

        AssociatedContentService associatedContentService = mock(AssociatedContentService.class);

        AssociatedContentQuery<Page> xfQuery = mock(AssociatedContentQuery.class);
        when(xfQuery.withLimit(20)).thenReturn(xfQuery);
        when(xfQuery.execute()).thenReturn(Collections.singletonList(xfPage).iterator());
        when(associatedContentService.listProductExperienceFragments(any(), any(XfParams.class))).thenReturn(xfQuery);

        AssociatedContentQuery<Page> pageQuery = mock(AssociatedContentQuery.class);
        when(pageQuery.withLimit(20)).thenReturn(pageQuery);
        when(pageQuery.execute()).thenReturn(Collections.singletonList(contentPage).iterator());
        when(associatedContentService.listProductContentPages(any(), any(PageParams.class))).thenReturn(pageQuery);

        AssociatedContentQuery<ContentFragment> cfQuery = mock(AssociatedContentQuery.class);
        when(cfQuery.withLimit(20)).thenReturn(cfQuery);
        when(cfQuery.execute()).thenReturn(Collections.singletonList(cf).iterator());
        when(associatedContentService.listProductContentFragments(any(), any(CfParams.class))).thenReturn(cfQuery);

        AssociatedContentQuery<Asset> assetQuery = mock(AssociatedContentQuery.class);
        when(assetQuery.withLimit(20)).thenReturn(assetQuery);
        when(assetQuery.execute()).thenReturn(Collections.<Asset>emptyList().iterator());
        when(associatedContentService.listProductAssets(any(), any(AssetParams.class))).thenReturn(assetQuery);

        ContentFragmentManager fragmentManager = mock(ContentFragmentManager.class);
        when(fragmentManager.resolveAssociatedContentFlat(cf)).thenReturn(Arrays.asList(assetResource));

        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.adaptTo(ContentFragmentManager.class)).thenReturn(fragmentManager);

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(resolver);

        GetProductAssociatedContentTool tool = new GetProductAssociatedContentTool();
        tool.associatedContentService = associatedContentService;

        JsonNode args = mapper.createObjectNode()
            .put("sku", "VSK01")
            .put("contentFragmentModel", "/conf/venia/settings/dam/cfm/models/product")
            .put("linkElement", "sku");
        JsonNode out = tool.call(ctx, args);

        assertEquals("VSK01", out.get("sku").asText());
        assertEquals("get_product_associated_content", tool.name());
        assertEquals(1, out.get("experienceFragments").size());
        assertEquals("Promo XF", out.get("experienceFragments").get(0).get("title").asText());
        assertEquals(1, out.get("contentFragments").size());
        assertEquals("bellona-details", out.get("contentFragments").get(0).get("name").asText());
        assertEquals(1, out.get("contentPages").size());
        assertEquals("/content/venia/us/en/lookbook", out.get("contentPages").get(0).get("path").asText());
        assertEquals(1, out.get("assets").size());
        assertEquals("/content/dam/venia/products/bellona-hero.jpg", out.get("assets").get(0).get("path").asText());
    }

    @Test
    public void discoversContentFragmentsWithoutExplicitModel() throws Exception {
        ContentFragment cf = mock(ContentFragment.class);
        when(cf.getName()).thenReturn("vd09-marketing-data");
        when(cf.getTitle()).thenReturn("VD09 marketing data");
        Resource cfResource = mock(Resource.class);
        when(cfResource.getPath()).thenReturn("/content/dam/venia/products/vd09-marketing-data");
        when(cfResource.getChild("jcr:content/data")).thenReturn(null);
        when(cf.adaptTo(Resource.class)).thenReturn(cfResource);

        AssociatedContentService associatedContentService = mock(AssociatedContentService.class);
        AssociatedContentQuery<Page> emptyQuery = mock(AssociatedContentQuery.class);
        when(emptyQuery.withLimit(20)).thenReturn(emptyQuery);
        when(emptyQuery.execute()).thenReturn(Collections.<Page>emptyList().iterator());
        when(associatedContentService.listProductExperienceFragments(any(), any(XfParams.class))).thenReturn(emptyQuery);
        when(associatedContentService.listProductContentPages(any(), any(PageParams.class))).thenReturn(emptyQuery);

        AssociatedContentQuery<ContentFragment> cfQuery = mock(AssociatedContentQuery.class);
        when(cfQuery.withLimit(20)).thenReturn(cfQuery);
        when(cfQuery.execute()).thenReturn(Collections.singletonList(cf).iterator());
        when(associatedContentService.listProductContentFragments(any(), any(CfParams.class))).thenReturn(cfQuery);

        AssociatedContentQuery<Asset> assetQuery = mock(AssociatedContentQuery.class);
        when(assetQuery.withLimit(20)).thenReturn(assetQuery);
        when(assetQuery.execute()).thenReturn(Collections.<Asset>emptyList().iterator());
        when(associatedContentService.listProductAssets(any(), any(AssetParams.class))).thenReturn(assetQuery);

        ContentFragmentManager fragmentManager = mock(ContentFragmentManager.class);
        when(fragmentManager.resolveAssociatedContentFlat(cf)).thenReturn(Collections.emptyList());

        ResourceResolver resolver = mock(ResourceResolver.class);
        when(resolver.adaptTo(ContentFragmentManager.class)).thenReturn(fragmentManager);

        Page landingPage = mock(Page.class);
        when(landingPage.getPath()).thenReturn("/content/venia/us/en");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(resolver);

        GetProductAssociatedContentTool tool = new GetProductAssociatedContentTool();
        tool.associatedContentService = associatedContentService;

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "VD09"));
        assertEquals(1, out.get("contentFragments").size());
        assertEquals("vd09-marketing-data", out.get("contentFragments").get(0).get("name").asText());
        assertEquals(0, out.get("assets").size());
    }

    @Test
    public void includesDamAssetsLinkedByProductMetadata() throws Exception {
        Asset damAsset = mock(Asset.class);
        when(damAsset.getPath()).thenReturn("/content/dam/venia/landing_page_image3.jpg");
        Resource damResource = mock(Resource.class);
        when(damAsset.adaptTo(Resource.class)).thenReturn(damResource);
        when(damResource.getPath()).thenReturn("/content/dam/venia/landing_page_image3.jpg");
        when(damResource.getValueMap()).thenReturn(ValueMap.EMPTY);
        when(damResource.getChild("jcr:content")).thenReturn(null);

        AssociatedContentService associatedContentService = mock(AssociatedContentService.class);
        AssociatedContentQuery<Page> emptyQuery = mock(AssociatedContentQuery.class);
        when(emptyQuery.withLimit(20)).thenReturn(emptyQuery);
        when(emptyQuery.execute()).thenReturn(Collections.<Page>emptyList().iterator());
        when(associatedContentService.listProductExperienceFragments(any(), any(XfParams.class))).thenReturn(emptyQuery);
        when(associatedContentService.listProductContentPages(any(), any(PageParams.class))).thenReturn(emptyQuery);

        AssociatedContentQuery<ContentFragment> cfQuery = mock(AssociatedContentQuery.class);
        when(cfQuery.withLimit(20)).thenReturn(cfQuery);
        when(cfQuery.execute()).thenReturn(Collections.<ContentFragment>emptyList().iterator());
        when(associatedContentService.listProductContentFragments(any(), any(CfParams.class))).thenReturn(cfQuery);

        AssociatedContentQuery<Asset> assetQuery = mock(AssociatedContentQuery.class);
        when(assetQuery.withLimit(20)).thenReturn(assetQuery);
        when(assetQuery.execute()).thenReturn(Collections.singletonList(damAsset).iterator());
        when(associatedContentService.listProductAssets(any(), any(AssetParams.class))).thenReturn(assetQuery);

        Page landingPage = mock(Page.class);
        when(landingPage.getPath()).thenReturn("/content/venia/us/en");

        StoreContext ctx = mock(StoreContext.class);
        when(ctx.getLandingPage()).thenReturn(landingPage);
        when(ctx.getRequest()).thenReturn(mock(org.apache.sling.api.SlingHttpServletRequest.class));
        when(ctx.getRequest().getResourceResolver()).thenReturn(mock(ResourceResolver.class));

        GetProductAssociatedContentTool tool = new GetProductAssociatedContentTool();
        tool.associatedContentService = associatedContentService;

        JsonNode out = tool.call(ctx, mapper.createObjectNode().put("sku", "VD09"));
        assertEquals(1, out.get("assets").size());
        assertEquals("/content/dam/venia/landing_page_image3.jpg", out.get("assets").get(0).get("path").asText());
    }
}
