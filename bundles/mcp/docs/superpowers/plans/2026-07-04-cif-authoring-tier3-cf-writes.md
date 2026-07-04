# CIF Authoring Tier-3 Content-Fragment Writes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ship the §6 commerce Content-Fragment WRITE tools: `update_commerce_content_fragment_field` (T-23) and `create_commerce_content_fragment` (T-24). Adds a `resolveContentFragment` helper to `CommerceWriteSupport`.

**Architecture:** WRITE tools (`writesContent()==true`, authoring endpoint), caller-resolver, fail-closed. They use the `com.adobe.cq.dam.cfm` write API (from the AEM uber-jar — 6.5-safe). T-23 resolves a CF by `fragmentPath` and sets one element's value (master by default, or a named variation); T-24 creates a new CF from a model and seeds its fields. No auto-publish — end at `resolver.commit()`.

**Tech Stack:** Java 8 / JDK 11, OSGi DS, Jackson, `com.adobe.cq.dam.cfm` (`ContentFragment`/`ContentElement`/`FragmentData`/`ContentFragmentManager`/`ContentVariation`), `com.day.cq.commons.jcr.JcrConstants`/`DamConstants`, aem-mock + Mockito + JUnit 4.

## ⚠️ Critical risk: the CF WRITE API is compile-to-confirm
The `com.adobe.cq.dam.cfm` READ API (`adaptTo(ContentFragment.class)`, `getElement`, `getValue`) is already used in-repo (`GetCommerceContentFragmentTool`, `AssociatedContentSupport`). The WRITE methods (`FragmentData.setValue(Object)`, `ContentElement.setValue(FragmentData)`, `ContentElement.setContent(String,String)`, `ContentElement.getVariation(String)`, `ContentFragmentManager.createFragment(...)`) have **no existing CIF caller** and are NOT in the IDE index. **Every task MUST verify these signatures compile against the uber-jar EARLY (a 2-minute spike: write the call, `mvn -pl bundles/mcp test-compile`).** If a signature differs from what this plan assumes, adapt to the actual uber-jar signature (the green gate enforces correctness); if it's fundamentally unavailable, STOP and escalate.

## Global Constraints
*(From `bundles/mcp/AGENTS.md`; §4 write-tool rules REQUIRED.)*
- AEM 6.5 / Java 8 / JDK 11. No `var`/`List.of`/records/switch-expr; `javax` never `jakarta`. Apache 2.0 header (`Licensed under the Apache License, Version 2.0`); explicit imports, no wildcards; Jackson only.
- Build/verify `mvn -pl bundles/mcp clean install` (clean); format `-Pformat-code process-classes`. JDK 11 env + mvn path as prior plans.
- New code under `…mcp.internal[.tools]`; no non-exported core `…components.internal.*` import (`com.adobe.cq.dam.cfm.*`, `DamConstants`, `JcrConstants` are exported/uber-jar).
- WRITE-tool security (§4): `writesContent()==true`; caller `ctx.getRequest().getResourceResolver()`; fail closed (IAE → -32000); MANDATORY negative test. Real readback verification. **No auto-publish** (no Replicator/publish API — end at `commit()`).
- Conventional commits `feat(mcp): …`, one logical change per commit, `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>` trailer.

