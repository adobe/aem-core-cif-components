[![CircleCI](https://circleci.com/gh/adobe/aem-core-cif-components.svg?style=svg)](https://circleci.com/gh/adobe/aem-core-cif-components)
[![codecov](https://codecov.io/gh/adobe/aem-core-cif-components/branch/master/graph/badge.svg)](https://codecov.io/gh/adobe/aem-core-cif-components)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.adobe.commerce.cif/core-cif-components-all/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.adobe.commerce.cif/core-cif-components-all)
[![npm](https://img.shields.io/npm/v/@adobe/aem-core-cif-react-components)](https://www.npmjs.com/package/@adobe/aem-core-cif-react-components)
![GitHub](https://img.shields.io/github/license/adobe/aem-core-cif-components.svg)
[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/codespaces/new?hide_repo_select=true&ref=master&repo=184471389&machine=standardLinux32gb&location=WestEurope&devcontainer_path=.devcontainer%2Fdevcontainer.json)

# AEM CIF Core Components

The AEM CIF Core Components project serves as accelerator to get started with projects using AEM, CIF and Adobe Commerce. The project contains re-useable Commerce core components which combine server-side rendered AEM components with client-side React commerce components (MPA) for dynamic experiences / data.

This project is intended to be used in conjunction with the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). AEM CIF Core Components use the AEM Sites Core Components as a foundation where possible and extending them.

For starting a new project please have a look at our [archetype](https://github.com/adobe/aem-project-archetype) project. Also have a look at our [Venia sample project](https://github.com/adobe/aem-cif-guides-venia) that uses the WCM and CIF core components to deliver a stunning store-front experience.

## Documentation

See the [AEM Content & Commerce documentation](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/developing/develop.html) for usage and configuration instructions of the AEM CIF Core Components and [Introduction to AEM Component Development](https://experienceleague.adobe.com/docs/experience-manager-learn/sites/components/component-development.html).

## Available Components

- [Product v1](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/product/v1/product)
- [Product v2](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/product/v2/product) - Adobe Commerce EE only with version >= 2.4.2
- [Product List v1](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productlist/v1/productlist)
- [Product List v2](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productlist/v2/productlist) - Adobe Commerce EE only with version >= 2.4.2
- [Product Collection v1](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productcollection/v1/productcollection)
- [Product Collection v2](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productcollection/v2/productcollection) - Adobe Commerce EE only with version >= 2.4.2
- [Product Teaser](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productteaser/v1/productteaser)
- [Product Carousel](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/productcarousel/v1/productcarousel)
- [Related Products](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/relatedproducts/v1/relatedproducts)
- [Navigation](ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/navigation/v2/navigation)
- [Breadcrumb](ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/breadcrumb/v1/breadcrumb)
- [Search Results v1](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/searchresults/v1/searchresults)
- [Search Results v2](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/searchresults/v2/searchresults)
- [Searchbar](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/searchbar/v2/searchbar)
- [Shopping Cart](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/minicart/v2/minicart)
- [Featured Category List](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist)
- [Commerce Teaser](ui.apps/src/main/content/jcr_root/apps/core/cif/components/content/teaser/v3/teaser)
- [Commerce Button](ui.apps/src/main/content/jcr_root/apps/core/cif/components/content/button/v2/button)
- [Commerce List](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/list/v1/list)
- [Sign In](react-components/src/components/SignIn)
- [Create Account](react-components/src/components/CreateAccount)
- [Mini Account](ui.apps/src/main/content/jcr_root/apps/core/cif/components/content/miniaccount/v2/miniaccount)
- [Account Details](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/accountdetails/v2/accountdetails)
- [Address Book](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/addressbook/v2/addressbook)
- [Reset Password](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/resetpassword/v2/resetpassword)
- [Category Carousel](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/categorycarousel/v1/categorycarousel)
- [Commerce Experience Fragment](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/experiencefragment/v1/experiencefragment)
- [Commerce Content Fragment](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/contentfragment/v1/contentfragment)
- [Commerce Content Fragment](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/contentfragment/v1/contentfragment)
- [Cart Details](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/cartdetails/v1/cartdetails)
- [Checkout Page](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/checkoutpage/v1/checkoutpage)

### Extension Components

-   [Product Recommendations](extensions/product-recs) - requires [Adobe Commerce Product Recommendations](https://docs.magento.com/user-guide/marketing/product-recommendations.html)

### Technical Preview

Components that are in technical preview may change in a none backward compatible way at any time. This includes all asepects and in particular the content structure, API, markup and styling if applicable. Do not use these components in production.

- [Product v3](ui.apps/src/main/content/jcr_root/apps/core/cif/components/commerce/product/v3/product) - Adobe Commerce EE only with version >= 2.4.4

## System Requirements

The latest version of the AEM CIF Core Components, require the below minimum system requirements:

| CIF Core Components | AEM as a Cloud Service | AEM 6.5 | AEM Commerce Add-On | Adobe Commerce | Java  |
|---------------------| ---------------------- | ------- | ------------------- | -------------- | ----- |
| 2.11.0              | Continual              | 6.5.8   | v2022.08.02.00      | 2.4.2 ee       | 8, 11 |

For a list of requirements for previous versions, see [Historical System Requirements](VERSIONS.md).

### AEM CIF Add-On

For AEM as a Cloud Service deployments this project requires the CIF Add-On provisioned on each [AEM as a Cloud Service](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content-and-commerce/home.html?lang=en) environment. The CIF Add-On is installed automatically, no extra deployment is needed. The CIF Add-On is also available for local development with AEM SDK from [Software Distribution portal](https://experience.adobe.com/#/downloads/content/software-distribution/en/aemcloud.html).

For AEM on-prem installations, this project requires the AEM Commerce Add-On for AEM 6.5 to improve the authoring experience by leveraging the product pickers, product assets view, and product consoles provided by the connector package. The AEM Commerce Add-On for AEM 6.5 is also available on the [Software Distribution portal](https://experience.adobe.com/#/downloads/content/software-distribution/en/aem.html). It must be installed separately.

### AEM Sites Core Components

This project relies on the [AEM Sites Core Components](https://github.com/adobe/aem-core-wcm-components). They are typically installed as part of AEM. If you install AEM without sample content option you have to [deploy them manually](https://github.com/adobe/aem-core-wcm-components#installation) before using the AEM CIF Core Components.

### GraphQL Caching with Adobe Commerce 2.3.2

Starting with 2.3.2, Adobe Commerce supports cache-able GraphQL requests and starting with version 0.2.1 the CIF core components will use it by default. To make the components work with Adobe Commerce 2.3.1 you can manually disable this feature in the following locations:

-   For client-side components: [CommerceGraphqlApi.js](https://github.com/adobe/aem-core-cif-components/blob/master/ui.apps/src/main/content/jcr_root/apps/core/cif/clientlibs/common/js/CommerceGraphqlApi.js)

## Installation

1. Clone this repository.
2. Run a `mvn clean install` in the root folder to install the artifacts to your local Maven repository.
3. Switch to the `all` project and run a `mvn clean install content-package:install`.

Here is a full [video walk-through of the setup process](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/developing/develop.html).

### Easy install with the "all" package

If you want to build all the modules yourself and get all the latest (yet) **unreleased** changes, just build and install all the modules with the following command at the root of the repository:

```
mvn clean install -PautoInstallAll
```

This installs everything by default to `localhost:4502` without any context path. You can also configure the install location with the following maven properties:

-   `aem.host`: the name of the AEM instance
-   `aem.port`: the port number of the AEM instance
-   `aem.contextPath`: the context path of your AEM instance (if not `/`)

### UberJar

This project relies on the AEM 6.5.7 `cq-quickstart` UberJar. This is publicly available on <https://repo.adobe.com>

For more details about the UberJar please head over to the
[How to Build AEM Projects using Apache Maven](https://experienceleague.adobe.com/docs/experience-manager-65/developing/devtools/ht-projects-maven.html?lang=en) documentation page.

## Include core components as subpackage into your own project maven build

The released version of the AEM CIF Core Components are available on the [maven central repository](https://search.maven.org/search?q=g:com.adobe.commerce.cif%20AND%20a:core-cif-components-all). To include the
AEM CIF Core Components package into your own project maven build you can add the dependency

```
<dependency>
    <groupId>com.adobe.commerce.cif</groupId>
    <artifactId>core-cif-components-all</artifactId>
    <type>zip</type>
    <version>x.y.z</version>
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

## Configuration

To connect the AEM CIF Core Components with your Adobe Commerce environment or a 3rd party commerce deployment follow the [configuration steps](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/getting-started.html). For a multi store / site setup [additional steps](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/administering/multi-store-setup.html) are needed to link an AEM site to a Adobe Commerce store view configuration.

## Customization

For customizing CIF Core Components, we provide use cases and examples in our documentation at [Customizing CIF Core Components](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/developing/customize-cif-components.html).

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
npm run prettier:fix
```

## Releases to Maven Central

Releases of this project are triggered by manually running `mvn release:prepare release:clean` on the `master` branch on the root folder of this repository. Once you choose the release and the next snapshot versions, this commits the change along with a release git tag like for example `core-cif-components-reactor-x.y.z`. Note that the commits are not automatically pushed to the git repository, so you have some time to check your changes and then manually push them. The push then triggers a dedicated `CircleCI` build that performs the deployment of the tagged artifact to Maven Central.

_Important_: this project does Maven reactor releases, do **not** trigger releases from sub modules!

Note: in case it is needed to update the version of a java bundle because of API changes and semantic versioning, one can easily update the parent POM version and all the POMs referencing the parent POM version by running the following command in the PARENT project folder: `mvn versions:set -DnewVersion=x.y.z-SNAPSHOT`. This will ensure all projects have the same version.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
