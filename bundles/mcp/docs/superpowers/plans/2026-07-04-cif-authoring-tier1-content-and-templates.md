# CIF Authoring Tier-1 Content & Template Reads — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the last 3 Tier-1 read/diagnostic tools from `AUTHORING_TOOLS_CATALOG.md`, completing Tier-1: `find_orphaned_commerce_content` (T-21, §5), `get_commerce_content_fragment` (T-22, §6), `suggest_template_for_page_type` (T-43, §9).

**Architecture:** All 3 are READ tools (`writesContent()==false` → both endpoints), OSGi `@Component(service = McpTool.class)`, auto-discovered by `ToolRegistry`. They read AEM content (JCR + the already-wired AEM CIF SDK) and return compact Jackson DTOs. No new external dependency. T-21 does a reverse JCR-SQL2 scan + retriever-based resolve check; T-22 reuses `AssociatedContentSupport`'s CF resolution and adds field reads; T-43 walks `/conf` templates.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, JCR-SQL2 (`javax.jcr` via `resolver.adaptTo(Session.class)`), AEM CIF SDK `AssociatedContentService` (exported, already wrapped by `AssociatedContentSupport`), `com.adobe.cq.dam.cfm` CF read API (exported, 6.5-safe), CIF retrievers, aem-mock + Mockito + JUnit 4.

## Global Constraints

