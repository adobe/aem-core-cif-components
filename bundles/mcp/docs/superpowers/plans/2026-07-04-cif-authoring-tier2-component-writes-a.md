# CIF Authoring Tier-2 Component Writes (Batch A) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship a shared write helper + the flat-property §2 component-config WRITE tools from `AUTHORING_TOOLS_CATALOG.md`: `configure_productteaser_component` (T-01), `configure_relatedproducts_component` (T-04), `configure_product_visible_sections` (T-07), `configure_page_commerce_links` (T-08 nav-config delta). (Multifield tools T-02/T-03/T-05/T-06 and §8/§7 writes are separate later plans.)

**Architecture:** All are WRITE tools — `@Component(service = McpTool.class)` with **`writesContent()` returning `true`** (authoring `mcp-authoring` endpoint only), auto-discovered by `ToolRegistry`. Each runs under the **caller's** `ResourceResolver` (JCR ACLs enforced) and **fails closed** on a non-`/content` path, missing resource, non-`ModifiableValueMap` resource, or a resource that is not the expected CIF component (super-type-aware `Resource.isResourceType` against redeclared literals). Task 1 extracts a shared `CommerceWriteSupport` helper (no such helper exists today — the shipped `Configure*Tool`s each copy the block) so these and future write tools don't duplicate the validate/adapt/commit/verify scaffold.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, Sling `ModifiableValueMap`/`ResourceResolver`, CIF `CombinedSku` (exported), aem-mock + Mockito + JUnit 4.

## Global Constraints
*(From `bundles/mcp/AGENTS.md` — every task implicitly includes these; §4 "How to add a write tool" is REQUIRED reading.)*
- **AEM 6.5 / Java 8 / JDK 11.** No `var`/`List.of`/records/switch-expressions; no API newer than `bundles/core`. `javax` never `jakarta`.
- **Build/verify `mvn -pl bundles/mcp clean install`** (clean required). Format `mvn -pl bundles/mcp -Pformat-code process-classes` before commit. JDK 11: `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home`; mvn `/Users/levente/devel/prg/maven/bin/mvn`.
- **Apache 2.0 header** (`Licensed under the Apache License, Version 2.0`) on every new `.java`. Explicit imports, no wildcards. Jackson only. No external MCP SDK.
- **New code under `com.adobe.cq.commerce.mcp.internal[.tools]`**; exported `…mcp` must not reference `…mcp.internal.*`; MUST NOT import non-exported core `…components.internal.*` (so component `*Impl.RESOURCE_TYPE` constants are NOT importable — redeclare the literals).
- **WRITE-tool security (AGENTS.md §4 — non-negotiable):** `writesContent()` returns `true`; use `ctx.getRequest().getResourceResolver()` (NEVER a service/admin resolver); fail closed with `IllegalArgumentException` (dispatcher → -32000) on missing args / path not under `/content/` / resource not found / not `ModifiableValueMap`-adaptable / **not the expected CIF component**; every write tool MUST have a **negative test** proving the fail-closed path (a non-CIF `sling:resourceType` → `IllegalArgumentException`). Author-only activation is via the existing `config.author` `AuthoringMcpServlet` config — no per-tool wiring.
- **Conventional commits `feat(mcp): …`, one tool per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.** Stage only files the task touches.

## Grounding (source-verified — the reuse boundary & value formats)
- **All component `RESOURCE_TYPE`s are internal → redeclare literals** and match with the shipped pattern `Arrays.asList(<literals>).stream().anyMatch(target::isResourceType)` (super-type-aware; handles Venia proxies). Verified literals:
  - productteaser: `core/cif/components/commerce/productteaser/v1/productteaser` (v1 only).
  - relatedproducts: `core/cif/components/commerce/relatedproducts/v1/relatedproducts` (v1 only).
  - product: `core/cif/components/commerce/product/v1/product`, `…/v2/product`, `…/v3/product`.
  - structure page: `core/cif/components/structure/page/v1/page`, `…/v2/page`, `…/v3/page`.
