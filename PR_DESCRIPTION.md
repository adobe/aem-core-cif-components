# SITES-40242: Reduce Bundle Size - Enable Tree-Shaking and Subpath Exports

Fixes: https://github.com/adobe/aem-core-cif-components/issues/1004

---

## Problem

Users report that importing from `@adobe/aem-core-cif-react-components` increases bundle size by **~1MB**, significantly impacting Lighthouse performance scores. When using custom implementations instead of the CIF components, Lighthouse scores improve from ~34 to ~48.

### Root Causes

1. **Single Entry Point Bundle**: The package used a single `dist/index.js` entry point that bundled everything together. Importing any component pulled in the entire library.
2. **No Tree-Shaking Support**: The webpack config used `libraryTarget: 'umd'` which doesn't support tree-shaking. Modern bundlers couldn't eliminate unused exports.
3. **Heavy Dependencies**: The package includes Apollo Client, GraphQL, Braintree payments, internationalization (formatjs), and many other dependencies that got bundled even when unused.
4. **CSS Modules Bundled Together**: All CSS was compiled into a single `main.css` file.

---

## What This PR Does

### 1. **Dual Build Output (ESM + CJS)**

- **ESM Build** (`dist/esm/`): Unbundled ES modules with `modules: false` and `targets: { esmodules: true }` for modern bundlers. Enables tree-shaking.
- **CJS Build** (`dist/cjs/`): CommonJS modules for subpath `require()` support and Node.js compatibility.

**Files changed**: `babel.config.js`, `package.json`, `react-components/pom.xml`

### 2. **Package.json Configuration**

- **`"module": "dist/esm/index.js"`**: Tells bundlers to use ESM for tree-shaking when using `import`.
- **`"sideEffects": ["**/*.css"]`**: Allows bundlers to safely remove unused JS while preserving CSS imports.
- **`"exports"` field**: Defines subpath exports for 40+ components, contexts, utils, and talons.

**How it works**: When consumers use `import { Minicart } from '@adobe/aem-core-cif-react-components/Minicart'`, only that component and its dependencies are resolved—not the entire bundle.

### 3. **Build Process**

The `webpack:prod` script now runs:
1. `webpack --mode=production` (existing UMD bundle for backward compatibility)
2. `npm run build:esm` → Babel transpiles `src/` → `dist/esm/`
3. `npm run build:cjs` → Babel transpiles `src/` → `dist/cjs/`
4. `node css-template.js` (existing)

### 4. **Examples Project: CSS Loader Scoping**

**File**: `examples/ui.frontend/webpack.common.js`

Added an `include` filter to the CSS rule so CSS modules are only applied to:
- `aem-core-cif-react-components`
- `react-components/dist`
- `peregrine`

This prevents CSS module transformation from conflicting with the examples' own SCSS and ensures CIF component styles load correctly when using the new ESM/CJS builds.

### 5. **Test Configuration**

- **jest.config.js**: Added `<rootDir>/dist/` to `testPathIgnorePatterns` so Jest doesn't try to run tests from compiled output.
- **babel.config.js**: Added `testFileIgnore` and fixed `exclude` in test env for proper test file handling.

### 6. **Package Upgrades**

| Package | Upgrade | Why |
|---------|---------|-----|
| **@magento/peregrine** | ^11.0.0 → ^12.5.0 | Peregrine 12.x provides better ESM support and aligns with Magento PWA Studio. It also has improved tree-shaking and smaller footprint. |
| **@apollo/client** | ~3.1.2 → ~3.5.0 | Required by Peregrine 12.x as a peer dependency. Apollo 3.5.x has better tree-shaking and smaller bundle size. |
| **informed** | ~3.9.0 → ~3.29.0 / 3.32.0 | Required by Peregrine 12.x as a peer dependency. Keeps form components compatible with the new Peregrine stack. |

**Summary**: All three upgrades are linked—Peregrine 12.x depends on Apollo 3.5.x and informed ~3.29.x. Upgrading together ensures compatibility and enables better tree-shaking across the dependency chain.

---

## How This Fixes the Issues

| Issue | Solution |
|-------|----------|
| **Single entry point** | Subpath exports allow `import X from '@adobe/aem-core-cif-react-components/X'` — only X is loaded |
| **No tree-shaking** | ESM build with `modules: false` lets webpack 5, Rollup, esbuild eliminate dead code |
| **Heavy dependencies** | Tree-shaking + subpath imports mean only used components (and their deps) are bundled |
| **Backward compatibility** | `main` still points to `dist/index.js`; existing `import X from '@adobe/aem-core-cif-react-components'` continues to work |

---

## Usage for Consumers

**Before (pulls everything):**
```js
import { Minicart } from '@adobe/aem-core-cif-react-components';
```

**After (tree-shakeable, smaller bundle):**
```js
// Option 1: Subpath import (recommended for minimal bundle)
import Minicart from '@adobe/aem-core-cif-react-components/Minicart';

// Option 2: Main entry (still works, but ESM enables tree-shaking)
import { Minicart } from '@adobe/aem-core-cif-react-components';
```

---

## Acceptance Criteria Met

- [x] Users can import individual components without pulling in the entire bundle (via subpath exports)
- [x] Tree-shaking works with modern bundlers (ESM build with `modules: false`)
- [x] Bundle size for typical use cases is significantly reduced (consumers using subpath imports + ESM)
- [x] Backward compatibility maintained (existing imports still resolve to `dist/index.js`)

---

## Files Changed Summary

| File | Change |
|------|--------|
| `react-components/package.json` | `module`, `sideEffects`, `exports`, build scripts, dependency updates |
| `react-components/babel.config.js` | ESM and CJS env configs, test file ignore |
| `react-components/jest.config.js` | Ignore `dist/` in tests |
| `react-components/pom.xml` | Include `dist/esm` and `dist/cjs` in package |
| `examples/ui.frontend/webpack.common.js` | CSS loader `include` for CIF components |
