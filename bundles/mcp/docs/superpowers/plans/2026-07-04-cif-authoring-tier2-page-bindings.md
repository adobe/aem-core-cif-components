# CIF Authoring Tier-2 Page Bindings — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the §8 specific-PDP/PLP page-BINDING write tools (`bind_page_to_products` T-29, `bind_page_to_category` T-30, `bind_product_page_to_category_tree` T-31, `unbind_specific_page` T-32) and extend the shipped `configure_catalog_page` with a `urlPath` idType option (§7 T-28). Completes Tier-2.

**Architecture:** WRITE tools (`writesContent()==true`, authoring endpoint), caller-resolver, fail-closed, operating on a CIF **structure page's** `jcr:content` via `CommerceWriteSupport.resolvePageContent(...)` (added in the component-writes-A plan). They set/clear the `SpecificPageStrategy` binding fields (`selectorFilter`/`selectorFilterType`/`includesSubCategories`/`useForCategories`) whose exact names/formats are already encoded in this branch's `internal/SpecificPageRouting.java` (Plan 2). The §8 diagnostics (`explain_page_resolution`, `list_specific_pages`, `validate_selector_filter_format`, etc.) already shipped — these are their write counterparts.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, Sling `ModifiableValueMap`/`ResourceResolver`, `CommerceWriteSupport`, aem-mock + Mockito + JUnit 4.

## Global Constraints
*(From `bundles/mcp/AGENTS.md`; §4 write-tool rules REQUIRED.)*
- AEM 6.5 / Java 8 / JDK 11. No `var`/`List.of`/records/switch-expr; `javax` never `jakarta`. Apache 2.0 header (`Licensed under the Apache License, Version 2.0`); explicit imports, no wildcards; Jackson only.
- Build/verify `mvn -pl bundles/mcp clean install` (clean); format `-Pformat-code process-classes`. JDK 11 env + mvn path as prior plans.
- New code under `…mcp.internal[.tools]`; no non-exported core `…components.internal.*` import (binding-field names + structure-page RT literals REDECLARED — reuse the literals already declared in `internal/SpecificPageRouting.java`, which redeclared them from `SpecificPageStrategy`).
- WRITE-tool security (§4): `writesContent()==true`; caller `ctx.getRequest().getResourceResolver()`; fail closed (IAE → -32000) on bad path / not-a-Page / non-CIF-structure-page `jcr:content`; MANDATORY non-CIF negative test per tool. Use `CommerceWriteSupport.resolvePageContent(resolver, "path", path, STRUCTURE_PAGE_TYPES)` + `putOrRemove`/`putOrRemoveArray`; real per-property readback `updated`.
- Conventional commits `feat(mcp):`/`refactor(mcp):`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer. Stage only touched files.

## Grounding (source-verified — from Plan 2's SpecificPageRouting + Plan 4/5)
- **Binding field names/formats** (verify against `internal/SpecificPageRouting.java` constants, which mirror the non-exported `SpecificPageStrategy`): `selectorFilter` (String[]); `selectorFilterType` (default `"uidAndUrlPath"`); `includesSubCategories` (boolean); `useForCategories` (String[]); `uid|urlPath` pipe format (separator `"|"`). Category `selectorFilter` entries are `uid|urlPath`; **product** `selectorFilter` entries are plain slugs/url-keys (selectionId=`slug`). `useForCategories` entries are `uid|urlPath` (same parse as category selectorFilter — VERIFY against `SpecificPageStrategy`/`SpecificPageRouting`).
- **Structure page RT literals** (redeclare; from Plan 4): `core/cif/components/structure/page/v1/page`, `…/v2/page`, `…/v3/page`. Gate the page's `jcr:content` via `resolvePageContent`.
- **Version note (catalog §8):** `useForCategories` is v2+ only; product-page `includesSubCategories` applicability is v2+ (the field exists v1/v2/v3 for category pages). The bind tools may write regardless of version (the render condition governs UI), but document that `useForCategories`/product-`includesSubCategories` only take effect on v2+.
- **Type ambiguity (Plan 2 finding):** the structure page RT does NOT distinguish product vs category pages; these tools gate on structure/page v* and write the operation's fields — the author is responsible for using the right tool on the right page type. Document this per tool.
- **`configure_catalog_page` (shipped)** writes `magentoRootCategoryId` + `magentoRootCategoryIdType=uid` (hardcoded) + `showMainCategories`. T-28 = add an optional `idType` arg (`uid`|`urlPath`, default `uid` to preserve behavior).
- **Reuse:** `CommerceWriteSupport` (`resolvePageContent`, `putOrRemove`, `putOrRemoveArray`); Plan 4/5 write tools as templates; `internal/SpecificPageRouting.java` for the constants + `uid|urlPath` build/parse.
- **Reference:** catalog §8 (mechanism/§8.1 constants) + §7; `AGENTS.md` §4.

