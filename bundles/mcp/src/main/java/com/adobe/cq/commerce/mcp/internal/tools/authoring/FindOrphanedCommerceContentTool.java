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
package com.adobe.cq.commerce.mcp.internal.tools.authoring;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.common.CombinedSku;
import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.mcp.McpCallContext;
import com.adobe.cq.commerce.mcp.McpTool;
import com.adobe.cq.commerce.mcp.internal.StoreContext;
import com.adobe.cq.commerce.mcp.internal.tools.McpCategoryRetriever;
import com.adobe.cq.commerce.mcp.internal.tools.McpProductRetriever;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * MCP read tool sweeping AEM content for CIF commerce reference tags ({@code cq:products}/{@code cq:categories} --
 * catalog §5) whose SKU/category-UID no longer resolves against the live catalog.
 * <p>
 * The AEM CIF SDK's {@code AssociatedContentService} is identifier-keyed only (no enumeration API), so it cannot
 * answer "what's tagged" without already knowing the identifier. This tool instead runs a reverse JCR-SQL2 scan
 * (via {@link #findTaggedContent}) for nodes that carry either tag property under a given root, then resolves each
 * tagged identifier the same way {@code validate_content_bindings} does (a fresh single-use retriever per
 * identifier; a null fetch or a non-empty {@code getErrors()} result means "does not resolve").
 */
@Component(service = McpTool.class)
public class FindOrphanedCommerceContentTool implements McpTool {

    private static final String DEFAULT_ROOT = "/content";
    private static final int DEFAULT_LIMIT = 200;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "find_orphaned_commerce_content";
    }

    @Override
    public String description() {
        return "Sweep AEM content under a root (default /content) for commerce reference tags (cq:products / "
            + "cq:categories) whose SKU or category UID no longer resolves against the live catalog. Bounded by "
            + "limit (default 200 content nodes scanned).";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("root").put("type", "string");
        properties.putObject("limit").put("type", "integer");
        return schema;
    }

    /**
     * Reverse JCR-SQL2 scan seam: find content nodes under {@code root} that carry a {@code cq:products} or
     * {@code cq:categories} property (page/XF {@code jcr:content}, DAM asset {@code jcr:content/metadata}),
     * bounded by {@code limit}. Overridden in tests -- see the class javadoc on {@link FindOrphanedCommerceContentTool}
     * for why: the pinned aem-mock's underlying {@code jcr-mock} {@code MockQueryManager} never parses/executes a
     * JCR-SQL2 statement against loaded content, so this method cannot be verified end-to-end against an aem-mock
     * fixture and is instead the seam a unit test overrides with real aem-mock {@link Resource}s.
     */
    protected List<Resource> findTaggedContent(ResourceResolver resolver, String root, int limit) {
        List<Resource> results = new ArrayList<>();
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            return results;
        }

        String statement = "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE(s, ["
            + root.replace("]", "]]") + "]) AND (s.[" + CommerceExperienceFragment.PN_CQ_PRODUCTS
            + "] IS NOT NULL OR s.[" + CommerceExperienceFragment.PN_CQ_CATEGORIES + "] IS NOT NULL)";

        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(statement, Query.JCR_SQL2);
            query.setLimit(limit);
            QueryResult queryResult = query.execute();
            RowIterator rows = queryResult.getRows();
            while (rows.hasNext() && results.size() < limit) {
                Node node = rows.nextRow().getNode();
                if (node == null) {
                    continue;
                }
                Resource resource = resolver.getResource(node.getPath());
                if (resource != null) {
                    results.add(resource);
                }
            }
        } catch (RepositoryException e) {
            // Fail closed to "nothing found" rather than surfacing a repository-level error to the MCP caller --
            // matches the read-tool contract of returning a compact, well-formed result.
            return new ArrayList<>();
        }
        return results;
    }

    protected boolean productResolves(StoreContext ctx, String sku) {
        McpProductRetriever retriever = new McpProductRetriever(ctx.getClient());
        retriever.setIdentifier(sku);
        ProductInterface product = retriever.fetchProduct();
        return product != null && retriever.getErrors().isEmpty();
    }

    protected boolean categoryResolves(StoreContext ctx, String uid) {
        McpCategoryRetriever retriever = new McpCategoryRetriever(ctx.getClient());
        retriever.setIdentifier(uid);
        CategoryInterface category = retriever.fetchCategory();
        return category != null && retriever.getErrors().isEmpty();
    }

    @Override
    public boolean authoringOnly() {
        return true; // authoring-oriented read tool -- not exposed on the anonymous shopper endpoint
    }

    @Override
    public JsonNode call(McpCallContext context, JsonNode args) {
        StoreContext ctx = (StoreContext) context;

        String root = args.path("root").asText(DEFAULT_ROOT);
        if (StringUtils.isBlank(root)) {
            root = DEFAULT_ROOT;
        }
        if (!root.startsWith("/content/") && !"/content".equals(root)) {
            throw new IllegalArgumentException("root must be under /content: " + root);
        }

        int limit = args.path("limit").asInt(DEFAULT_LIMIT);
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        ResourceResolver resolver = ctx.getRequest().getResourceResolver();
        List<Resource> tagged = findTaggedContent(resolver, root, limit);

        ObjectNode out = mapper.createObjectNode();
        out.put("root", root);
        out.put("scanned", tagged.size());
        ArrayNode orphans = out.putArray("orphans");

        for (Resource resource : tagged) {
            ValueMap properties = resource.getValueMap();

            for (String sku : CommerceContentTagger.readTagList(properties, CommerceExperienceFragment.PN_CQ_PRODUCTS)) {
                String baseSku = CombinedSku.parse(sku).getBaseSku();
                if (!productResolves(ctx, baseSku)) {
                    ObjectNode orphan = orphans.addObject();
                    orphan.put("path", resource.getPath());
                    orphan.put("identifier", baseSku);
                    orphan.put("identifierType", "product");
                    orphan.put("property", CommerceExperienceFragment.PN_CQ_PRODUCTS);
                }
            }

            for (String uid : CommerceContentTagger.readTagList(properties, CommerceExperienceFragment.PN_CQ_CATEGORIES)) {
                if (!categoryResolves(ctx, uid)) {
                    ObjectNode orphan = orphans.addObject();
                    orphan.put("path", resource.getPath());
                    orphan.put("identifier", uid);
                    orphan.put("identifierType", "category");
                    orphan.put("property", CommerceExperienceFragment.PN_CQ_CATEGORIES);
                }
            }
        }

        return out;
    }
}
