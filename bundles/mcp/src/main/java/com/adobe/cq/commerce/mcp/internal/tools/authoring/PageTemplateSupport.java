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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Shared classification + resolution of editable-template candidates under
 * {@code /conf/*&#47;settings/wcm/templates/*} by the page type ({@code product}/{@code category}/{@code catalog})
 * their pre-placed {@code initial} content implies -- see catalog &sect;9. Hoisted out of
 * {@code SuggestTemplateForPageTypeTool} (which now delegates to it) so the Tier-3 page-creation tools resolve
 * templates using the exact same signal a {@code suggest_template_for_page_type} discovery call would report.
 * <p>
 * <b>Signal, in priority order</b>: the pre-placed component under
 * {@code initial/jcr:content/root/container/container} (the responsive grid), classified via
 * {@link Resource#isResourceType(String)} against the known CIF commerce resource types (see the
 * {@code ProductImpl}/{@code ProductListImpl} {@code RESOURCE_TYPE} constants in {@code bundles/core}'s
 * {@code v1}/{@code v2}/{@code v3} {@code …components.internal.models.*} packages -- those constants live in a
 * non-exported internal package, so the literal strings are redeclared here). {@code isResourceType} follows a
 * resource's {@code sling:resourceSuperType} chain -- including one registered on an {@code /apps} component
 * definition -- so Venia's proxy components (e.g. {@code venia/components/commerce/product}) resolve correctly in
 * real AEM: an empty grid classifies as {@code catalog}; a product/productlist grid child classifies as
 * {@code product}/{@code category}; a grid whose children are all non-commerce classifies as nothing.
 * <p>
 * <b>Fallback signal</b>: only when the {@code initial} grid can't be read at all (missing/malformed) does this
 * fall back to matching the template's own {@code jcr:title} case-insensitively against the Venia convention
 * ({@code "Product page"}/{@code "Category page"}/{@code "Catalog Page"}).
 * <p>
 * <b>aem-mock limitation</b>: the pinned aem-mock's {@code isResourceType} matches by exact identity only -- it does
 * not walk a resource's super-type chain through {@code /apps} component definitions the way real AEM does. Unit
 * fixtures therefore set the pre-placed component's {@code sling:resourceType} directly to the core type (as if the
 * {@code /apps} proxy resolution had already happened); the real Venia proxy path can only be verified against a
 * live AEM instance.
 * <p>
 * This is a plain stateless helper, not an {@code McpTool} -- it is not an OSGi component and not discovered by
 * {@code ToolRegistry}.
 */
public final class PageTemplateSupport {

    private static final String TEMPLATES_QUERY_ROOT = "/conf";
    private static final String TEMPLATES_SUFFIX = "settings/wcm/templates";
    private static final String INITIAL_GRID_PATH = "initial/jcr:content/root/container/container";

    /**
     * CIF PRODUCT component resource types, by version (verified against {@code ProductImpl.RESOURCE_TYPE} in
     * {@code bundles/core}'s {@code v1}/{@code v2}/{@code v3} {@code …models.*.product} packages).
     */
    private static final List<String> PRODUCT_RESOURCE_TYPES = Arrays.asList(
        "core/cif/components/commerce/product/v1/product",
        "core/cif/components/commerce/product/v2/product",
        "core/cif/components/commerce/product/v3/product");

    /**
     * CIF PRODUCTLIST component resource types, by version (verified against {@code ProductListImpl.RESOURCE_TYPE}
     * in {@code bundles/core}'s {@code v1}/{@code v2} {@code …models.*.productlist} packages -- there is no v3
     * productlist).
     */
    private static final List<String> PRODUCTLIST_RESOURCE_TYPES = Arrays.asList(
        "core/cif/components/commerce/productlist/v1/productlist",
        "core/cif/components/commerce/productlist/v2/productlist");

    public static final List<String> VALID_KINDS = Arrays.asList("product", "category", "catalog");

    private static final String SIGNAL_RESOURCE_SUPER_TYPE = "resourceSuperType";
    private static final String SIGNAL_TITLE = "title";

    private PageTemplateSupport() {}

    /**
     * Classifies a single {@code /conf/*&#47;settings/wcm/templates/<name>} resource by its {@code initial} grid
     * content, falling back to {@code jcr:title} only when the grid itself can't be read.
     *
     * @param template the template resource to classify (typically a {@code cq:Template} node)
     * @return the {@link Classification} (kind + title + signal), or {@code null} if the template carries no
     *         recognizable page-type signal (e.g. its grid holds only non-commerce components)
     */
    public static Classification classify(Resource template) {
        if (template == null) {
            return null;
        }
        String title = title(template);
        Resource grid = template.getChild(INITIAL_GRID_PATH);
        if (grid == null) {
            return classifyByTitle(title);
        }

        Iterator<Resource> children = grid.listChildren();
        if (!children.hasNext()) {
            return new Classification("catalog", SIGNAL_RESOURCE_SUPER_TYPE, title);
        }

        while (children.hasNext()) {
            Resource component = children.next();
            // Check productlist BEFORE product: both families are matched via isResourceType against distinct,
            // fully-versioned literal strings (e.g. ".../product/v3/product" vs. ".../productlist/v2/productlist"),
            // so there is no prefix-collision risk here -- the ordering is kept for readability/symmetry, not
            // because it is load-bearing.
            if (matchesAnyType(component, PRODUCTLIST_RESOURCE_TYPES)) {
                return new Classification("category", SIGNAL_RESOURCE_SUPER_TYPE, title);
            }
            if (matchesAnyType(component, PRODUCT_RESOURCE_TYPES)) {
                return new Classification("product", SIGNAL_RESOURCE_SUPER_TYPE, title);
            }
        }

        // Grid has children, but none is a recognized commerce component: no signal, and not a title-fallback
        // candidate either (title fallback is reserved for when the grid can't be read at all).
        return null;
    }

    /**
     * Resolves the template a page of {@code kind} should be created from.
     * <ul>
     * <li>If {@code explicitTemplatePath} is non-blank, it must resolve to an existing template whose
     * {@link #classify(Resource)} kind equals {@code kind}; otherwise {@link IllegalArgumentException}.</li>
     * <li>If {@code explicitTemplatePath} is null/blank, every {@code /conf/*&#47;settings/wcm/templates/*} is
     * scanned and the first whose {@code classify} kind equals {@code kind} is returned; if none matches,
     * {@link IllegalArgumentException} (the caller should then pass {@code template} explicitly).</li>
     * </ul>
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param kind one of {@link #VALID_KINDS}
     * @param explicitTemplatePath an explicit template path, or {@code null}/blank to auto-discover
     * @return the resolved template {@link Resource} (its {@code initial} content carries the expected page-type
     *         signal, validated pre-create per catalog &sect;9's guardrail)
     * @throws IllegalArgumentException if {@code kind} is invalid, an explicit path is missing or of the wrong kind,
     *             or no template of {@code kind} can be auto-discovered
     */
    public static Resource resolveTemplate(ResourceResolver resolver, String kind, String explicitTemplatePath) {
        return resolveTemplate(resolver, kind, explicitTemplatePath, null);
    }

    /**
     * Site-scoped overload of {@link #resolveTemplate(ResourceResolver, String, String)}: when
     * {@code explicitTemplatePath} is null/blank, auto-discovery scans {@code preferredConfPath} (the site's own
     * {@code /conf/<site>}, from {@code SiteAppsSupport.confPathFor(landingPage)}) <em>first</em>, so a page is
     * created from the site's own template rather than an unrelated same-kind template that happens to sort earlier
     * in a global {@code /conf} scan; only if the site conf has no template of {@code kind} (or
     * {@code preferredConfPath} is null/blank) does it fall back to the global {@code /conf/*} scan.
     *
     * @param resolver the caller's {@link ResourceResolver} (JCR ACLs enforced)
     * @param kind one of {@link #VALID_KINDS}
     * @param explicitTemplatePath an explicit template path, or {@code null}/blank to auto-discover
     * @param preferredConfPath the site's {@code /conf/<site>} root to try first, or {@code null}/blank to skip
     * @return the resolved template {@link Resource}
     * @throws IllegalArgumentException if {@code kind} is invalid, an explicit path is missing or of the wrong kind,
     *             or no template of {@code kind} can be auto-discovered
     */
    public static Resource resolveTemplate(ResourceResolver resolver, String kind, String explicitTemplatePath,
        String preferredConfPath) {
        if (!VALID_KINDS.contains(kind)) {
            throw new IllegalArgumentException("kind must be one of " + VALID_KINDS + ": " + kind);
        }

        if (StringUtils.isNotBlank(explicitTemplatePath)) {
            Resource template = resolver.getResource(explicitTemplatePath);
            if (template == null) {
                throw new IllegalArgumentException("template not found: " + explicitTemplatePath);
            }
            Classification classification = classify(template);
            if (classification == null || !kind.equals(classification.getKind())) {
                throw new IllegalArgumentException("template is not a " + kind + " template: " + explicitTemplatePath);
            }
            return template;
        }

        if (StringUtils.isNotBlank(preferredConfPath)) {
            Resource preferred = firstTemplateOfKind(resolver.getResource(preferredConfPath), kind);
            if (preferred != null) {
                return preferred;
            }
        }

        Resource confRoot = resolver.getResource(TEMPLATES_QUERY_ROOT);
        if (confRoot != null) {
            for (Resource configRoot : confRoot.getChildren()) {
                Resource template = firstTemplateOfKind(configRoot, kind);
                if (template != null) {
                    return template;
                }
            }
        }
        throw new IllegalArgumentException(
            "no " + kind + " template found under /conf/*/settings/wcm/templates; pass template explicitly");
    }

    /**
     * Scans one {@code /conf/<config>} root's {@code settings/wcm/templates} folder and returns the first template
     * whose {@link #classify(Resource)} kind equals {@code kind}, or {@code null} when {@code configRoot} is null,
     * carries no templates folder, or holds no template of that kind.
     */
    private static Resource firstTemplateOfKind(Resource configRoot, String kind) {
        Resource templatesFolder = configRoot == null ? null : configRoot.getChild(TEMPLATES_SUFFIX);
        if (templatesFolder == null) {
            return null;
        }
        for (Resource template : templatesFolder.getChildren()) {
            Classification classification = classify(template);
            if (classification != null && kind.equals(classification.getKind())) {
                return template;
            }
        }
        return null;
    }

    /**
     * True if the component resolves to any of the given CIF core resource types via
     * {@link Resource#isResourceType(String)} (which follows {@code sling:resourceSuperType}, including one
     * registered on an {@code /apps} component definition, so a Venia proxy component still matches in real AEM;
     * the pinned aem-mock matches by identity only -- see the class javadoc's "aem-mock limitation" note).
     */
    private static boolean matchesAnyType(Resource component, List<String> coreResourceTypes) {
        for (String coreResourceType : coreResourceTypes) {
            if (component.isResourceType(coreResourceType)) {
                return true;
            }
        }
        return false;
    }

    private static Classification classifyByTitle(String title) {
        if (title == null) {
            return null;
        }
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        if ("product page".equals(normalized)) {
            return new Classification("product", SIGNAL_TITLE, title);
        }
        if ("category page".equals(normalized)) {
            return new Classification("category", SIGNAL_TITLE, title);
        }
        if ("catalog page".equals(normalized)) {
            return new Classification("catalog", SIGNAL_TITLE, title);
        }
        return null;
    }

    private static String title(Resource template) {
        Resource content = template.getChild("jcr:content");
        return content == null ? null : content.getValueMap().get("jcr:title", String.class);
    }

    /**
     * The page-type classification of a template: its {@code kind} ({@code product}/{@code category}/
     * {@code catalog}), the {@code signal} that produced it ({@code resourceSuperType} or {@code title}), and the
     * template's own {@code jcr:title}.
     */
    public static final class Classification {
        private final String kind;
        private final String signal;
        private final String title;

        private Classification(String kind, String signal, String title) {
            this.kind = kind;
            this.signal = signal;
            this.title = title;
        }

        public String getKind() {
            return kind;
        }

        public String getSignal() {
            return signal;
        }

        public String getTitle() {
            return title;
        }
    }
}
