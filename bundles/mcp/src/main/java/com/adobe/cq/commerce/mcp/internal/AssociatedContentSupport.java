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
package com.adobe.cq.commerce.mcp.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.cif.common.associatedcontent.AssociatedContentQuery;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.AssetParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.CfParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.PageParams;
import com.adobe.cq.cif.common.associatedcontent.AssociatedContentService.XfParams;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.dam.cfm.ContentFragment;
import com.adobe.cq.dam.cfm.ContentFragmentManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Shared helpers for MCP tools that resolve AEM content associated with commerce identifiers.
 */
public final class AssociatedContentSupport {

    private static final String XF_ROOT_PREFIX = "/content/experience-fragments/";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private AssociatedContentSupport() {}

    public static int limit(JsonNode args) {
        return Math.min(Math.max(args.path("limit").asInt(DEFAULT_LIMIT), 1), MAX_LIMIT);
    }

    public static ObjectNode buildProductResult(AssociatedContentService service, ObjectMapper mapper,
        ResourceResolver resolver, Page landingPage, String sku, String fragmentLocation, String contentFragmentModel,
        String linkElement, int limit) {
        ObjectNode out = mapper.createObjectNode();
        out.put("sku", sku);

        ArrayNode experienceFragments = out.putArray("experienceFragments");
        XfParams xfParams = XfParams.of(sku).path(getExperienceFragmentsRoot(landingPage));
        if (StringUtils.isNotBlank(fragmentLocation)) {
            xfParams = xfParams.location(fragmentLocation);
        }
        appendExperienceFragments(service.listProductExperienceFragments(resolver, xfParams).withLimit(limit), mapper,
            experienceFragments);

        ArrayNode contentFragments = out.putArray("contentFragments");
        ArrayNode assets = out.putArray("assets");
        Set<String> assetPaths = new HashSet<>();
        CfParams cfParams = CfParams.of(sku);
        if (StringUtils.isNotBlank(contentFragmentModel)) {
            cfParams = cfParams.model(contentFragmentModel);
        }
        if (StringUtils.isNotBlank(linkElement)) {
            cfParams = cfParams.property(linkElement);
        }
        appendContentFragments(mapper, resolver, contentFragmentModel,
            service.listProductContentFragments(resolver, cfParams).withLimit(limit), contentFragments, assets,
            assetPaths);
        appendAssets(mapper, service.listProductAssets(resolver, AssetParams.of(sku)).withLimit(limit), assets,
            assetPaths);

        ArrayNode contentPages = out.putArray("contentPages");
        appendContentPages(mapper,
            service.listProductContentPages(resolver, PageParams.of(sku).path(landingPage.getPath())).withLimit(limit),
            contentPages);
        return out;
    }

    public static ObjectNode buildCategoryResult(AssociatedContentService service, ObjectMapper mapper,
        ResourceResolver resolver, Page landingPage, String categoryUid, String fragmentLocation,
        String contentFragmentModel, String linkElement, int limit) {
        ObjectNode out = mapper.createObjectNode();
        out.put("categoryUid", categoryUid);

        ArrayNode experienceFragments = out.putArray("experienceFragments");
        XfParams xfParams = XfParams.of(categoryUid).path(getExperienceFragmentsRoot(landingPage));
        if (StringUtils.isNotBlank(fragmentLocation)) {
            xfParams = xfParams.location(fragmentLocation);
        }
        appendExperienceFragments(service.listCategoryExperienceFragments(resolver, xfParams).withLimit(limit), mapper,
            experienceFragments);

        ArrayNode contentFragments = out.putArray("contentFragments");
        ArrayNode assets = out.putArray("assets");
        Set<String> assetPaths = new HashSet<>();
        CfParams cfParams = CfParams.of(categoryUid);
        if (StringUtils.isNotBlank(contentFragmentModel)) {
            cfParams = cfParams.model(contentFragmentModel);
        }
        if (StringUtils.isNotBlank(linkElement)) {
            cfParams = cfParams.property(linkElement);
        }
        appendContentFragments(mapper, resolver, contentFragmentModel,
            service.listCategoryContentFragments(resolver, cfParams).withLimit(limit), contentFragments, assets,
            assetPaths);

        ArrayNode contentPages = out.putArray("contentPages");
        appendContentPages(mapper,
            service.listCategoryContentPages(resolver, PageParams.of(categoryUid).path(landingPage.getPath()))
                .withLimit(limit),
            contentPages);
        return out;
    }

    /**
     * Resolves the single content fragment matching {@code identifier} via the same {@code linkElement} lookup
     * {@link #buildProductResult}/{@link #buildCategoryResult} use, taking the first hit ({@code withLimit(1)}).
     *
     * @param service the associated content service to query
     * @param resolver the resolver to query with
     * @param isCategory {@code true} to use the category content-fragment listing, {@code false} for product
     * @param identifier the SKU or category UID to match
     * @param contentFragmentModel optional CF model path to scope the match; {@code null}/blank to not scope
     * @param linkElement optional model field name holding the identifier; {@code null}/blank to not scope
     * @return the first matching content fragment, or {@code null} if none matched
     */
    public static ContentFragment resolveSingleContentFragment(AssociatedContentService service,
        ResourceResolver resolver, boolean isCategory, String identifier, String contentFragmentModel,
        String linkElement) {
        CfParams cfParams = CfParams.of(identifier);
        if (StringUtils.isNotBlank(contentFragmentModel)) {
            cfParams = cfParams.model(contentFragmentModel);
        }
        if (StringUtils.isNotBlank(linkElement)) {
            cfParams = cfParams.property(linkElement);
        }
        AssociatedContentQuery<ContentFragment> query = isCategory
            ? service.listCategoryContentFragments(resolver, cfParams)
            : service.listProductContentFragments(resolver, cfParams);
        Iterator<ContentFragment> results = query.withLimit(1).execute();
        return results.hasNext() ? results.next() : null;
    }

