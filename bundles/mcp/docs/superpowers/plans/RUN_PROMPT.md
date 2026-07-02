# Autonomous execution prompt — CIF Commerce MCP

Paste the block below into a fresh session started in this repo
(`aem-core-cif-components`). It drives the implementation plan to completion,
unattended, and validates against the running AEM instance at
http://localhost:4502 (admin:admin).

The 4502 instance is only a deploy/runtime target — the **code must stay AEM 6.5
compatible** (Java source level 8, no AEMaaCS-only APIs), matching the other bundles.

---

```text
Work fully autonomously to completion. I am away and will not answer questions —
do NOT ask for confirmation or pause between steps. Only stop if genuinely blocked
after exhausting reasonable fixes; if that happens, write STATUS.md at the repo root
explaining exactly where you stopped and why. Everything should be done and validated
when I return.

## What to build
Implementation plan: docs/superpowers/plans/2026-07-02-cif-commerce-mcp.md
Reference design/spec: docs/superpowers/specs/2026-07-02-cif-commerce-mcp-design.md
Both already exist in this repo (aem-core-cif-components).

## How to execute
- Use the superpowers:subagent-driven-development skill (fallback: superpowers:executing-plans)
  to implement the plan task by task, in order, start to finish.
- Follow each task's TDD steps exactly: write the failing test, run it red, write the
  minimal implementation, run it green, then commit — one commit per task, using the
  commit messages the plan specifies.
- Work on the current `mcp` branch (do not create a new branch and do not switch away
  from it). Never commit to main. Do not push and do not merge.

## Hard constraints
- The code MUST be AEM 6.5 compatible, exactly like the other bundles in this repo:
  Java source/target level 8, and no API newer than what bundles/core relies on.
  Mirror bundles/core/pom.xml's parent, compiler, and plugin (maven-bundle-plugin,
  formatter, impsort, macker, source) configuration.
- Build a single module from the repo root: `mvn -pl bundles/mcp -am clean install`.
- Keep formatter/impsort/macker green: run `mvn -q formatter:format impsort:sort`
  before each commit, as the plan instructs.
- Apache 2.0 license header on every new .java file (copy from any bundles/core file).

## Runtime validation (after the module builds green and all new tests pass)
A running AEM author instance is available at http://localhost:4502 (admin:admin).
Use it ONLY as a deploy/runtime target — it does not relax the 6.5 code constraint above.
Use curl for everything and capture the ACTUAL responses.

1. Deploy the new bundle using the repo's standard mechanism (the autoInstallBundle /
   sling-maven-plugin profile, or POST the built jar to the Felix console).
2. Confirm it is running: GET http://localhost:4502/system/console/bundles.json and
   verify `core-cif-components-mcp` is ACTIVE (not Installed/Resolved). If it fails to
   resolve, fix the bundle manifest/imports until it activates.
3. Locate a CIF nav-root page (look under /content/venia or any CIF site on the instance).
   If a CIF nav-root exists, exercise the endpoint with JSON-RPC POSTs:
   - POST {navroot}.mcp.json  method "initialize"  → expect protocolVersion "2025-06-18"
     and a tools capability.
   - POST {navroot}.mcp.json  method "tools/list"  → expect the shopper (read) tools.
   - POST {navroot}.mcp.json  method "tools/call" for search_products and
     browse_categories → expect a structuredContent result. If the page's commerce
     backend is not configured/reachable, a WELL-FORMED JSON-RPC tool error is an
     acceptable outcome (it proves dispatch works) — note it as such.
   - POST {navroot}.mcp-authoring.json → confirm the authoring selector is reachable on
     author and exposes the write tools.
   - POST a NON-nav-root page's .mcp.json → expect HTTP 404.
   If no CIF content exists on the instance, say so explicitly and validate as far as
   possible (bundle ACTIVE + servlet 404 on a non-nav-root page).

## When done
- Ensure the full `mvn -pl bundles/mcp -am clean install` is green and every new test passes.
- Write VALIDATION.md at the repo root summarizing, with the REAL observed output:
  build result, tests passed, bundle active state, and each endpoint check above.
  Be honest — if a check could not run, state that plainly; never claim a success you
  did not observe.
- Leave all per-task commits on the `mcp` branch.
```
