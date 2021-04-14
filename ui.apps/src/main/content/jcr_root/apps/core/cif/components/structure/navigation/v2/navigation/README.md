<!--
Copyright 2019 Adobe Systems Incorporated

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Commerce Navigation (v2)

Commerce Navigation component is a server-side component written in HTL, rendering a combined AEM page and commerce catalog navigation tree.
The category tree details are retrieved from Magento via GraphQL. The main usage of this component would be as part of all page templates which include navigation.
This component is based on Navigation component of the AEM WCM Core Components library.

## Features

- Combines AEM pages and commerce categories
- Supports multi-level navigation
- Optionally can use catalog page as the main anchor for commerce categories

### Use Object

The Navigation component uses the `com.adobe.cq.wcm.core.components.models.Navigation` Sling model as its Use-object.

### Properties

This component has the following properties defined by the Navigation component of the AEM Core Components library.

1. `./navigationRoot` - defines the site's navigation root for which to build the navigation tree
2. `./structureStart` -  the start level of the navigation structure relative to the navigation root
3. `./structureDepth` - defines the category navigation structure depth, valid values are in the interval [1, 10]
4. `./skipNavigationRoot` - it should be always `true` or omitted as it defaults to `true`, to include only the descendants of the navigation root and exclude the root itself
5. `./collectAllPages` - it should be always `false` to honor the `./structureDepth` property and avoid collecting all pages that are descendants of the `./navigationRoot`
6. `./disableShadowing` - for redirecting pages PageA -> PageB. If true - PageA(original page) is shown. If false or not configured - PageB(target page)
7. `./accessibilityLabel` - defines an accessibility label for the navigation
8. `./id` - defines the component HTML ID attribute

Indirectly references the following properties of the [catalog page](/ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/catalogpage/v1/catalogpage):

1. `./magentoRootCategoryId` - the Magento root category id used as entry point for commerce categories in the navigation
2. `./showMainCategories` - show the top level commerce categories directly or use catalog page as entry point

## BEM Description

See the BEM description of the [Navigation component](https://github.com/adobe/aem-core-wcm-components/tree/master/content/src/content/jcr_root/apps/core/wcm/components/navigation/v1/navigation#bem-description) of the AEM WCM Core Components library.

## Information

- **Vendor**: Adobe
- **Version**: v2
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
