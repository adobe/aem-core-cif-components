# Regenerating `it/site` (AEM Archetype → CIF IT Site layout)

Use this when you want a **fresh** AEM Commerce project with the same IDs as this module, then apply the same trims and additions so the next iteration matches **`it/site`** without rediscovering steps.

**Context:** This module lives inside the `aem-core-cif-components` monorepo at `it/site/`. Its reactor pom inherits from **`core-cif-components-parent`** (version `2.18.3-SNAPSHOT`, relativePath `../../parent/pom.xml`). If you need to recreate this as a **standalone** project outside the monorepo, the archetype command in step 1 is your starting point; for embedding inside the monorepo again, follow the same steps and then re-parent as described in step 6.

---

## Target layout (what you should have when done)

```text
it/site/
├── pom.xml                          # reactor + dependencyManagement + profile classic
├── all/                             # cloud "all" container (no local core bundle embed)
├── ui.apps/
├── ui.apps.structure/               # structure package; filters in pom.xml only
├── ui.config/
├── ui.content/
├── ui.frontend/
├── classic/                         # built only with -Pclassic
│   ├── ui.config/                   # osgiconfig-classic OSGi for 6.x CIF GraphQL
│   ├── ui.content/                  # commerce cloud config + /var/commerce/products/…
│   └── all/                         # mixed classic "all": site + classic + CIF + WCM Core
├── generate.md
└── README.md
```

**Default reactor modules (root `pom.xml`):** `all`, `ui.frontend`, `ui.apps`, `ui.apps.structure`, `ui.config`, `ui.content` — **no** `core`, `it.tests`, `ui.tests`.

**Classic reactor modules (profile `classic`):** `classic/ui.config`, `classic/ui.content`, `classic/all`.

---

## 1. Maven archetype (baseline)

Run from the **parent folder** where the new directory should appear. Adjust `-DarchetypeVersion` to a version you can resolve from Maven (release or snapshot).

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=com.adobe.aem \
  -DarchetypeArtifactId=aem-project-archetype \
  -DarchetypeVersion=57-SNAPSHOT \
  -DgroupId=com.adobe.commerce.cif \
  -DartifactId=cif-components-it-site \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=com.adobe.commerce.cif \
  -DappId=cif-components-it-site \
  -DappTitle="CIF IT Site" \
  -DaemVersion=cloud \
  -DsdkVersion=latest \
  -Dlanguage=en \
  -Dcountry=us \
  -DsingleCountry=n \
  -DincludeCif=y \
  -DcommerceEndpoint=https://YOUR-HOST/graphql \
  -DfrontendModule=general \
  -DincludeDispatcherConfig=n \
  -DincludeExamples=n
```

- Replace **`https://YOUR-HOST/graphql`** with your Commerce GraphQL endpoint (must be `https` and end with **`/graphql`** per archetype validation).
- **`sdkVersion=latest`**: AEM Cloud SDK API version label for the generated parent POM — not the frontend. Pin a concrete SDK string in **`pom.xml`** if you want reproducible builds.
- **`includeDispatcherConfig=n`**: matches this module (no dispatcher module).
- **`includeExamples=n`**: reduces sample surface; you still fix **Hello World** if the archetype leaves a model-bound HTL (see below).

---

## 2. Remove the `core` bundle and test modules (no in-repo Java)

The archetype may create **`core`**, **`it.tests`**, and **`ui.tests`**. This module is **FileVault + frontend only** (CIF Core comes from Maven coordinates).

1. **Delete directories:** `core/`, `it.tests/`, `ui.tests/` (if present).
2. **Root `pom.xml`:** remove `<module>core</module>`, `<module>it.tests</module>`, `<module>ui.tests</module>` from `<modules>`.
3. **`all/pom.xml`:** remove the `<embedded>` (and any related dependency) for **`cif-components-it-site.core`**.
4. **`ui.apps/pom.xml`:** remove the **`cif-components-it-site.core`** dependency.
5. **`ui.content/pom.xml`:** remove the **`cif-components-it-site.core`** dependency.

---

## 3. Hello World HTL (compile without local Sling models)