- **Shipped write tools to mirror/refactor:** `internal/tools/ConfigureProductComponentTool.java`, `ConfigureProductListComponentTool.java` (has the post-write re-read verification — the better template), `ConfigureCatalogPageTool.java`, `TagContentWithCommerceTool.java`. They each copy: cast `StoreContext`; read args `args.path(x).asText(null)`; validate `path` under `/content/`; `resolver = ctx.getRequest().getResourceResolver()`; `resolver.getResource(path)` (page tools append `/jcr:content`); resource-type gate; `adaptTo(ModifiableValueMap.class)`; `put`/`remove`; `resolver.commit()`; return `{path, …, updated}`.
- **Value formats:** `selection` (teaser) = combinedSku via **exported** `com.adobe.cq.commerce.core.components.models.common.CombinedSku` (`new CombinedSku(base, variant).toString()`, separator `#`); relatedproducts `product` = **plain base SKU** (no combinedSku); `relationType` ∈ `{RELATED_PRODUCTS, UPSELL_PRODUCTS, CROSS_SELL_PRODUCTS}` (enum names, redeclare as a validated set); `visibleSections` = `String[]` of **lowercase** tokens `{title, price, sku, images, options, quantity, actions, description, details}` (note **`images` plural**).
- **Corrections vs catalog §2:** `enableAddToCart`/`enableAddToWishList` are **style/policy** properties (read from `currentStyle`), NOT component-instance dialog fields — do NOT expose them as settable args (writing them on the instance is a no-op). The teaser model reads `selection` only (no `selectionType` on teaser).
- **Reference:** catalog §2 (per-tool props/gotchas), `AGENTS.md` §4.

## Shared conventions
Write-tool skeleton per AGENTS.md §4. aem-mock tests: build a resource with the right `sling:resourceType` in a JCR fixture (or `context.create().resource(path, "sling:resourceType", <type>, …)`), call the tool with the caller resolver, assert the persisted property via `resolver.getResource(path).getValueMap()`; **negative test** with a non-CIF resourceType → `IllegalArgumentException`. Per-task TDD loop: failing test → RED → implement → GREEN → green gate → commit.

---

### Task 1: `CommerceWriteSupport` helper + `configure_productteaser_component` (T-01)

**Files:** Create `internal/CommerceWriteSupport.java` (+ `internal/CommerceWriteSupportTest.java`); Create `internal/tools/ConfigureProductTeaserComponentTool.java` (+ test).

**Interfaces — Produces (later Tier-2 write tools consume the helper):**
- `CommerceWriteSupport.resolveComponent(ResourceResolver resolver, String argName, String path, java.util.List<String> allowedResourceTypes)` → the target `Resource` (its own node), throwing `IllegalArgumentException` on: blank path / not under `/content/` / not found / none of `allowedResourceTypes` match `resource.isResourceType(t)`. (A page variant `resolvePageContent(...)` that appends `/jcr:content` may be added when a page tool needs it — defer to the plan that needs it.)
- `CommerceWriteSupport.mutableMap(Resource resource, String argName)` → `ModifiableValueMap` (throws if not adaptable).
- (Optional) `CommerceWriteSupport.commitAndVerify(ResourceResolver, String path, String verifyProperty, Object expected)` → boolean persisted — OR return the committed value; keep it small. Tools may just `resolver.commit()` then re-read.

**Tool contract:** `configure_productteaser_component`; `writesContent()==true`; args `{ "path": string (required — the teaser component resource), "sku": string (required), "cta": string (optional: ""|"add-to-cart"|"details"), "ctaText": string (optional), "linkTarget": string (optional), "id": string (optional) }`; effect: writes `selection` = combinedSku(sku) and the provided optional props on the component node; result `{"path":"…","selection":"…","updated":true}`. Gate: productteaser v1. `sku` may be `base` or `base#variant` — normalize via `CombinedSku`.

