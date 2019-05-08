[![CircleCI](https://circleci.com/gh/adobe/aem-core-cif-components.svg?style=svg)](https://circleci.com/gh/adobe/aem-core-cif-components)
[![codecov](https://codecov.io/gh/adobe/aem-core-cif-components/branch/master/graph/badge.svg)](https://codecov.io/gh/adobe/aem-core-cif-components)

# AEM CIF Core Components
AEM Commerce reference store front project that serves as accelerator to get started with projects using AEM, CIF and Magento. The project contains re-useable Commerce core components as well as a complete sample store front serving as documentation and showing best practices. The store front combines server-side rendered AEM components with client-side React commerce components (MPA) for dynamic experiences / data. The components use the [Venia](https://github.com/magento-research/pwa-studio/tree/develop/packages/venia-concept) theme<sup id="a1">[1](#f1)</sup>. 

This project is intended to be used in conjunction with the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). AEM CIF Core Components use the AEM Sites Core Components as a foundation where possible and extending them.

## Documentation
> TODO

## Available Components
* [Product](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/product/v1/product)
* [Product List](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/productlist/v1/productlist)
* [Navigation](ui.apps/src/main/content/jcr_root/apps/venia/components/structure/navigation/v1/navigation)
* [Search Results](ui.apps/src/main/content/jcr_root/apps/venia/components/commerce/searchresults/v1/searchresults)

## Installation
1. Clone this repository.
2. Run a `mvn clean install` in the root folder to install the artifacts to your local Maven repository.
3. Switch to the `all` project and run a `mvn clean install content-package:install`.

### UberJar
This project relies on the AEM 6.4.4 `cq-quickstart` UberJar. This is publicly available on https://repo.adobe.com

For more details about the UberJar please head over to the
[How to Build AEM Projects using Apache Maven](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/ht-projects-maven.html) documentation page.

## System Requirements
The latest version of the AEM CIF Core Components, require the below minimum system requirements:

| CIF Core Components | AEM 6.4 | AEM 6.5 | Magento | Java |
|---------------------|---------|---------|---------|------|
| 0.0.1               | 6.4.4.0 | 6.5.0   | 2.3.1   | 1.8  |

Additionally, it is highly recommended to use this project together with the [AEM Commerce connector for Magento](https://github.com/adobe/commerce-cif-connector). This is no hard dependency but some authoring functionality will be limited without the AEM Commerce connector for Magento as product pickers, product assets view and consoles are not available.

For a list of requirements for previous versions, see [Historical System Requirements](VERSIONS.md).

## CIF Magento GraphQL Configuration

The AEM CIF Core Components connect to a Magento via GraphQL that has to be configured to access your Magento instance. Follow the steps below to configure the bundle: 

1) Configure the generic GraphQL instance
    * Go to http://localhost:4502/system/console/configMgr
    * Look for _CIF GraphQL Client Configuration Factory_
    * Create a child configuration
        * Keep the `default` service identifier or set something custom. Make sure to use the same value in step 2) below.
        * For _GraphQL Service URL_ enter the URL of your Magento GraphQL endpoint (usually `https://hostname/graphql`)
        * If you use this project together with the [AEM Commerce connector for Magento](https://github.com/adobe/commerce-cif-connector) this has to be configured only once.

2) Link your AEM site with the GraphQL client instance
    * Go to http://localhost:4502/crx/de/index.jsp
    * Navigate to the page root node e.g. /content/venia/jcr:content for the Venia sample page
    * Add a property `cq:graphqlClient` with the value of the service identifier of step 1
    
3) Assign the Magento root category to your website
    * Go to AEM Sites console http://localhost:4502/sites.html/
    * Navigate to the catalog page with your site structure e.g. /content/venia/us/en/products for the Venia sample page
    * Select the catalog page and open page properties
    * Select the Commerce tab
    * Enter the root category of your Magento catalog the site should be assigned or use the category picker

### Contributing
 
Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.
 
### Licensing
 
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.


---
<b id="f1">1</b>: "Venia" is the name of the sample progressive web app development by Magento. It has a specific theme which has been applied to our project, event though we're using classic AEM components [&#8617;](#a1)