If **`ui.apps`** contains a **Hello World** component whose HTL uses **`data-sly-use`** / **`com.adobe.commerce.cif.core.models.HelloWorldModel`** from the removed **`core`** bundle, **`htl-maven-plugin`** will fail.

**Choose one:**

- Remove the `helloworld` component, **or**
- Strip the model usage so the component uses only dialog / static markup (this module's approach).

---

## 4. Cloud CIF configuration under `/conf` (required for AEM CS / cloud `all`)

The default **`ui.content`** package must include **Commerce** under the site's context-aware configuration, otherwise **Tools → Cloud Services → Commerce** (and bindings that use `cq:graphqlClient="default"`) will not exist for **`/conf/cif-components-it-site`**.

Add:

- `conf/<appId>/settings/cloudconfigs/.content.xml` — `sling:Folder`
- `conf/<appId>/settings/cloudconfigs/commerce/.content.xml` — **Cloud only** in **`ui.content`**: **`cif/shell/components/configuration/page`**. **Do not** put **`commerce/gui`** or **`/var/commerce/...`** here; those stay **`classic/ui.content`** for 6.5.
- `conf/<appId>/settings/cloudconfigs/commerce/_rep_policy.xml` — ACLs for authors (copy from Venia / this module).

**Classic-only** commerce pages (`commerce/gui`, `cq:catalogPath`, `/var/commerce/products/…`) stay in **`classic/ui.content`** and ship with **`classic/all`** for 6.5.

---

## 5. `ui.apps.structure`

- If the archetype left an **empty** `ui.apps.structure/src/main/content` tree, **delete** it so the structure package is driven by **`<filters>`** in **`ui.apps.structure/pom.xml`**.
- Align **`<filters>`** with paths your code packages install under (e.g. `/apps/cif-components-it-site`, `/conf/cif-components-it-site`, `/content/cif-components-it-site`, `/var`, etc.).
- On **`filevault-package-maven-plugin`**, set **`<extensions>true</extensions>`** when the plugin is declared at module level.

---

## 6. Root `pom.xml`: parent and versions

### 6a. Standalone project (outside the monorepo)

Keep / tune these **properties** (examples as in this module; bump to match your Cloud Manager / CIF release):

- **`aem.sdk.api`** — AEM as a Cloud Service SDK API for cloud modules.
- **`aem.cif.sdk.api`** — CIF add-on API for `ui.apps` / compile scope.
- **`core.cif.components.version`** — released **`core-cif-components-*`** + **`magento-graphql`** from Maven (used by **`all`** and **`classic/all`**).
- **`core.wcm.components.version`** — WCM Core (needed for **`classic/all`** embeds).
- **`magento.graphql.version`** — GraphQL client artifact line.

### 6b. Inside `aem-core-cif-components` monorepo

The reactor pom must inherit from **`core-cif-components-parent`**:

```xml
<parent>
  <groupId>com.adobe.commerce.cif</groupId>
  <artifactId>core-cif-components-parent</artifactId>
  <version>2.18.3-SNAPSHOT</version>
  <relativePath>../../parent/pom.xml</relativePath>
</parent>
```

Drop `<groupId>` and `<version>` from the reactor pom (both inherited). Properties already in the monorepo parent (`aem.host`, `aem.port`, `core.wcm.components.version`, `magento.graphql.version`, etc.) should be omitted from `it/site/pom.xml` unless you need to override them. Only site-specific properties need to be declared (`core.cif.components.version`, `aem.sdk.api`, `aem.cif.sdk.api`, `aemanalyser.version`, etc.).

All child poms' `<parent><version>` must match the monorepo version (`2.18.3-SNAPSHOT`), not the archetype-generated `1.0.0-SNAPSHOT`.

**Version compatibility — WCM Core vs CIF Core (critical):** `core-cif-components-apps` declares a vault dependency on a minimum `core.wcm.components.content` version. If `core.wcm.components.version` is lower than what CIF requires, Package Manager will refuse to install CIF apps on AEM 6.5. Always check the CIF release notes or inspect the vault `properties.xml` of the `core-cif-components-apps-*.zip` artifact for its `dependencies` entry. Example: CIF Core **2.18.2** requires WCM Core **≥ 2.29.0**.

**`dependencyManagement`:** ensure at least:

- **`com.adobe.commerce.cif`**: `core-cif-components-apps` (zip), `core-cif-components-config` (zip), `core-cif-components-core`, `magento-graphql`
- **`com.adobe.cq`**: `core.wcm.components.core` (without `provided` scope to allow embedding), **`core.wcm.components.content`** (zip), **`core.wcm.components.config`** (zip) — the last two are required so **`classic/all`** can resolve WCM Core content/config packages.

---

## 7. Adding AEM 6.5 / AMS support — the `classic/` folder

> **The Maven archetype (step 1) generates a cloud-only project.** It does not create `classic/` at all. To make the project deployable on AEM 6.5 / AMS you need to add the three classic modules manually.
>
> **Fastest approach:** copy the `classic/` folder from this module into your new project, then do the targeted substitutions listed below. All structure, packaging rules, and pitfalls are already correct in this folder — you are just renaming artifact IDs and paths.

### 7.1 Copy `classic/` into the new project

```bash
# from the root of your new archetype-generated project
cp -r /path/to/aem-core-cif-components/it/site/classic ./classic
# remove build artifacts if present
rm -rf classic/*/target
```

### 7.2 String substitutions — find & replace throughout `classic/`

Every occurrence of the strings below must be replaced with your project's values. A single `sed` or IDE find-and-replace across the whole `classic/` folder is sufficient.

| Find (this module) | Replace with | Where it appears |
|---|---|---|
| `cif-components-it-site` | `<your-appId>` | pom.xml `<artifactId>`, `<parent>`, filter paths, JCR paths, `.content.xml` properties |
| `cif-components-it-site-default` | `<your-appId>-default` | filter roots, `cq:catalogPath`, directory name under `var/commerce/products/` |
| `com.adobe.commerce.cif` | `<your-groupId>` | pom.xml `<groupId>`, `<parent><groupId>`, `<dependency><groupId>` |

After substitution, also **rename the two JCR content directories** to match your appId:

```bash
# classic/ui.content — rename the conf path directory
mv classic/ui.content/src/main/content/jcr_root/conf/cif-components-it-site \
   classic/ui.content/src/main/content/jcr_root/conf/<your-appId>

# classic/ui.content — rename the var/commerce/products directory
mv "classic/ui.content/src/main/content/jcr_root/var/commerce/products/cif-components-it-site-default" \
   "classic/ui.content/src/main/content/jcr_root/var/commerce/products/<your-appId>-default"
```

### 7.3 Changes in the root `pom.xml`

Three things to add / adjust in the **root `pom.xml`** of the new project:

**a) Add the `classic` profile** (inside `<profiles>`):