## Shared conventions
Write-tool skeleton per §4; resolve the page's `jcr:content` via `resolvePageContent`; write binding fields; `commit()`; real per-property readback `updated`. aem-mock tests: create a `cq:Page` whose `jcr:content` `sling:resourceType` is a structure-page core literal; assert persisted binding fields via readback; MANDATORY non-CIF negative (non-page or non-CIF jcr:content). Per-task TDD loop.

---

### Task 1: `bind_page_to_products` (T-29)

**Files:** Create `internal/tools/BindPageToProductsTool.java` (+ test).
**Interfaces:** Consumes `CommerceWriteSupport.resolvePageContent`/`putOrRemoveArray`. Tool `bind_page_to_products`; `writesContent()==true`; args `{ "path": string (required — a product page), "skusOrUrlKeys": [string,…] (required — plain slugs/url-keys/SKUs) }`; effect: write `selectorFilter` = `String[]` of the given entries (plain, product selectionId=slug — do NOT pipe-encode); result `{"path":"…","selectorFilter":[…],"updated":true}`. Gate: structure page v1/v2/v3 (via `resolvePageContent`). Empty array → clear `selectorFilter`.

- [ ] Step 1 failing test: create a cq:Page with structure-page-v3 jcr:content; `{path, skusOrUrlKeys:["jillian-top","vitalia-top"]}` → assert persisted `selectorFilter` String[] on jcr:content (readback); empty array clears; MANDATORY non-CIF negative (non-page or non-CIF jcr:content) → IAE; `writesContent()==true`. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add bind_page_to_products write tool`).

---

### Task 2: `bind_page_to_category` (T-30)

**Files:** Create `internal/tools/BindPageToCategoryTool.java` (+ test).
**Interfaces:** Consumes `resolvePageContent`/`putOrRemove`/`putOrRemoveArray` + `SpecificPageRouting`'s `uid|urlPath` format. Tool `bind_page_to_category`; `writesContent()==true`; args `{ "path": string (required — a category page), "categoryUid": string (required), "urlPath": string (required), "includesSubCategories": boolean (optional, default false) }`; effect: write `selectorFilter` = `String[]{ categoryUid + "|" + urlPath }`, `selectorFilterType` = `"uidAndUrlPath"`, `includesSubCategories` boolean; result `{"path":"…","selectorFilter":[…],"includesSubCategories":bool,"updated":true}`. Gate: structure page v1/v2/v3.

- [ ] Step 1 failing test: create structure-page jcr:content; `{path, categoryUid:"MjA=", urlPath:"venia-tops", includesSubCategories:true}` → assert `selectorFilter==["MjA=|venia-tops"]`, `selectorFilterType=="uidAndUrlPath"`, `includesSubCategories==true` (boolean) via readback; missing categoryUid/urlPath → IAE; MANDATORY non-CIF negative → IAE. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add bind_page_to_category write tool`).

---

### Task 3: `bind_product_page_to_category_tree` (T-31)

