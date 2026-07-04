# CIF Authoring Tier-3 Bulk Component Creation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the §4 bulk component-CREATE tools: `create_product_teasers` (T-17) and `create_product_carousels` (T-18). Adds a `createChild` helper to `CommerceWriteSupport`. First Tier-3 (content-creation) batch.

**Architecture:** WRITE tools (`writesContent()==true`, authoring endpoint), caller-resolver, fail-closed. They CREATE new `nt:unstructured` component nodes as unique-named children under a caller-supplied container `parentPath` (the grid path is template-dependent, so the author passes it explicitly — pair with the shipped `suggest_template_for_page_type`). Each created node gets `sling:resourceType` + the SAME property shapes the shipped `configure_productteaser_component`/`configure_productcarousel_component` write (so create and configure never diverge). Both tools support **`dryRun`** (preview the node names + props without creating), per the approved Tier-3 guardrail.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, Sling `ResourceResolver`/`Resource`/`ModifiableValueMap`/`ResourceUtil`, CIF `CombinedSku` (exported), aem-mock + Mockito + JUnit 4.

## Global Constraints
*(From `bundles/mcp/AGENTS.md`; §4 write-tool rules REQUIRED.)*
- AEM 6.5 / Java 8 / JDK 11. No `var`/`List.of`/records/switch-expr; `javax` never `jakarta`. Apache 2.0 header (`Licensed under the Apache License, Version 2.0`); explicit imports, no wildcards; Jackson only.
- Build/verify `mvn -pl bundles/mcp clean install` (clean); format `-Pformat-code process-classes`. JDK 11 env + mvn path as prior plans.
- New code under `…mcp.internal[.tools]`; no non-exported core `…components.internal.*` import (component RT literals + `selectionType` values redeclared, as the configure_* tools do).
- WRITE-tool security (§4): `writesContent()==true`; caller `ctx.getRequest().getResourceResolver()`; fail closed (IAE → -32000) on bad `parentPath` / non-existent / non-writable; MANDATORY negative test. Real readback verification of created nodes.
- **Tier-3 guardrail:** support `dryRun` (default false) — when true, compute + return the node names/props that WOULD be created, and do NOT `create`/`commit`.
- Conventional commits `feat(mcp): …`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer. Stage only touched files.

## Grounding (source-verified)
- **Placement:** grid/container path is template-dependent (Venia nests at `…/root/container/container`; examples use `…/root/responsivegrid`) — NO exported API returns "the" editable grid. So take an explicit `parentPath` = the target container resource. `Page.getContentResource()` only reaches `jcr:content`, not the grid.
- **Create:** `resolver.create(parentResource, nodeName, propsMap)` with `jcr:primaryType=nt:unstructured` + `sling:resourceType` + props, then caller `commit()` (mirrors `CommerceWriteSupport.writeComposite`'s `resolver.create`). No extra required props (no `cq:responsive`, no `componentGroup` — those aren't instance props). Unique sibling name: `<base>`, `<base>_1`, … — use `org.apache.sling.api.resource.ResourceUtil.createUniqueChildName(parent, base)` OR a loop appending `_<i>` until `parent.getChild(candidate)==null`.
- **RT literals + props (identical to the shipped configure_* tools):**
  - productteaser: `core/cif/components/commerce/productteaser/v1/productteaser`; `selection`=combinedSku (`CombinedSku.parse(sku).toString()`), optional `cta`/`ctaText`/`linkTarget`/`id`. No `selectionType`.
  - productcarousel: `core/cif/components/commerce/productcarousel/v1/productcarousel`; `selectionType`(`product`|`category`), `product`=`String[]` combinedSkus, `category`=UID, `productCount`=Integer.
- **Reuse:** `CommerceWriteSupport.putOrRemove`/`putOrRemoveArray` for prop application; `CombinedSku`; add a new `createChild` helper (none of the existing helpers create a uniquely-named top-level sibling — `writeComposite` deletes-then-recreates a fixed container, wrong semantics here).
- **Fail-closed:** `parentPath` under `/content/`, resource exists, adaptable to `ModifiableValueMap` (writable node); reject a `parentPath` that is itself a `cq:Page` node (author must pass a container inside the page). dryRun bypasses create but still validates.
- **Reference:** catalog §4 + §0.1 (node-creation pattern); `AGENTS.md` §4; the shipped `ConfigureProductTeaserComponentTool`/`ConfigureProductCarouselComponentTool` as prop templates.

## Shared conventions
Write-tool skeleton per §4. aem-mock: create a parent container resource (`context.create().resource(parentPath, "jcr:primaryType","nt:unstructured")`), call the tool, assert created children exist with the right `sling:resourceType` + props via readback; assert `dryRun` creates nothing; MANDATORY negative (parentPath under /apps, or non-existent, or a cq:Page). Per-task TDD loop.

---

### Task 1: `createChild` helper + `create_product_teasers` (T-17)