    static String getExperienceFragmentsRoot(Page landingPage) {
        String landingPath = landingPage.getPath();
        if (landingPath.startsWith("/content/")) {
            return landingPath.replace("/content/", XF_ROOT_PREFIX);
        }
        return XF_ROOT_PREFIX;
    }

    private static void appendExperienceFragments(AssociatedContentQuery<Page> query, ObjectMapper mapper,
        ArrayNode target) {
        Iterator<Page> results = query.execute();
        while (results.hasNext()) {
            target.add(toExperienceFragment(mapper, results.next()));
        }
    }

    private static void appendContentPages(ObjectMapper mapper, AssociatedContentQuery<Page> query, ArrayNode target) {
        Iterator<Page> results = query.execute();
        while (results.hasNext()) {
            target.add(toContentPage(mapper, results.next()));
        }
    }

    private static void appendContentFragments(ObjectMapper mapper, ResourceResolver resolver, String modelPath,
        AssociatedContentQuery<ContentFragment> query, ArrayNode contentFragments, ArrayNode assets,
        Set<String> assetPaths) {
        Iterator<ContentFragment> results = query.execute();
        ContentFragmentManager fragmentManager = resolver.adaptTo(ContentFragmentManager.class);
        while (results.hasNext()) {
            ContentFragment fragment = results.next();
            contentFragments.add(toContentFragment(mapper, fragment, modelPath));
            if (fragmentManager != null) {
                for (Resource assetResource : fragmentManager.resolveAssociatedContentFlat(fragment)) {
                    if (assetResource != null && assetPaths.add(assetResource.getPath())) {
                        assets.add(toAsset(mapper, assetResource));
                    }
                }
            }
        }
    }

    private static void appendAssets(ObjectMapper mapper, AssociatedContentQuery<Asset> query, ArrayNode assets,
        Set<String> assetPaths) {
        Iterator<Asset> results = query.execute();
        while (results.hasNext()) {
            Asset asset = results.next();
            if (asset != null && assetPaths.add(asset.getPath())) {
                Resource assetResource = asset.adaptTo(Resource.class);
                if (assetResource != null) {
                    assets.add(toAsset(mapper, assetResource));
                }
            }
        }
    }

    private static ObjectNode toExperienceFragment(ObjectMapper mapper, Page xfVariationPage) {
        ObjectNode node = mapper.createObjectNode();
        node.put("pagePath", xfVariationPage.getPath());
        node.put("title", xfVariationPage.getTitle());
        Resource content = xfVariationPage.getContentResource();
        if (content != null) {
            node.put("variationPath", content.getPath());
            ValueMap properties = content.getValueMap();
            if (properties.containsKey(CommerceExperienceFragment.PN_FRAGMENT_LOCATION)) {
                node.put("fragmentLocation", properties.get(CommerceExperienceFragment.PN_FRAGMENT_LOCATION, ""));
            }
            if (properties.containsKey(CommerceExperienceFragment.PN_CQ_PRODUCTS)) {
                node.put("products", properties.get(CommerceExperienceFragment.PN_CQ_PRODUCTS, ""));
            }
            if (properties.containsKey(CommerceExperienceFragment.PN_CQ_CATEGORIES)) {
                node.put("categories", properties.get(CommerceExperienceFragment.PN_CQ_CATEGORIES, ""));
            }
        }
        return node;
    }

    private static ObjectNode toContentFragment(ObjectMapper mapper, ContentFragment fragment, String modelPath) {
        ObjectNode node = mapper.createObjectNode();
        Resource resource = fragment.adaptTo(Resource.class);
        if (resource != null) {
            node.put("path", resource.getPath());
            if (StringUtils.isBlank(modelPath)) {
                Resource data = resource.getChild("jcr:content/data");
                if (data != null) {
                    String resolvedModel = data.getValueMap().get("cq:model", "");
                    if (StringUtils.isNotBlank(resolvedModel)) {
                        modelPath = resolvedModel;
                    }
                }
            }
        }
        node.put("name", fragment.getName());
        node.put("title", fragment.getTitle());
        if (StringUtils.isNotBlank(modelPath)) {
            node.put("modelPath", modelPath);
        }
        return node;
    }

    private static ObjectNode toContentPage(ObjectMapper mapper, Page page) {
        ObjectNode node = mapper.createObjectNode();
        node.put("path", page.getPath());
        node.put("title", page.getTitle());
        return node;
    }

    private static ObjectNode toAsset(ObjectMapper mapper, Resource assetResource) {
        ObjectNode node = mapper.createObjectNode();
        node.put("path", assetResource.getPath());
        ValueMap properties = assetResource.getValueMap();
        node.put("title", properties.get("jcr:content/jcr:title", properties.get("jcr:title", "")));
        Resource jcrContent = assetResource.getChild("jcr:content");
        if (jcrContent != null) {
            node.put("mimeType", jcrContent.getValueMap().get("jcr:mimeType", ""));
        } else if (properties.containsKey(DamConstants.DC_FORMAT)) {
            node.put("mimeType", properties.get(DamConstants.DC_FORMAT, ""));
        }
        return node;
    }
}