- [ ] **Step 1: failing tests.** Helper test: a resource of an allowed type resolves; a non-allowed type → IAE; non-/content → IAE; not-found → IAE. Tool test: create a `productteaser/v1/productteaser` resource, call with `{path, sku:"MJ01", cta:"add-to-cart"}`, assert persisted `selection=="MJ01"` + `cta=="add-to-cart"`; **negative test**: a `core/wcm/components/text` resource → `IllegalArgumentException`; assert `writesContent()==true`.
- [ ] **Step 2: RED** (`-Dtest=CommerceWriteSupportTest,ConfigureProductTeaserComponentToolTest`).
- [ ] **Step 3: implement** the helper (redeclare no RT literals in the helper — it takes the allowed list as a param) + the tool (redeclare the productteaser v1 literal; use `CommerceWriteSupport` + `CombinedSku`).
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add CommerceWriteSupport + configure_productteaser_component write tool`).

---

### Task 2: `configure_relatedproducts_component` (T-04)

**Files:** Create `internal/tools/ConfigureRelatedProductsComponentTool.java` (+ test).

**Interfaces:** Consumes `CommerceWriteSupport`. Tool `configure_relatedproducts_component`; `writesContent()==true`; args `{ "path": string (required), "product": string (optional — PLAIN base SKU; falls back to page-URL product when absent, so allow clearing/omitting), "relationType": string (required: one of `RELATED_PRODUCTS`|`UPSELL_PRODUCTS`|`CROSS_SELL_PRODUCTS`) }`; effect: writes `product` (plain SKU, or remove when omitted) + `relationType`; result `{"path":"…","relationType":"…","updated":true}`. Gate: relatedproducts v1.

- [ ] Step 1 failing test: create a relatedproducts v1 resource; set `{product:"MJ01", relationType:"UPSELL_PRODUCTS"}`; assert persisted `product`(plain, NOT combinedSku)+`relationType`; invalid `relationType` → IAE; negative non-CIF type → IAE. Step 2 RED. Step 3 implement (validate relationType against the redeclared enum-name set; `product` is a plain SKU, do NOT combinedSku-encode). Step 4 GREEN. Step 5 commit (`feat(mcp): add configure_relatedproducts_component write tool`).

---

### Task 3: `configure_product_visible_sections` (T-07)

**Files:** Create `internal/tools/ConfigureProductVisibleSectionsTool.java` (+ test).

**Interfaces:** Consumes `CommerceWriteSupport`. Tool `configure_product_visible_sections`; `writesContent()==true`; args `{ "path": string (required — a product component), "visibleSections": [string,…] (required — lowercase tokens) }`; effect: writes `visibleSections` = `String[]` of validated lowercase tokens; result `{"path":"…","visibleSections":[…],"updated":true}`. Gate: product v1/v2/v3. Valid tokens: `title, price, sku, images, options, quantity, actions, description, details` (reject unknown tokens → IAE). Empty array clears (removes) the property (falls back to style default).

- [ ] Step 1 failing test: create a product v3 resource; set `{visibleSections:["title","price","images"]}`; assert persisted `String[]`; an invalid token (e.g. `"image"` singular) → IAE; negative non-CIF type → IAE. Step 2 RED. Step 3 implement (validate against the lowercase token set incl. `images` plural; write `String[]`; empty → remove). Step 4 GREEN. Step 5 commit (`feat(mcp): add configure_product_visible_sections write tool`).

---

### Task 4: `configure_page_commerce_links` (T-08, nav-config delta)

**Files:** Create `internal/tools/ConfigurePageCommerceLinksTool.java` (+ test).

**Interfaces:** Consumes `CommerceWriteSupport` (page variant — resolve `path` then operate on its `jcr:content`). Tool `configure_page_commerce_links`; `writesContent()==true`; args `{ "path": string (required — a CIF structure page), "cifProductPage": string (optional content path), "cifCategoryPage": string (optional), "cifSearchResultsPage": string (optional) }`; effect: writes the nav-config pagefields `cq:cifProductPage`/`cq:cifCategoryPage`/`cq:cifSearchResultsPage` on the page's `jcr:content` (each: set when provided, remove when explicitly null/empty per a documented rule); result `{"path":"…","updated":true}`. Gate: structure page v1/v2/v3 (validate the page's `jcr:content` resource type).
> Scope note (from catalog §2.8): the `cq:products`/`cq:categories` associated-content markers on a page are handled by the shipped `tag_content_with_commerce` — this tool covers ONLY the `cq:cif*Page` nav-config pagefields. Document that in the tool description.

- [ ] Step 1 failing test: create a structure page (v3) with `jcr:content`; set `{cifProductPage:"/content/venia/us/en/products"}`; assert persisted `cq:cifProductPage` on `jcr:content`; negative: a non-CIF page/resource → IAE; missing all three optionals → decide (no-op with `updated:false`, or IAE — pick one and test it). Step 2 RED. Step 3 implement (resolve `path` + `/jcr:content`, gate structure-page RT, write the three `cq:cif*Page` props). Step 4 GREEN. Step 5 commit (`feat(mcp): add configure_page_commerce_links write tool`).

---

## Self-Review
**Spec coverage:** T-01 → Task 1; T-04 → Task 2; T-07 → Task 3; T-08 (nav-config delta) → Task 4. Deferred to later Tier-2 plans: T-02 (productcarousel), T-03 (productlist full fields incl. `fragments` multifield), T-05/T-06 (featuredcategorylist/categorycarousel `items` multifield), §8 bind/unbind (T-29–32), §7 `configure_catalog_page` `urlPath` (T-28).
**Type consistency:** all tools `writesContent()==true`, use `CommerceWriteSupport` (Task 1) with the same `resolveComponent(resolver, argName, path, allowedTypes)` signature; RT literals redeclared per tool; `CombinedSku` imported (exported). Names match catalog §11 (T-01/T-04/T-07/T-08).
**Constraint check:** enableAddToCart/WishList intentionally NOT exposed (style props); `visibleSections` uses `images` plural; relatedproducts `product` is plain SKU not combinedSku; every tool has a fail-closed negative test.
**Ordering:** Task 1 (helper) before 2–4; all consume the helper.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §2 + the shipped `Configure*Tool`s first), TDD incl. the fail-closed negative test, green gate, commit; task review after each; whole-branch review at the end. Task 1 (helper + first write tool) sets the shared pattern — review it hardest. These are content-mutating tools; live-validate the batch against Venia after review.