**Files:** Modify `internal/CommerceWriteSupport.java` (+ test) — ADD `public static Resource createChild(ResourceResolver resolver, Resource parent, String baseName, java.util.Map<String,Object> props) throws PersistenceException` (compute a unique child name under `parent`; ensure `jcr:primaryType=nt:unstructured` in props; `resolver.create(parent, name, props)`; return the new Resource; caller commits). Create `internal/tools/CreateProductTeasersTool.java` (+ test).

**Interfaces — Produces:** `CommerceWriteSupport.createChild(...)` (Task 2 reuses). Tool `create_product_teasers`; `writesContent()==true`; args `{ "parentPath": string (required — a container resource under /content), "skus": [string,…] (required, non-empty), "cta": string (optional, applied to all), "ctaText": string (optional, applied to all), "dryRun": boolean (optional, default false) }`; effect: for each sku, create a productteaser node (`selection`=combinedSku(sku) + shared cta/ctaText); result `{"parentPath":"…","created":[{"path":"…","selection":"…"}],"dryRun":bool}` (on dryRun, `created` lists the computed would-be node paths + props, nothing persisted). Gate: parentPath fail-closed (under /content, exists, writable, not a cq:Page).

- [ ] **Step 1: failing tests.** Helper `createChild` (creates uniquely-named child with props; second call with same base → `_1`). Tool: parent nt:unstructured container; `{parentPath, skus:["MJ01","MJ02"], cta:"add-to-cart"}` → assert 2 productteaser children created with `selection` (combinedSku) + `cta`, unique names, via readback; `dryRun:true` → returns would-be paths, `parent.getChildren()` empty; empty skus → IAE; MANDATORY negative: parentPath not under /content OR a `cq:Page` node → IAE; `writesContent()==true`.
- [ ] **Step 2: RED** (`-Dtest=CommerceWriteSupportTest,CreateProductTeasersToolTest`).
- [ ] **Step 3: implement** helper + tool (redeclare productteaser v1 RT literal + prop names; reuse `CombinedSku`, `putOrRemove` for building the props map or set directly).
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add createChild + create_product_teasers write tool`).

---

### Task 2: `create_product_carousels` (T-18)

**Files:** Create `internal/tools/CreateProductCarouselsTool.java` (+ test).

**Interfaces:** Consumes `CommerceWriteSupport.createChild`. Tool `create_product_carousels`; `writesContent()==true`; args `{ "parentPath": string (required — container under /content), "carousels": [ {"selectionType": string (optional, product|category), "product": [sku,…] (optional), "category": string (optional, UID), "productCount": int (optional)}, … ] (required, non-empty), "dryRun": boolean (optional, default false) }`; effect: for each carousel spec, create a productcarousel node with the given props (`product` combinedSku-normalized `String[]`; `selectionType` validated ∈{product,category} when provided; `productCount` integral-guarded); result `{"parentPath":"…","created":[{"path":"…"}],"dryRun":bool}`. Gate: same parentPath fail-closed as Task 1.
> Deviation note (from catalog §4's `(categoryUid, count)` sketch): a per-spec `carousels[]` array is more flexible (create different carousels, or repeat a spec N times) and mirrors the shipped `configure_productcarousel_component` shape — document this.

- [ ] Step 1 failing tests: parent container; `{parentPath, carousels:[{selectionType:"product", product:["MJ01","MJ02"], productCount:5},{selectionType:"category", category:"MjA="}]}` → assert 2 productcarousel children with correct props (product String[] combinedSku, category, productCount, selectionType) via readback; `dryRun:true` → nothing created; invalid selectionType → IAE; empty carousels → IAE; MANDATORY parentPath negative → IAE. Step 2 RED. Step 3 implement (reuse createChild + putOrRemove/putOrRemoveArray + CombinedSku; integral-guard productCount per AGENTS.md §8). Step 4 GREEN. Step 5 commit (`feat(mcp): add create_product_carousels write tool`).

---

## Self-Review
**Spec coverage:** T-17 → Task 1; T-18 → Task 2. Completes §4. (§6 CF writes + §9 page creation are the next Tier-3 plans.)
**Type consistency:** both `writesContent()==true`, reuse `CommerceWriteSupport.createChild` (Task 1) + `putOrRemove`/`putOrRemoveArray` + `CombinedSku`; RT literals + prop shapes identical to the shipped configure_* tools (create = create node + same props). Both support `dryRun`. Names match catalog §11 (T-17/T-18).
**Risk:** content-CREATION (Tier-3, higher blast radius) — dryRun + strict parentPath fail-closed mitigate; real readback verifies created nodes. Duplication of teaser/carousel prop-name literals with the configure_* tools is accepted (small; note for a later shared-apply extraction).
**Ordering:** Task 1 (createChild helper) before Task 2.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §4/§0.1 + `CommerceWriteSupport` + the configure_* teaser/carousel tools), TDD incl. dryRun + parentPath negative, green gate, commit; task review each; whole-branch review at end; live-validate on reversible scratch content after review. Task 1 (helper + first create tool) sets the create pattern — review it hardest.
