# CIF Authoring Tier-3 Page / PDP / PLP Creation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the §9 page-creation tools: `create_catalog_page` (T-38), `create_specific_pdp` (T-39), `create_specific_plp` (T-40), `create_specific_pdp_for_category_tree` (T-41), `scaffold_catalog_section` (T-42). Adds a shared `PageCreationSupport` (page create + parent/template validation) and hoists template classification so both `suggest_template_for_page_type` (T-43) and these tools share one signal.

**Architecture:** WRITE tools (`writesContent()==true`, authoring endpoint), caller-resolver, fail-closed. Each tool: validate parent + resolve/validate a template → (unless `dryRun`) `PageManager.create(parentPath, name, templatePath, title)` (AEM copies the template's `initial` content, so the commerce component is pre-placed) → **delegate the category/product binding to the already-shipped tool** for that page type (`configure_catalog_page` / `bind_page_to_products` / `bind_page_to_category` / `bind_product_page_to_category_tree`) by calling its `call(ctx, {path:newPagePath, …})` → readback → return. All support **`dryRun`** (approved Tier-3 guardrail) and **strict parent-path validation** (approved decision: parent must be an existing page/folder under `/content`).

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, `com.day.cq.wcm.api.PageManager`/`Page`/`WCMException` (via `resolver.adaptTo(PageManager.class)`), Sling `ResourceResolver`/`Resource`, aem-mock + Mockito + JUnit 4.

## Global Constraints
*(From `bundles/mcp/AGENTS.md`; §4 write-tool rules REQUIRED.)*
- AEM 6.5 / Java 8 / JDK 11. No `var`/`List.of`/records/switch-expr; `javax` never `jakarta`. Apache 2.0 header; explicit imports, no wildcards; Jackson only.
- Build/verify `mvn -pl bundles/mcp clean install` (clean); format `-Pformat-code process-classes`. Build env `export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`; mvn `/Users/levente/devel/prg/maven/bin/mvn`. Filter logs with `grep -E "Tests run:|BUILD (SUCCESS|FAILURE)|<<< FAILURE|<<< ERROR"` (DEBUG is very verbose). Baseline = 447 tests.
- New code under `…mcp.internal[.tools]`; no non-exported core `…components.internal.*` import (redeclare component/page RT literals as the shipped tools do; `com.day.cq.wcm.api.*` is exported).
- WRITE-tool security (§4): `writesContent()==true`; caller `ctx.getRequest().getResourceResolver()`; fail closed (IAE → -32000); MANDATORY negative test. Real readback. **No auto-publish** (end at the page create + delegated bind's `commit()`).
- **Tier-3 guardrail:** `dryRun` (default false) → compute the would-be page path + template + intended binding, create/commit NOTHING.
- Conventional commits `feat(mcp): …`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer. Stage only touched files.

## Grounding (source-verified)
- **Create API:** `PageManager pm = resolver.adaptTo(PageManager.class); Page p = pm.create(parentPath, name, templatePath, title);` — overload `create(String parent, String name, String template, String title)` (also a `(…, boolean autoSave)` overload; use the 4-arg + explicit `resolver.commit()`, or the autoSave=false overload then commit). Throws checked `com.day.cq.wcm.api.WCMException` → catch → `IllegalArgumentException` (fail closed, clear message). AEM copies the template's `initial/jcr:content` automatically — NO CIF-specific step. `name` may be null (AEM derives from title) but prefer an explicit unique name via `ResourceUtil.createUniqueChildName(parent, base)`.
- **Template resolution (hoist):** the classify logic in `SuggestTemplateForPageTypeTool` (pre-placed `initial/jcr:content/root/container/container` component via `Resource.isResourceType` against the versioned product/productlist literals; empty grid ⇒ `catalog`; `jcr:title` fallback) is the signal. Hoist it into a shared helper (e.g. `PageTemplateSupport.classify(Resource template)` / `resolveTemplate(ResourceResolver, String kind, String explicitTemplatePath)`) and refactor `SuggestTemplateForPageTypeTool` to delegate to it (keep its tests green). Create tools: if `template` given → validate `classify(template).kind == expectedKind` else IAE; if omitted → scan `/conf/*/settings/wcm/templates/*`, pick the first whose `classify` matches the expected kind, else IAE ("no <kind> template found; pass template explicitly").
- **Guardrail (catalog §9):** BEFORE `PageManager.create`, validate the resolved template's `initial` content actually carries the expected `product`/`productlist` component (via the shared classify) — fail clearly rather than create an empty-rendering page. (For `catalog`, the empty-grid signal is expected.)
- **Binding delegation (DRY):** after create, apply the page-type binding by invoking the shipped tool's `call(ctx, args)` with `path` = the new page path:
  - T-38 → `new ConfigureCatalogPageTool().call(ctx, {path, categoryUid:rootCategoryId, idType?, showMainCategories?})`.
  - T-39 → `new BindPageToProductsTool().call(ctx, {path, skusOrUrlKeys})`.
  - T-40 → `new BindPageToCategoryTool().call(ctx, {path, categoryUid, urlPath, includesSubCategories?})`.
  - T-41 → `new BindProductPageToCategoryTreeTool().call(ctx, {path, categoryUid, urlPath, includesSubCategories?})`.
  These tools are stateless (only an `ObjectMapper` field) and `new`-able; each does its own `jcr:content` resolve + resourceType gate + write + `commit()` + readback. Reusing them guarantees create and configure never diverge. (Alternative if a subagent finds delegation awkward: hoist the write bodies into shared helpers — but delegation is preferred; note the choice.)
- **`initial` vs `structure`:** the pre-placed commerce component lives in the template's `initial` grid (copied into the new page). The new page's `jcr:content` resourceType (what the bind/configure gate checks via `isResourceType`) comes from the template — in Venia these super-type the CIF structure/catalog page types, so `isResourceType` matches in **real AEM** (but not aem-mock's identity-only match — see testing note).
- **Reference:** catalog §9 (768-822); `AGENTS.md` §4; `SuggestTemplateForPageTypeTool` (classify), `ConfigureCatalogPageTool` / `BindPageToProductsTool` / `BindPageToCategoryTool` / `BindProductPageToCategoryTreeTool` (delegation targets + write/gate/readback pattern), `CreateProductTeasersTool` (dryRun precedent), `CommerceWriteSupport` (parent-path validation idioms).

## Testing note (IMPORTANT — page create + aem-mock)
aem-mock provides a `PageManager` (`context.pageManager()`), and `pm.create(...)` works in-mock for a template that exists — BUT the created page's `jcr:content` won't carry Venia's proxy super-typed resourceType, so `isResourceType` gates in the delegated bind/configure tools match by **identity only** in-mock. Two-part approach:
1. **Unit (aem-mock + a create seam):** put `PageManager.create` behind a `protected` seam so tests can (a) assert it's called with the right `parentPath/name/templatePath/title`, and (b) return a canned page resource whose `jcr:content` resourceType is set **directly to the core catalog/product/category page type** (as if the proxy resolved), so the delegated binding's gate passes in-mock. Assert the binding props are written (readback) + `dryRun` creates nothing (seam + `commit` never called) + parent/template negatives → IAE. Mirror `SuggestTemplateForPageTypeTool`/`CheckSpecificPageCapabilityTool`'s documented aem-mock `isResourceType` caveat.
2. **LIVE validation** (the real proof, after review): against Venia (`/content/venia/us/en`, templates under `/conf/venia/settings/wcm/templates/`), create each page type on a reversible scratch parent, verify the page + its `initial` commerce component + the binding props, then delete. Venia proxy super-typing only resolves on a real instance.

## Shared conventions
Write-tool skeleton per §4. Strict parent-path validation: `parentPath` non-blank, under `/content`, resolves to an existing resource that is a page (`adaptTo(Page.class)!=null`) or a folder — reject a non-existent parent (fail closed). dryRun bypasses create/commit but still validates parent + template. Per-task TDD loop; each result includes `{pagePath, template, dryRun, …binding echo…}`.

---

### Task 1: `PageTemplateSupport` (hoisted classify) + `PageCreationSupport` + `create_catalog_page` (T-38)

**Files:** Create `internal/PageTemplateSupport.java` (+ test) — hoist `classify(Resource template)` (returns kind + title + signal) and `resolveTemplate(ResourceResolver, String kind, String explicitTemplatePath)` from `SuggestTemplateForPageTypeTool`; refactor that tool to delegate (keep `SuggestTemplateForPageTypeToolTest` green). Create `internal/PageCreationSupport.java` (+ test) — `validatePageParent(resolver, argName, parentPath)` (under /content, exists) and a `createPage`-style seam contract note. Create `internal/tools/CreateCatalogPageTool.java` (+ test).

**Interfaces — Produces:** `PageTemplateSupport.classify/resolveTemplate`; `PageCreationSupport.validatePageParent`. Tool `create_catalog_page`; `writesContent()==true`; args `{ "parent": string (required, under /content, existing page/folder), "name": string (optional; else derived from title), "title": string (required), "rootCategoryId": string (required), "idType": string (optional, uid|urlPath, default uid), "showMainCategories": boolean (optional, default false), "template": string (optional; else auto-discover a kind=catalog template), "dryRun": boolean (optional, default false) }`; effect: validate parent + resolve/validate a `catalog` template → create the page → delegate to `ConfigureCatalogPageTool` with `{path, categoryUid:rootCategoryId, idType, showMainCategories}` → result `{"pagePath":"…","template":"…","rootCategoryId":"…","idType":"…","dryRun":bool}` (dryRun → would-be path + template, nothing created).

- [ ] **Step 1: failing tests.** classify hoist (product/category/catalog/empty-grid/title-fallback — port `SuggestTemplateForPageTypeToolTest` cases to `PageTemplateSupportTest`); `validatePageParent` (ok / not under /content / missing → IAE). Tool: parent + canned catalog template + create seam returning a canned catalog page (jcr:content resourceType = core catalogpage type) → assert page created at parent/name, `magentoRootCategoryId`/type written (readback), result shape; `dryRun:true` → seam+commit never called, would-be path returned; MANDATORY negatives: parent not under /content / missing → IAE, template not a catalog template → IAE.
- [ ] **Step 2: RED.**
- [ ] **Step 3: implement** helpers + refactor T-43 to delegate + tool (create seam; delegate to ConfigureCatalogPageTool).
- [ ] **Step 4: GREEN** (incl. still-green `SuggestTemplateForPageTypeToolTest`).
- [ ] **Step 5: green gate + commit** (`feat(mcp): add PageCreationSupport + create_catalog_page write tool`).

---

### Task 2: `create_specific_pdp` (T-39) + `create_specific_plp` (T-40)

**Files:** Create `internal/tools/CreateSpecificPdpTool.java` (+ test), `internal/tools/CreateSpecificPlpTool.java` (+ test).

**Interfaces:** Consume Task 1's helpers + the create seam pattern. 
- `create_specific_pdp`; args `{ parent, name?, title (required), skusOrUrlKeys: [string,…] (required, non-empty), template? (kind=product), dryRun? }`; create a **product** page → delegate to `BindPageToProductsTool` `{path, skusOrUrlKeys}`; result `{pagePath, template, boundSkus:[…], dryRun}`.
- `create_specific_plp`; args `{ parent, name?, title (required), categoryUid (required), urlPath (required), includesSubCategories? (default false), template? (kind=category), dryRun? }`; create a **category** page → delegate to `BindPageToCategoryTool` `{path, categoryUid, urlPath, includesSubCategories}`; result `{pagePath, template, categoryUid, urlPath, dryRun}`.

- [ ] Step 1 failing tests (each: parent + canned product/category template + create seam returning a canned product/category page whose jcr:content resourceType = core product/category page type so the delegated bind gate passes in-mock; assert binding props written via readback; `dryRun` creates nothing; empty skusOrUrlKeys / missing categoryUid|urlPath → IAE; parent/template negatives → IAE). Step 2 RED. Step 3 implement (reuse Task 1 helpers; delegate to the two bind tools). Step 4 GREEN. Step 5 commit (`feat(mcp): add create_specific_pdp + create_specific_plp write tools`).

---

### Task 3: `create_specific_pdp_for_category_tree` (T-41)

**Files:** Create `internal/tools/CreateSpecificPdpForCategoryTreeTool.java` (+ test).

**Interfaces:** Tool `create_specific_pdp_for_category_tree`; args `{ parent, name?, title (required), categoryUid (required), urlPath (required), includesSubCategories? (default false), template? (kind=product), dryRun? }`; create a **product** page → delegate to `BindProductPageToCategoryTreeTool` `{path, categoryUid, urlPath, includesSubCategories}` (writes `useForCategories` = plain urlPath, v2+); result `{pagePath, template, categoryUid, urlPath, dryRun}`.

- [ ] Step 1 failing tests (product template + create seam; assert `useForCategories`/`includesSubCategories` via the delegated tool's readback; dryRun creates nothing; missing categoryUid|urlPath → IAE; parent/template negatives → IAE). Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add create_specific_pdp_for_category_tree write tool`).

---

### Task 4: `scaffold_catalog_section` (T-42)

**Files:** Create `internal/tools/ScaffoldCatalogSectionTool.java` (+ test).

**Interfaces:** Tool `scaffold_catalog_section`; args `{ parent, name (required — the section root name), title? (default from name), rootCategoryId (required), idType? (default uid), template? (kind=catalog), dryRun? }`; effect: creates a whole catalog **section** = a catalog page (as T-38, wired to `rootCategoryId`) as the section root, plus example child pages (e.g. one example product PDP + one example category PLP under it, using auto-discovered product/category templates) so an author has a working starting tree. Result `{sectionPath, catalogPage, children:[{path,pageType}], dryRun}`. dryRun → the full would-be tree, nothing created.
> Composition note: implement by reusing Task 1–3 tool logic (delegate to the create tools or the shared helpers), not by re-coding page creation. Keep the example children minimal and clearly named (e.g. `example-product`, `example-category`). If a product/category template can't be auto-discovered, create just the catalog page + report which children were skipped (don't fail the whole section).

- [ ] Step 1 failing tests (parent + templates + seam → assert the catalog page + example children created with correct types/bindings via readback; dryRun creates nothing; missing rootCategoryId → IAE; parent negative → IAE; graceful skip when a child template is absent). Step 2 RED. Step 3 implement (compose Task 1–3). Step 4 GREEN. Step 5 commit (`feat(mcp): add scaffold_catalog_section write tool`).

---

## Self-Review
**Spec coverage:** T-38 → Task 1; T-39/T-40 → Task 2; T-41 → Task 3; T-42 → Task 4. Completes §9 (T-43 already shipped). This is the LAST Tier-3 plan; after it, the whole branch is ready for the final review + finishing.
**Type consistency:** all `writesContent()==true`; all reuse `PageCreationSupport`/`PageTemplateSupport` + the create seam; all **delegate** the binding to the shipped configure/bind tools (no divergence); all support `dryRun` + strict parent-path validation (approved decisions). Names/args match catalog §9 + §11 (T-38–42).
**Risk:** first tools that CREATE PAGES (largest blast radius) — dryRun + strict parent-path + pre-create template validation mitigate; delegation reuses proven, tested binding writes; real readback verifies. aem-mock can't resolve Venia proxy super-types, so `isResourceType` gates are exercised in-mock via canned core-typed pages + a create seam, and **live validation is the real proof**.
**Ordering:** Task 1 (helpers + first create tool, sets the seam+delegation pattern) first and reviewed hardest; Tasks 2–4 build on it.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §9 + `SuggestTemplateForPageTypeTool` + the four delegation-target tools + `CommerceWriteSupport` + this plan), TDD with the create seam + delegation, green gate, commit; task review each; whole-branch review at end; **live-validate** each page type on reversible Venia scratch content after review (create → verify page + component + binding → delete). Page creation is the highest-blast-radius work in the catalog — review Task 1 hardest and lean on live validation.
