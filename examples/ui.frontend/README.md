# Frontend Build

## Overview

A React app which bootstraps the client-side CIF components used in the store-front of the CIF components library (details at https://github.com/adobe/aem-core-cif-components/blob/master/react-components/README.md). This app uses the `@adobe/aem-core-cif-react-components` library as a dependency.

## Usage

The following npm scripts drive the frontend workflow:

-   `npm run dev` - Full build of client libraries with JS optimization disabled (tree shaking, etc) and source maps enabled and CSS optimization disabled.
-   `npm run prod` - Full build of client libraries build with JS optimization enabled (tree shaking, etc), source maps disabled and CSS optimization enabled.

### General

The ui.frontend module compiles the code under the `ui.frontend/src` folder and outputs the compiled CSS and JS, and any resources beneath a folder named `ui.frontend/dist`.

-   **cif-examples-react** - `site.js` and a `resources/` folder for translations are created in a `dist/cif-examples-react` folder.

#### Notes

-   Utilizes dev-only and prod-only webpack config files that share a common config file. This way the development and production settings can be tweaked independently.

### Client Library Generation

The second part of the ui.frontend module build process leverages the [aem-clientlib-generator](https://www.npmjs.com/package/aem-clientlib-generator) plugin to move the compiled CSS, JS and any resources into the `ui.apps` module. The aem-clientlib-generator configuration is defined in `clientlib.config.js`. The following client library is generated:

-   **cif-examples-react** - [../ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/cif-examples-react](../ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/cif-examples-react)

That clientlib is embedded into the [cif-clientlib-base](../ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs/cif-clientlib-base) to include the CIF React components in the CIF components library pages.

### Local development

For local development you may want to work with the SNAPSHOT version of the `@adobe/aem-core-cif-react-components` package using `npm link`. You can do this by going to the root of the `examples` project and using the following command to build it:

```bash
mvn clean install -PautoInstallPackage,fedDev
```

This will build and install the whole project, but will also use the `fedDev` which will build and install the SNAPSHOT version of `@adobe/aem-core-cif-react-components`

If you don't want to use the Maven profile you can do these steps manually:

1. Go to the `../../react-components` folder and run `npm run webpack:dev` and `npm link` to generate a development build and link it
2. Come back to `ui.fronted` and run `npm link @adobe/aem-core-cif-react-components` to link the module above
3. Run `npm run prod` to generate the client library
4. Install the `ui.apps` package using `mvn clean install content-package:install`

The alternative to step 4 above is to use an utility like [aemsync](https://www.npmjs.com/package/aemsync) to "watch" the `../ui.apps/src/main/content/jcr_root/apps/cif-components-examples/clientlibs` for changes and automatically install these changes in AEM.
