[![CircleCI](https://circleci.com/gh/adobe/aem-core-cif-components.svg?style=svg)](https://circleci.com/gh/adobe/aem-core-cif-components)
[![codecov](https://codecov.io/gh/adobe/aem-core-cif-components/branch/master/graph/badge.svg)](https://codecov.io/gh/adobe/aem-core-cif-components)

# AEM CIF Core Components

The AEM CIF Core Components project serves as accelerator to get started with projects using AEM, CIF and Magento. The project contains re-useable Commerce core components which combine server-side rendered AEM components with client-side React commerce components (MPA) for dynamic experiences / data. The components use the [Venia](https://github.com/magento-research/pwa-studio/tree/develop/packages/venia-concept) theme<sup id="a1">[1](#f1)</sup>.

This project is intended to be used in conjunction with the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). AEM CIF Core Components use the AEM Sites Core Components as a foundation where possible and extending them.

For starting a new project please have a look at our [CIF archetype](https://github.com/adobe/aem-cif-project-archetype) project. There you will also find a complete sample project that uses the WCM and CIF core components to deliver a stunning store-front experience.

## Documentation

See our [wiki](https://github.com/adobe/aem-core-wcm-components/wiki) for usage and configuration instructions of the AEM CIF Core Components.

## Available Components

- [Product Teaser](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productteaser/v1/productteaser)
- [Product](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product)
- [Product List](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productlist/v1/productlist)
- [Navigation](ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/navigation/v1/navigation)
- [Search Results](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/searchresults/v1/searchresults)

## System Requirements

The latest version of the AEM CIF Core Components, require the below minimum system requirements:

| CIF Core Components | AEM 6.4 | AEM 6.5 | Magento | Java |
| ------------------- | ------- | ------- | ------- | ---- |
| 0.0.1               | 6.4.4.0 | 6.5.0   | 2.3.1   | 1.8  |

### AEM Commerce connector for Magento

This project uses the [AEM Commerce connector for Magento](https://github.com/adobe/commerce-cif-connector) to improve the authoring experience by leveraging the product pickers, product assets view and consoles provided by the connector package.

### AEM Sites Core Components

This project relies on the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). They are typically installed as part of AEM. If you install AEM without sample content option you have to [deploy them manually](https://github.com/adobe/aem-core-wcm-components#installation) before using the AEM CIF Core Components.

For a list of requirements for previous versions, see [Historical System Requirements](VERSIONS.md).

## Installation

1. Clone this repository.
2. Run a `mvn clean install` in the root folder to install the artifacts to your local Maven repository.
3. Switch to the `all` project and run a `mvn clean install content-package:install`.

Here is a full [video walk trough of the setup process](https://www.adobe.io/apis/experiencecloud/commerce-integration-framework/getting-started.html).

### UberJar

This project relies on the AEM 6.4.4 `cq-quickstart` UberJar. This is publicly available on https://repo.adobe.com

For more details about the UberJar please head over to the
[How to Build AEM Projects using Apache Maven](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/ht-projects-maven.html) documentation page.

## Include core components as subpackage into your own project maven build

The released version of the AEM CIF Core Components are available on the maven central repository. To include the
AEM CIF Core Components package into your own project maven build you can add the dependency
 ```
 <dependency>
     <groupId>com.adobe.commerce.cif</groupId>
     <artifactId>core-cif-components-all</artifactId>
     <type>zip</type>
     <version>2.2.0</version>
 </dependency>
 ```

 and sub package section
```
 <subPackage>
     <groupId>com.adobe.commerce.cif</groupId>
     <artifactId>core-cif-components-all</artifactId>
     <filter>true</filter>
 </subPackage>
```

 to the `content-package-maven-plugin`.
 
You also need to add the [AEM Commerce connector for Magento](https://github.com/adobe/commerce-cif-connector) all package, see above. Make sure you add that as a dependency as well.


## Configuration

To connect the AEM CIF Core Components with your Magento instance follow the [configuration steps](https://github.com/adobe/aem-core-cif-components/wiki/configuration). For a multi store / site setup one [additional step](https://github.com/adobe/aem-core-cif-components/wiki/configuration#multi-store--site-configuration) is needed to link an AEM site to a Magento store view.

## Testing
### Karma Unit Tests
The client-side JavaScript code of the components is covered using Mocha unit tests executed with Karma. Please make sure that for every contribution new client-side code is covered by tests and that all tests pass.

```bash
cd ui.apps
npm install
npm test
```

Karma will test with Chrome and Firefox. Make sure you have both browsers installed.

## Code Formatting
### Java
You can find the code formatting rules in the `eclipse-formatter.xml` file. The code formatting is automatically checked for each build. To automatically format your code, please run:
```bash
mvn clean install -Pformat-code
```

### JavaScript & CSS
For formatting JavaScript and CSS we use [prettier](https://prettier.io/). The formatting is automatically checked when running `npm test` in the `ui.apps` project. To automatically format your code, please run the following command in `ui.apps`:
```bash
npm run prettier-fix
```

## Contributing
 
Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.
 
## Licensing
 
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

---

<b id="f1">1</b>: "Venia" is the name of the sample progressive web app development by Magento. It has a specific theme which has been applied to our project, event though we're using classic AEM components [&#8617;](#a1)