## Grounding (source-verified)
- **CF write surface** (verify signatures by compiling): `ContentFragment cf = resource.adaptTo(ContentFragment.class)`; `ContentElement el = cf.getElement(name)`; `FragmentData d = el.getValue(); d.setValue(Object); el.setValue(d)`; richtext (when `d.getContentType()` is `text/html`) → `el.setContent(html, "text/html")`; type-gate scalar writes with `d.getDataType()` + `d.isTypeSupported(Class)` before `setValue` (avoids `ContentFragmentException`). Named variation: `ContentVariation v = el.getVariation(name)` (returns `null` if it doesn't exist → do NOT auto-create), then `v.setValue(d)`/`v.setContent(...)`.
- **T-23 resolve/gate:** resolve `fragmentPath` → `resource.adaptTo(ContentFragment.class)`; fail closed if blank / not under `/content/dam` / not found / `adaptTo(ContentFragment.class)==null` (a non-CF asset yields null — precise CF gate). Variation default `master` = the base element value; unknown named variation → IAE.
- **T-24 create:** `resolver.adaptTo(ContentFragmentManager.class).createFragment(Resource parent, String modelPath, String title, String name)` (path-based overload; model passed as its path — verify overload by compiling). Then seed `linkElement` (the model field holding the SKU/UID) + other `fields` via the write API; `commit()`. `parentPath` default `/content/dam` (`DamConstants.MOUNTPOINT_ASSETS`); fail closed on parentPath not under `/content/dam` / model not resolvable.
- **Read/write master caveat** (catalog §6): the SDK CF lookup only queries the `master` variation — a value written to a non-master variation is INVISIBLE to `get_commerce_content_fragment`/associated-content tools. Surface this in T-23's `description()`.
- **Reuse:** add `resolveContentFragment(resolver, argName, path)` to `CommerceWriteSupport` (mirror `resolveComponent`/`resolveContainer` shape; gate `/content/dam` + non-null CF). `AssociatedContentSupport.resolveSingleContentFragment` may be used by T-24 for an optional duplicate-check only. `GetCommerceContentFragmentTool` shows the read-side CF/element iteration to mirror.
- **Reference:** catalog §6 (lines 528-585); `AGENTS.md` §4; `GetCommerceContentFragmentTool`/`AssociatedContentSupport` (CF API in use); `CreateProductTeasersTool` (dryRun Tier-3 precedent).

## Testing note (IMPORTANT — CF writes + aem-mock)
aem-mock's DAM Content-Fragment support is limited/uncertain for writes. Use a **test seam**: put the actual CF resolution + element write behind a `protected` method the unit test overrides with a **Mockito `ContentFragment`/`ContentElement`/`FragmentData`** (verify the tool calls `setValue`/`setContent`/`getVariation` correctly, branches on richtext/type, and fails closed) — mirroring how `GetCommerceContentFragmentTool` seams its fetch. The REAL JCR persistence is proven by **live validation** (deploy + create a CF via T-24, update via T-23, verify, delete) after review, not by aem-mock. Document the seam clearly.

---

### Task 1: `resolveContentFragment` helper + `update_commerce_content_fragment_field` (T-23)

**Files:** Modify `internal/CommerceWriteSupport.java` (+ test) — ADD `public static Resource resolveContentFragment(ResourceResolver resolver, String argName, String path)` (fail closed: blank / not under `/content/dam` / not found / `adaptTo(ContentFragment.class)==null` → IAE; return the Resource). Create `internal/tools/UpdateCommerceContentFragmentFieldTool.java` (+ test).

**Interfaces — Produces:** `CommerceWriteSupport.resolveContentFragment(...)`. Tool `update_commerce_content_fragment_field`; `writesContent()==true`; args `{ "fragmentPath": string (required — a CF under /content/dam), "elementName": string (required — the CF model field), "value": string (required — new value; for a multi-value field, accept a JSON array too), "variation": string (optional — default writes the master/base value; a named variation must already exist) }`; effect: set the element's value on master (or the named variation), branching to `setContent(value,"text/html")` for a richtext element (`FragmentData.getContentType()==text/html`) else `setValue` (type-gated); `commit()`; readback; result `{"fragmentPath":"…","elementName":"…","variation":"master|<name>","updated":true}`. NO dryRun required (single-field update; add only if trivial). NO auto-publish.

- [ ] **Step 1: SPIKE + failing test.** First confirm the write signatures compile (a throwaway `el.setValue(el.getValue())` etc., `mvn -pl bundles/mcp test-compile`). Then write the failing test using a Mockito `ContentFragment`/`ContentElement`/`FragmentData` behind the tool's `protected` CF-resolve seam: assert a plain-text field write calls `setValue`; a richtext field (contentType text/html) calls `setContent(...,"text/html")`; a named variation routes to `getVariation(name)`; an unknown variation (`getVariation` returns null) → IAE. Negative: `fragmentPath` not under `/content/dam` or not a CF → IAE.
- [ ] **Step 2: RED.**
- [ ] **Step 3: implement** helper (gate `/content/dam` + `adaptTo(ContentFragment.class)`) + tool (branch richtext/type; master vs variation; fail-closed).
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add resolveContentFragment + update_commerce_content_fragment_field write tool`).

---

### Task 2: `create_commerce_content_fragment` (T-24)

**Files:** Create `internal/tools/CreateCommerceContentFragmentTool.java` (+ test).

**Interfaces:** Consumes `CommerceWriteSupport` + `ContentFragmentManager`. Tool `create_commerce_content_fragment`; `writesContent()==true`; args `{ "identifier": string (required — the SKU/UID to seed), "type": "product"|"category" (required), "modelPath": string (required — CF model path), "linkElement": string (required — the model field that holds the identifier), "fields": { "<field>": <value>, … } (optional — other fields to seed), "parentPath": string (optional — default /content/dam), "name": string (optional — node name; else derive from identifier), "dryRun": boolean (optional, default false) }`; effect: validate parentPath (under /content/dam) + model resolves; `createFragment(parent, modelPath, title, name)`; seed `linkElement`=identifier + `fields` via the write API; `commit()`; readback; result `{"fragmentPath":"…","modelPath":"…","seeded":[…field names…],"dryRun":bool}`. dryRun → compute the would-be fragment path + seeded fields, create NOTHING. Optionally pre-check via `AssociatedContentSupport.resolveSingleContentFragment` and report if one already exists. NO auto-publish.

- [ ] **Step 1: SPIKE + failing test.** Confirm `ContentFragmentManager.createFragment(...)` overload compiles. Test with a Mockito `ContentFragmentManager` (behind a seam) returning a Mockito `ContentFragment`: assert `createFragment` called with the right parent/modelPath/name, then `linkElement`+fields seeded via `setValue`; `dryRun:true` → no `createFragment`/`commit` call, returns would-be path; negatives: parentPath not under /content/dam → IAE, invalid `type` → IAE, missing required args → IAE.
- [ ] **Step 2: RED.**
- [ ] **Step 3: implement** (validate parentPath/model; createFragment via a `protected` seam; seed fields; dryRun early-return).
- [ ] **Step 4: GREEN.**
- [ ] **Step 5: green gate + commit** (`feat(mcp): add create_commerce_content_fragment write tool`).

---

## Self-Review
**Spec coverage:** T-23 → Task 1; T-24 → Task 2. Completes §6 (get_commerce_content_fragment T-22 already shipped). §9 page creation is the last Tier-3 plan.
**Type consistency:** both `writesContent()==true`; T-23 uses new `resolveContentFragment`; both use the `com.adobe.cq.dam.cfm` write API behind a test seam; real readback. Names match catalog §11 (T-23/T-24). Variation default master (user decision); T-24 has dryRun (user decision).
**Risk:** the CF write API is compile-to-confirm (no in-repo caller) — SPIKE first each task; aem-mock CF-write support uncertain → seam + Mockito unit tests, LIVE validation as the real proof. No auto-publish.
**Ordering:** Task 1 (resolveContentFragment helper) before Task 2 if Task 2 reuses it; otherwise independent.

## Execution Handoff
Execute via superpowers:subagent-driven-development: fresh subagent per task (read `AGENTS.md` §4 + catalog §6 + `GetCommerceContentFragmentTool`/`AssociatedContentSupport` + this plan's ⚠️ compile-to-confirm note), SPIKE the write signatures FIRST, TDD with the Mockito/seam approach, green gate, commit; task review each; whole-branch review at end; **live-validate** (create CF via T-24 → update via T-23 → verify → delete) against Venia after review. These use an unproven-in-repo write API — review + live validation are the real safety net.