```xml
<profile>
  <id>classic</id>
  <modules>
    <module>classic/ui.config</module>
    <module>classic/ui.content</module>
    <module>classic/all</module>
  </modules>
</profile>
```

**b) Fix WCM Core version** to match what CIF Core requires.  
Check the CIF Core artifact's vault dependency (inspect `core-cif-components-apps-<version>.zip` → `META-INF/vault/properties.xml` → `dependencies` entry). Set `core.wcm.components.version` to that version or higher.  
Example: CIF Core **2.18.2** requires WCM Core **≥ 2.29.0**.

```xml
<core.wcm.components.version>2.29.0</core.wcm.components.version>
```

**c) Add WCM Core to `dependencyManagement`** if not already present:

```xml
<dependency>
    <groupId>com.adobe.cq</groupId>
    <artifactId>core.wcm.components.content</artifactId>
    <version>${core.wcm.components.version}</version>
    <type>zip</type>
</dependency>
<dependency>
    <groupId>com.adobe.cq</groupId>
    <artifactId>core.wcm.components.config</artifactId>
    <version>${core.wcm.components.version}</version>
    <type>zip</type>
</dependency>
<dependency>
    <groupId>com.adobe.cq</groupId>
    <artifactId>core.wcm.components.core</artifactId>
    <version>${core.wcm.components.version}</version>
</dependency>
```

### 7.4 What the classic modules contain (reference)

