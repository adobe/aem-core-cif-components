# CIF Authoring Tier-2 Component Writes (Batch B) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Finish the §2 component-config WRITE tools: `configure_productcarousel_component` (T-02), `configure_featuredcategorylist_component` (T-05/T-06 — one tool, both resource types), and extend the shipped `configure_productlist_component` to the full productlist dialog (T-03). Adds two multifield helpers to `CommerceWriteSupport` (array property + composite child-node multifield).

**Architecture:** WRITE tools (`writesContent()==true`, authoring endpoint), caller-resolver, fail-closed, super-type-aware `isResourceType` gate against **redeclared** literals — same pattern as Batch A (`CommerceWriteSupport` + `Configure*Tool` in `internal/tools`). New capability: writing a multi-valued `String[]` property and a **composite multifield** (child nodes `item0..itemN` under a container child), via two new `CommerceWriteSupport` helpers.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, Sling `ModifiableValueMap`/`ResourceResolver`/`Resource` (child-node creation via `resolver.create(...)` or `ResourceUtil`), CIF `CombinedSku` (exported), aem-mock + Mockito + JUnit 4.

## Global Constraints
*(From `bundles/mcp/AGENTS.md`; §4 write-tool rules are REQUIRED reading.)*
- AEM 6.5 / Java 8 / JDK 11. No `var`/`List.of`/records/switch-expressions; `javax` never `jakarta`. Apache 2.0 header (`Licensed under the Apache License, Version 2.0`); explicit imports, no wildcards; Jackson only.
- Build/verify `mvn -pl bundles/mcp clean install` (clean); format `-Pformat-code process-classes` first. JDK 11 env + mvn path as in prior plans.
- New code under `…mcp.internal[.tools]`; no non-exported core `…components.internal.*` import (component RESOURCE_TYPE literals REDECLARED, matched via `Resource.isResourceType`).
- WRITE-tool security (§4): `writesContent()==true`; caller `ctx.getRequest().getResourceResolver()`; fail closed (IAE → -32000) on bad path / non-CIF type / not modifiable; MANDATORY non-CIF-type negative test per tool. Optional string props use `CommerceWriteSupport.putOrRemove` (isBlank → remove).
- Conventional commits `feat(mcp):`/`refactor(mcp):`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer. Stage only touched files.

## Grounding (source-verified — from the Tier-2 §2 research pass)
- **Reuse:** `CommerceWriteSupport` (Batch A) — `resolveComponent(resolver, argName, path, allowedTypes)`, `mutableMap(resource, argName)`, `resolvePageContent(...)`, `putOrRemove(map, name, value)`. Extend it here (additive; do NOT change existing methods). `CombinedSku` (exported, `models.common`) for combinedSku values (separator `#`). The `ProductCollection`/`ProductList` `PN_*`/`NN_*` constants ARE exported (import them where useful) — but component `*Impl.RESOURCE_TYPE` are internal (redeclare literals).
- **RESOURCE_TYPE literals (redeclare):** productcarousel `core/cif/components/commerce/productcarousel/v1/productcarousel`; productlist `core/cif/components/commerce/productlist/v1/productlist` + `…/v2/productlist`; featuredcategorylist `core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist` AND categorycarousel `core/cif/components/commerce/categorycarousel/v1/categorycarousel` (the SAME `FeaturedCategoryListImpl` model backs both).
- **productcarousel props** (`ProductCarouselImpl`): `selectionType` (`product`|`category`), `product` (`String[]` of combinedSkus), `category` (single UID), `productCount` (int). (`enableAddToCart`/`enableAddToWishList` are STYLE — do NOT expose.)
- **productlist full dialog** (`ProductListImpl` v1/v2 + `ProductCollection`): `category` (already shipped), `showTitle` (`ProductList.PN_SHOW_TITLE`), `showImage` (`ProductList.PN_SHOW_IMAGE`), `pageSize` (`ProductCollection.PN_PAGE_SIZE`, int), `defaultSortField` (`PN_DEFAULT_SORT_FIELD`), `defaultSortOrder` (`PN_DEFAULT_SORT_ORDER`, `asc`|`desc`), `fragments` (composite multifield). Verify each prop's value TYPE against source (boolean vs string, int) before writing.
- **Composite multifield structure:** a container child node named `items` (featuredcategorylist) / `fragments` (productlist), whose children (`item0`,`item1`,… — names arbitrary, model iterates in order) are `nt:unstructured` carrying: featuredcategorylist item → `categoryId` (UID) + optional `asset` (path); productlist fragment → `fragmentLocation`, `fragmentPage`, `fragmentCssClass` (redeclare these three literals — the `ProductListImpl` PN_* for them are internal). Model reads via `resource.getChild("items"|"fragments").getChildren()`.
- **featuredcategorylist other props:** `jcr:title`, `titleType`, `linkTarget`.
- **Reference:** catalog §2.2/§2.3/§2.5/§2.6; `AGENTS.md` §4; Batch A tools as templates.

