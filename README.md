[![CircleCI](https://circleci.com/gh/adobe/aem-core-cif-components.svg?style=svg)](https://circleci.com/gh/adobe/aem-core-cif-components)
[![codecov](https://codecov.io/gh/adobe/aem-core-cif-components/branch/master/graph/badge.svg)](https://codecov.io/gh/adobe/aem-core-cif-components)

# AEM CIF Core Components

AEM Commerce reference store front project that serves as accelerator to get started with projects using AEM, CIF and Magento. The project contains re-useable Commerce core components as well as a complete sample store front serving as documentation and showing best practices. The store front combines server-side rendered AEM components with client-side React commerce components (MPA) for dynamic experiences / data. The components use the [Venia](https://github.com/magento-research/pwa-studio/tree/develop/packages/venia-concept) theme<sup id="a1">[1](#f1)</sup>.

This project is intended to be used in conjunction with the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). AEM CIF Core Components use the AEM Sites Core Components as a foundation where possible and extending them.

## Documentation

See our [wiki](https://github.com/adobe/aem-core-wcm-components/wiki) for usage and configuration instructions of the AEM CIF Core Components.

## Available Components

- [Product Teaser](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/productteaser/v1/productteaser)
- [Product](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/product/v1/product)
- [Product List](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/productlist/v1/productlist)
- [Navigation](ui.apps/src/main/content/jcr_root/apps/venia/components/structure/navigation/v1/navigation)
- [Search Results](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/searchresults/v1/searchresults)

## System Requirements

The latest version of the AEM CIF Core Components, require the below minimum system requirements:

| CIF Core Components | AEM 6.4 | AEM 6.5 | Magento | Java |
| ------------------- | ------- | ------- | ------- | ---- |
| 0.0.1               | 6.4.4.0 | 6.5.0   | 2.3.1   | 1.8  |

Additionally, it is highly recommended to use this project together with the [AEM Commerce connector for Magento](https://github.com/adobe/commerce-cif-connector). This is no hard dependency but some authoring functionality will be limited without the AEM Commerce connector for Magento as product pickers, product assets view and consoles are not available.

For a list of requirements for previous versions, see [Historical System Requirements](VERSIONS.md).

## Installation

1. Clone this repository.
2. Run a `mvn clean install` in the root folder to install the artifacts to your local Maven repository.
3. Switch to the `all` project and run a `mvn clean install content-package:install`.

Here is a full [video walk trough of the setup process](https://images-tv.adobe.com/mpcv3/c2f213a8-a219-4be7-b80b-3281b962394d_1558051150.1920x1080at3000_h264.mp4).

### UberJar

This project relies on the AEM 6.4.4 `cq-quickstart` UberJar. This is publicly available on https://repo.adobe.com

For more details about the UberJar please head over to the
[How to Build AEM Projects using Apache Maven](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/ht-projects-maven.html) documentation page.

## Configuration

To connect the AEM CIF Core Components with your Magento instance follow the [configuration steps](https://github.com/adobe/aem-core-cif-components/wiki/configuration). For a multi store / site setup one [additional step](https://github.com/adobe/aem-core-cif-components/wiki/configuration#multi-store--site-configuration) is needed to link an AEM site to a Magento store view.

### Contributing

Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.

### Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

---

<b id="f1">1</b>: "Venia" is the name of the sample progressive web app development by Magento. It has a specific theme which has been applied to our project, event though we're using classic AEM components [&#8617;](#a1)