| Module | packageType | Key content |
|---|---|---|
| `classic/ui.config` | `container` | OSGi configs under `osgiconfig-classic/config/` (GraphQL client + data service) and `osgiconfig-classic/config.author/` (editor status type) |
| `classic/ui.content` | `content` | Commerce cloudconfig page with AEM 6.5 resource type; `/var/commerce/products/` catalog root folder |
| `classic/all` | `mixed` | Embeds all site packages + classic overlays + CIF Core + WCM Core; the only artifact you deploy to AEM 6.5 |

### 7.5 Critical pitfalls

**⚠️ Never embed `ui.apps.structure` in `classic/all`.**  
`ui.apps.structure` is an intentionally empty package with broad REPLACE-mode filters covering `/apps`, `/conf`, `/content`, etc. Embedding it causes AEM's JCR Package Installer to wipe out everything under those roots on install — corrupting the entire repository. It must only ever appear in `<repositoryStructurePackages>` (build-time validation, never deployed). The `classic/` folder in this module already has this correct.

**WCM Core version must satisfy CIF Core's vault dependency.**  
If `core.wcm.components.version` is lower than what `core-cif-components-apps` requires, Package Manager will refuse to install it on AEM 6.5 with a `dependencies!` error. See 7.3b above.

**Install order is enforced by vault dependency.**  
`classic/ui.content-classic` declares a vault dependency on `ui.content` in its `properties.xml`. This guarantees AEM Package Manager installs the cloud content package first (which creates `/conf/.../cloudconfigs/commerce` with the cloud resource type), then the classic overlay runs (replacing it with the 6.5 resource type `commerce/gui/components/configuration/page`).

### 7.6 Deploy and verify on AEM 6.5

```bash
# 1. Build everything including classic modules (from it/site/ or monorepo root)
mvn clean install -Pclassic

# 2. Upload to AEM 6.5 Package Manager UI:
#    classic/all/target/cif-components-it-site.all-classic-*.zip
```

The JCR Package Installer runs **asynchronously** — wait ~30 seconds after Package Manager reports success before checking CRXDE.

**Expected state in CRXDE after install:**

| Path | Expected value |
|---|---|
| `/conf/<appId>/settings/cloudconfigs/commerce/jcr:content/@sling:resourceType` | `commerce/gui/components/configuration/page` |
| `/var/commerce/products/<appId>-default/@jcr:primaryType` | `sling:Folder` |
| `/var/commerce/products/<appId>-default/@cq:conf` | `/conf/<appId>` |

In Package Manager, confirm **`<appId>.ui.content-classic`** shows **Installed** (not just the `all-classic` container).

---

## 8. `ui.frontend`

The Webpack build runs in CI / full reactor. After clone or checkout:

```bash
cd ui.frontend && npm ci
```

If **`webpack`** is missing from PATH during Maven, dependencies were not installed.

---

## 9. Verify

**Cloud reactor (from `it/site/`):**

```bash
mvn clean install
```

**Including classic:**

```bash
mvn clean install -Pclassic
```

**From the monorepo root (integration-tests profile):**

```bash
mvn clean install -pl it/site -am
```

**Local install (AEM Cloud SDK):** `mvn clean install -PautoInstallSinglePackage` — do **not** add `-Pclassic` for Cloud SDK.

**Local install (AEM 6.5):** build first, then upload directly:
```bash
mvn clean install -Pclassic
# Upload classic/all/target/cif-components-it-site.all-classic-*.zip via AEM Package Manager UI
```

Avoid `mvn clean install -PautoInstallSinglePackage,classic` on AEM 6.5 — it deploys the cloud `all` container first, which may install Cloud Service-specific content before the classic overlay can correct it.

---

## 10. Optional: CIF Core from a local build

Since this module is part of `aem-core-cif-components`, the CIF Core artifacts are built in the same reactor. When building the full monorepo (`mvn clean install` from the root), the locally built CIF Core is automatically available to `it/site` via the local `.m2` cache. If building `it/site` in isolation, ensure `core-cif-components-apps`, `core-cif-components-config`, `core-cif-components-core`, and `magento-graphql` at version `${core.cif.components.version}` are already installed in your local `.m2`:

```bash
# From monorepo root — build CIF Core first, then site
mvn clean install -pl ui.apps,all -am
mvn clean install -pl it/site
```