## Shared conventions
Write-tool skeleton per §4. aem-mock: `context.create().resource(path, "sling:resourceType", <coreLiteral>, …)`; assert persisted scalars/arrays and child-node structure via `resolver.getResource(...)` readback; MANDATORY non-CIF negative test. Per-task TDD: failing test → RED → implement → GREEN → green gate → commit.

---

### Task 1: multifield-array helper + `configure_productcarousel_component` (T-02)

**Files:** Modify `internal/CommerceWriteSupport.java` (+ its test) — ADD `public static void putOrRemoveArray(ModifiableValueMap map, String propertyName, java.util.List<String> values)` (null/empty → `remove`; else `map.put(name, values.toArray(new String[0]))`). Create `internal/tools/ConfigureProductCarouselComponentTool.java` (+ test).

**Interfaces — Produces:** `CommerceWriteSupport.putOrRemoveArray(...)` (Task 3 also uses it if needed). Tool `configure_productcarousel_component`; `writesContent()==true`; args `{ "path": string (required), "selectionType": string (optional: `product`|`category`), "product": [string,…] (optional — SKUs, combinedSku-encoded on write), "category": string (optional — UID), "productCount": int (optional) }`; effect: put/remove each provided prop (`product` via `putOrRemoveArray` with each entry `CombinedSku`-normalized; `selectionType`/`category` via `putOrRemove`; `productCount` as int, remove when absent); result `{"path":"…","updated":true}`. Gate: productcarousel v1.
> Overlap note: the shipped `configure_productlist_component` also accepts productcarousel v1 (category-only) — this fuller tool is preferred for carousels; document it. (Not narrowing the shipped tool's gate here to avoid churn.)

- [ ] Step 1 failing tests: helper `putOrRemoveArray` (empty → remove; values → String[]); tool: create a productcarousel v1 resource, set `{selectionType:"product", product:["MJ01","MJ02"], productCount:5}`, assert persisted `selectionType`, `product` String[] (combinedSku-normalized), `productCount`; validate `selectionType` ∈ {product,category} else IAE; MANDATORY non-CIF negative → IAE. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add putOrRemoveArray + configure_productcarousel_component write tool`).

---

### Task 2: composite-multifield helper + `configure_featuredcategorylist_component` (T-05/T-06)

**Files:** Modify `internal/CommerceWriteSupport.java` (+ test) — ADD `public static void writeComposite(ResourceResolver resolver, Resource parent, String childName, java.util.List<java.util.Map<String,Object>> items)` — removes an existing `parent/childName` node, and when `items` non-empty creates `parent/childName` (`nt:unstructured`) with children `item0..itemN` (`nt:unstructured`) each carrying its map's props. Create `internal/tools/ConfigureFeaturedCategoryListComponentTool.java` (+ test).

**Interfaces — Produces:** `CommerceWriteSupport.writeComposite(...)` (Task 3 reuses for `fragments`). Tool `configure_featuredcategorylist_component`; `writesContent()==true`; args `{ "path": string (required), "items": [ {"categoryId": string (required, UID), "asset": string (optional, /content/dam path)}, … ] (required), "title": string (optional → `jcr:title`), "titleType": string (optional), "linkTarget": string (optional) }`; effect: rewrite the `items` composite multifield from `items[]`; put/remove `jcr:title`/`titleType`/`linkTarget`; result `{"path":"…","itemCount":N,"updated":true}`. Gate: featuredcategorylist v1 OR categorycarousel v1.

- [ ] Step 1 failing tests: helper `writeComposite` (creates item0/item1 with props; empty clears the container; re-write replaces prior children); tool: create a featuredcategorylist v1 resource, set `{items:[{categoryId:"MjA="},{categoryId:"Mjk=","asset":"/content/dam/x.jpg"}], title:"Shop"}`, assert `items/item0/categoryId`, `items/item1/asset`, `jcr:title` via readback; also test the categorycarousel v1 RT is accepted; MANDATORY non-CIF negative → IAE; missing/empty `items` → decide (IAE or clear) and test. Step 2 RED. Step 3 implement. Step 4 GREEN. Step 5 commit (`feat(mcp): add writeComposite + configure_featuredcategorylist_component write tool`).

---

### Task 3: extend `configure_productlist_component` to the full dialog (T-03)

**Files:** Modify `internal/tools/ConfigureProductListComponentTool.java` (+ its test). Behavior-preserving for the existing `category` write; ADDITIVE new optional props. Reuse `CommerceWriteSupport.putOrRemove`/`writeComposite`.

**Interfaces:** Consumes Task 2's `writeComposite`. Extend tool `configure_productlist_component` args to `{ "path": string (required), "categoryUid": string (optional — existing), "showTitle": boolean (optional), "showImage": boolean (optional), "pageSize": int (optional), "defaultSortField": string (optional), "defaultSortOrder": string (optional: `asc`|`desc`), "fragments": [ {"fragmentLocation": string, "fragmentPage": string, "fragmentCssClass": string}, … ] (optional) }`; effect: keep writing `category` (unchanged); additionally put/remove the new props (verify value TYPES against `ProductListImpl`/`ProductCollection`); `fragments` via `writeComposite(resolver, component, "fragments", …)`; result extends existing shape with what changed + `updated`. Gate unchanged (productlist v1/v2 + productcarousel v1 — but the new productlist-specific props are no-ops on carousel; document).
> This MODIFIES a shipped + live-validated tool. Keep the existing `categoryUid` behavior and all existing tests passing; only ADD. Re-run the existing `ConfigureProductListComponentToolTest` plus new cases.

- [ ] Step 1 failing tests: existing category behavior still passes; NEW: set `{path, showTitle:true, pageSize:12, defaultSortField:"price", defaultSortOrder:"desc"}` → assert persisted (correct types); set `{path, fragments:[{fragmentLocation:"loc",fragmentPage:"/content/x",fragmentCssClass:"c"}]}` → assert `fragments/item0/*`; `defaultSortOrder` not asc/desc → IAE; non-CIF negative still IAE. Step 2 RED. Step 3 implement (additive; verify prop types). Step 4 GREEN (existing + new tests). Step 5 commit (`feat(mcp): extend configure_productlist_component with full productlist dialog`).

---

## Self-Review
**Spec coverage:** T-02 → Task 1; T-05/T-06 → Task 2 (one tool, both RTs); T-03 → Task 3 (extends shipped tool). Completes §2 config-write tools. (§8 bind/unbind + §7 T-28 urlPath are the next plan; Tier-3 after.)
**Type consistency:** all `writesContent()==true`, reuse `CommerceWriteSupport` (`resolveComponent`/`mutableMap`/`putOrRemove` + new `putOrRemoveArray`/`writeComposite`); RT literals redeclared; `CombinedSku` for carousel product entries. Names match catalog §11 (T-02/T-05/T-06/T-03).
**Risk:** Task 3 modifies a shipped tool — additive + keep existing tests green (re-validate live after). Multifield child-node writing is the new mechanism — helper tested directly + via tools. `enableAddToCart/WishList` not exposed (style).
**Ordering:** Task 1 (putOrRemoveArray) and Task 2 (writeComposite) add helpers each with a consumer; Task 3 depends on Task 2's `writeComposite`.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §2 + Batch A tools + `CommerceWriteSupport`), TDD incl. non-CIF negative, green gate, commit; task review each; whole-branch review at end; live-validate the batch (incl. multifield child nodes) against Venia after review. Task 3 (shipped-tool change) and the multifield helpers deserve the hardest review.