**Files:** Create `internal/tools/BindProductPageToCategoryTreeTool.java` (+ test).
**Interfaces:** Consumes `resolvePageContent`/`putOrRemoveArray`/`putOrRemove`. Tool `bind_product_page_to_category_tree`; `writesContent()==true`; args `{ "path": string (required — a PRODUCT page, v2+), "categoryUid": string (required), "urlPath": string (required), "includesSubCategories": boolean (optional, default false) }`; effect: write `useForCategories` = `String[]{ categoryUid + "|" + urlPath }` (VERIFY the useForCategories entry format against `SpecificPageStrategy`/`SpecificPageRouting` — if it's plain urlPath rather than uid|urlPath, use that), `includesSubCategories` boolean; result `{"path":"…","useForCategories":[…],"includesSubCategories":bool,"updated":true}`. Gate: structure page v1/v2/v3 (document `useForCategories` only takes effect on v2+).

- [ ] Step 1 failing test: create structure-page jcr:content; `{path, categoryUid:"MjA=", urlPath:"venia-tops", includesSubCategories:true}` → assert persisted `useForCategories` (correct format, verified) + `includesSubCategories` via readback; missing args → IAE; MANDATORY non-CIF negative → IAE. Step 2 RED. Step 3 implement (verify useForCategories format against source first). Step 4 GREEN. Step 5 commit (`feat(mcp): add bind_product_page_to_category_tree write tool`).

---

### Task 4: `unbind_specific_page` (T-32)

**Files:** Create `internal/tools/UnbindSpecificPageTool.java` (+ test).
**Interfaces:** Consumes `resolvePageContent` + `ModifiableValueMap.remove`. Tool `unbind_specific_page`; `writesContent()==true`; args `{ "path": string (required — a structure page) }`; effect: REMOVE all binding fields from the page's `jcr:content`: `selectorFilter`, `selectorFilterType`, `includesSubCategories`, `useForCategories`; result `{"path":"…","cleared":[…names actually removed…],"updated":true}`. Gate: structure page v1/v2/v3. Idempotent (removing absent props is a no-op → `cleared:[]`, still `updated:true` or `false` — pick and document).

- [ ] Step 1 failing test: create a structure page whose jcr:content HAS `selectorFilter`+`includesSubCategories`; call `{path}` → assert all four binding props absent on readback + `cleared` lists what was removed; calling on an already-unbound page → no error; MANDATORY non-CIF negative → IAE. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add unbind_specific_page write tool`).

---

### Task 5: extend `configure_catalog_page` with `urlPath` idType (T-28)

**Files:** Modify `internal/tools/ConfigureCatalogPageTool.java` (+ its test). ADDITIVE + behavior-preserving (default remains `uid`).
**Interfaces:** Extend tool `configure_catalog_page` args from `{path, categoryUid, showMainCategories?}` to also accept `{ "idType": string (optional, `uid`|`urlPath`, default `uid`) }`; effect: write `magentoRootCategoryId` = categoryUid (unchanged) and `magentoRootCategoryIdType` = the given idType (validate ∈ {uid,urlPath}; default `uid` preserves current behavior), `showMainCategories` unchanged; result unchanged shape + reflect idType. Gate unchanged (catalog page v1/v3). 
> Keep ALL existing `ConfigureCatalogPageToolTest` cases passing unmodified; only ADD the idType handling + tests. Consider adopting `CommerceWriteSupport.resolvePageContent` here too (the earlier review noted this tool's inline page-resolution is weaker) — OPTIONAL; if you do, keep behavior identical and all existing tests green.

- [ ] Step 1 failing test: existing cases still pass; NEW: `{path, categoryUid:"MjA=", idType:"urlPath"}` → assert `magentoRootCategoryIdType=="urlPath"`; default (no idType) → `"uid"` (existing behavior); invalid idType → IAE. Step 2 RED. Step 3 implement (additive). Step 4 GREEN (existing + new). Step 5 commit (`feat(mcp): add urlPath idType option to configure_catalog_page`).

---

## Self-Review
**Spec coverage:** T-29→Task 1; T-30→Task 2; T-31→Task 3; T-32→Task 4; T-28→Task 5. Completes Tier-2 (§2 + §7 + §8 config writes). Tier-3 (creates) is next.
**Type consistency:** all `writesContent()==true`, reuse `CommerceWriteSupport.resolvePageContent`/`putOrRemove`/`putOrRemoveArray`; binding-field literals reused from `SpecificPageRouting`; real per-property readback `updated`. Names match catalog §11 (T-29/30/31/32/28).
**Risk:** Task 5 modifies a shipped+live-validated tool (additive; keep existing tests). `useForCategories` entry format must be verified against source (Task 3). Product-vs-category page type isn't RT-distinguishable — documented per tool.
**Ordering:** Tasks 1-4 independent (all reuse existing helpers); Task 5 independent. Task 3 must verify the useForCategories format before writing.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §8/§7 + `SpecificPageRouting` constants + `CommerceWriteSupport` + Plan 4/5 tools), TDD incl. non-CIF negative, green gate, commit; task review each; whole-branch review at end; live-validate the batch on reversible scratch pages after review. Task 5 (shipped-tool change) and Task 3 (format verification) deserve the hardest review.