*(From `bundles/mcp/AGENTS.md` — every task implicitly includes these.)*
- **AEM 6.5 / Java 8 / JDK 11.** No `var`/`List.of`/records/switch-expressions; no API newer than `bundles/core`. `javax` never `jakarta` (note: JCR/`javax.jcr` IS fine — it's `javax`).
- **Build/verify `mvn -pl bundles/mcp clean install`** (clean required). Format `mvn -pl bundles/mcp -Pformat-code process-classes` before commit. JDK 11: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`; mvn `/Users/levente/devel/prg/maven/bin/mvn`.
- **Apache 2.0 header** (exactly `Licensed under the Apache License, Version 2.0`) on every new `.java`. Explicit imports, no wildcards. Jackson only. No external MCP SDK.
- **New code under `com.adobe.cq.commerce.mcp.internal[.tools]`**; exported `…mcp` must not reference `…mcp.internal.*`; MUST NOT import non-exported core `…components.internal.*`.
- **READ tools must NOT override `writesContent()`.** Compact DTOs, never raw JCR/ValueMaps in output. Fail-closed on bad `path`/`root` args via the shared `internal/tools/PathArgs.resolvePage(...)` where a page arg is required.
- **Conventional commits `feat(mcp): …`, one tool per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.** Stage only files the task touches.

## Grounding (source-verified — reuse boundary)
- **AEM CIF SDK `AssociatedContentService`** (`com.adobe.cq.cif.common.associatedcontent.AssociatedContentService`, exported/`provided`) is **identifier-keyed only — NO enumeration API.** Already wrapped by `internal/AssociatedContentSupport.java` (`buildProductResult`/`buildCategoryResult` accept `contentFragmentModel`+`linkElement`; CF resolution uses `CfParams.of(id).model(...).property(...)`; `toContentFragment(...)` emits `{path,name,title,modelPath}` but NOT field values).
- **Commerce tag property names:** `CommerceExperienceFragment.PN_CQ_PRODUCTS` / `PN_CQ_CATEGORIES` (exported core model API, values `cq:products`/`cq:categories`), already imported by `CommerceContentTagger`/`AssociatedContentSupport`. Tag paths (§5): page/XF `jcr:content/cq:products`|`cq:categories`; DAM asset `jcr:content/metadata/cq:products`|`cq:categories`.
- **`CommerceContentTagger.readTagList(ValueMap, property)`** is `public static` — reuse to parse a multi-valued tag into distinct identifiers.
- **Resolve check:** `new McpProductRetriever(ctx.getClient())`/`new McpCategoryRetriever(...)` → `setIdentifier` → `fetchProduct()`/`fetchCategory()`; `null` or non-empty `getErrors()` = does-not-resolve (same as `ValidateContentBindingsTool`). Reduce a combined-SKU (`baseSku#variantSku`, separator `#`) to its base SKU before the product lookup.
- **CF field read:** `com.adobe.cq.dam.cfm.ContentFragment` (`getElement(name)`/`getElements()`/`ContentElement.getValue()`), already imported/used in `AssociatedContentSupport` — exported, 6.5-safe. (No CF *writing* — that's Tier-3, out of scope here.)
- **Templates:** `/conf/*/settings/wcm/templates/*`; page-type signal = the pre-placed component `sling:resourceSuperType` under `initial/jcr:content/root/container/container` (`…/commerce/product/*` = product; `…/productlist/*` = category; empty initial grid = catalog). `jcr:title` (`Product page`/`Category page`/`Catalog Page`) is fallback only. Pure `ResourceResolver` traversal (`ctx.getRequest().getResourceResolver()`); `SiteStructure`/`UrlProvider` don't help.
- **Reference:** catalog §5/§6/§9; `AGENTS.md` §3/§6/§8; templates `internal/tools/PathArgs.java`, `AssociatedContentSupport.java`, `CommerceContentTagger.java`, `ValidateContentBindingsTool.java`, and any shipped tool for tool/test shape.

## Shared conventions
Read-tool skeleton (no `writesContent()` override); `StoreContext ctx = (StoreContext) context;`; validate args → `IllegalArgumentException` (dispatcher → -32000). aem-mock tests with JCR fixtures under `src/test/resources/context/*.json` (AGENTS.md §6); use a `protected` seam for GraphQL resolve checks (so no live backend) and for the SDK CF lookup where needed. Per-task TDD loop: failing test → RED (`-Dtest=XxxTest`) → implement → GREEN → green gate → commit.

---

### Task 1: `find_orphaned_commerce_content` (T-21, §5)

Reverse JCR-SQL2 scan for commerce-tagged content, then flag dead identifiers.

**Files:** Create `internal/tools/FindOrphanedCommerceContentTool.java`; Test `internal/tools/FindOrphanedCommerceContentToolTest.java` + fixture `src/test/resources/context/orphaned-content.json`.

**Interfaces — Produces:** tool `find_orphaned_commerce_content`; args `{ "root": string (optional; JCR path prefix to scan, default "/content"; must be under /content), "limit": int (optional; max content nodes to scan, default 200) }`; result `{"root":"…","scanned":N,"orphans":[{"path":"…","identifier":"…","identifierType":"product|category","property":"cq:products|cq:categories"}]}` — only content whose identifier no longer resolves is listed.

- [ ] **Step 1: failing test.** Fixture `orphaned-content.json`: a page/asset tagged with a LIVE sku + one tagged with a DEAD sku, and a category-tagged page (one live UID, one dead UID). Override the resolve seam(s) (`protected boolean productResolves(StoreContext,String)` / `categoryResolves(...)`) so specific ids are dead. Assert only the dead-id content appears in `orphans` with the right `identifier`/`identifierType`/`property`; assert `scanned` counts the tagged nodes. Add: `root` not under /content → `IllegalArgumentException`; combined-SKU tag reduced to base before resolve.
- [ ] **Step 2: RED** (`-Dtest=FindOrphanedCommerceContentToolTest`).
- [ ] **Step 3: implement.** `resolver.adaptTo(Session.class)` → `QueryManager` → JCR-SQL2 selecting nodes where `[cq:products]` IS NOT NULL OR `[cq:categories]` IS NOT NULL under `ISDESCENDANTNODE([root])` (cover the page/XF `jcr:content` + asset `jcr:content/metadata` locations — a single query on the property, or per-location queries; keep it simple and bounded by `limit`). For each match: read the tag via `CommerceContentTagger.readTagList`, reduce combined-SKU to base, call the resolve seam; collect misses. Property-name constants from `CommerceExperienceFragment.PN_CQ_PRODUCTS`/`PN_CQ_CATEGORIES`. Do NOT depend on the SDK (no enumeration API).
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add find_orphaned_commerce_content read tool`).

---

### Task 2: `get_commerce_content_fragment` (T-22, §6)

Resolve a single commerce CF by `linkElement` match and return its fields. Reuses `AssociatedContentSupport`'s CF resolution; adds field reads.

**Files:** Create `internal/tools/GetCommerceContentFragmentTool.java`; Test `internal/tools/GetCommerceContentFragmentToolTest.java` + fixture `src/test/resources/context/commerce-cf.json`. May add a `fields`-returning method to `AssociatedContentSupport` if that's cleaner than reading in the tool — prefer the smallest change; if you add to `AssociatedContentSupport`, keep its existing methods unchanged.

**Interfaces — Produces:** tool `get_commerce_content_fragment`; args `{ "identifier": string (required — sku or category uid), "type": "product|category" (required), "contentFragmentModel": string (optional), "linkElement": string (optional) }`; result on hit `{"identifier":"…","type":"…","modelPath":"…","fragmentPath":"…","fields":{"<name>":"<value>", …}}`; on no match `{"identifier":"…","type":"…","resolves":false}`.

- [ ] **Step 1: failing test.** Fixture `commerce-cf.json`: a CF asset whose `linkElement` field holds the test sku, with a couple of scalar fields. Use a `protected` seam returning the resolved CF (or the `AssociatedContentSupport` result) so the test supplies a `com.adobe.cq.dam.cfm.ContentFragment` (aem-mock or Mockito) — assert `modelPath`/`fragmentPath` + `fields` map. Add: no CF found → `{resolves:false}`; missing `identifier`/invalid `type` → `IllegalArgumentException`.
- [ ] **Step 2: RED.**
- [ ] **Step 3: implement.** Reuse `AssociatedContentSupport`'s product/category CF resolution (`CfParams.of(identifier).model(contentFragmentModel).property(linkElement)`, first hit / `withLimit(1)`). For the hit, adapt the CF resource to `com.adobe.cq.dam.cfm.ContentFragment`, iterate `getElements()`, map `name → getValue()` (stringify; multi-value → array). Return `modelPath`/`fragmentPath`/`fields`.
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add get_commerce_content_fragment read tool`).

---

### Task 3: `suggest_template_for_page_type` (T-43, §9)

List `/conf` editable-template candidates matching a page-type signal.

**Files:** Create `internal/tools/SuggestTemplateForPageTypeTool.java`; Test `internal/tools/SuggestTemplateForPageTypeToolTest.java` + fixture `src/test/resources/context/conf-templates.json`.

**Interfaces — Produces:** tool `suggest_template_for_page_type`; args `{ "kind": "product|category|catalog" (required) }`; result `{"kind":"…","templates":[{"path":"…","title":"…","signal":"resourceSuperType|title"}]}` — templates whose `initial` pre-placed component matches the requested kind (product → `…/commerce/product/*`; category → `…/commerce/productlist/*`; catalog → empty `initial` grid), `jcr:title` as a fallback signal only.

- [ ] **Step 1: failing test.** Fixture `conf-templates.json` under `/conf/testsite/settings/wcm/templates/`: a product-page template (initial component `resourceSuperType=core/cif/components/commerce/product/v3/product`), a category-page template (`…/productlist/v2/productlist`), a catalog-page template (empty initial grid), and a non-commerce template (should never match). Assert `kind:"product"` returns only the product template with `signal:"resourceSuperType"`; `kind:"category"` only the category one; `kind:"catalog"` the empty-grid one. Add: invalid `kind` → `IllegalArgumentException`.
- [ ] **Step 2: RED.**
- [ ] **Step 3: implement.** Via `ctx.getRequest().getResourceResolver()`, iterate `/conf/*/settings/wcm/templates/*`; for each, read `initial/jcr:content/root/container/container/*` child's `sling:resourceSuperType` (super-type-aware where relevant) to classify; empty initial grid ⇒ catalog. Fall back to `jcr:title` matching only when no component signal (`signal:"title"`). Filter to the requested `kind`.
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add suggest_template_for_page_type read tool`).

---

## Self-Review
**Spec coverage:** T-21 → Task 1; T-22 → Task 2; T-43 → Task 3. Completes all Tier-1 (§3/§5/§6/§7/§8/§9 read/diagnostic tools). T-19/T-20 (§5) already shipped as baseline; §6 write tools (T-23/24) and all Tier-2/3 are out of scope.
**Type consistency:** all three return `ObjectNode`; none overrides `writesContent()`; names match catalog §11 (T-21/T-22/T-43). Reuse: `CommerceContentTagger.readTagList`, `CommerceExperienceFragment` constants, `AssociatedContentSupport` CF resolution, `Mcp*Retriever`, `com.adobe.cq.dam.cfm.ContentFragment` — all confirmed exported/in-bundle.
**Independence:** all 3 tasks independent; parallelizable across subagents.
**Placeholder scan:** mechanisms specified by exact APIs/constants/paths + cited catalog sections; the one JCR-SQL2 query shape is described (property IS NOT NULL under ISDESCENDANTNODE(root), bounded by limit) with the fallback of per-location queries.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` + cited catalog section + the named in-bundle templates first), TDD, green gate, commit; task review after each; whole-branch review at the end. Task 1 (JCR-SQL2 reverse scan) is the highest-risk — review it hardest.
