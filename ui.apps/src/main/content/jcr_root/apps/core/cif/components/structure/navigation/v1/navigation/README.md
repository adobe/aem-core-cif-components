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

# Commerce Navigation (v1)

Commerce Navigation component is a server-side component written in HTL, rendering a combined AEM page and commerce catalog navigation tree.
The category tree details are retrieved from Magento via GraphQL. The main usage of this component would be as part of all page templates which include navigation.
This component is based on Navigation component of the AEM Core Components library.

## Features

- Combines AEM page and commerce categories
- Supports multi-level navigation
- Optionally can use catalog page as the main anchor for commerce categories

### Use Object

The Navigation component uses the `com.adobe.cq.commerce.core.components.models.navigation.NavigationModel` Sling model as its Use-object.

### Properties

This component has the following properties defined by the Navigation component of the AEM Core Components library.

1. `./navigationRoot` - defines the site's navigation root for which to build the navigation tree
2. `./structureDepth` - defines the category navigation structure depth, valid values are in the interval [1, 10]
3. `./skipNavigationRoot` - it should be always `true` or omitted as it defaults to `true`, to include only the descendants of the navigation root and exclude the root itself
4. `./collectAllPages` - it should be always `false` to honor the `./structureDepth` property and avoid collecting all pages that are descendants of the `./navigationRoot`

Indirectly references the following properties of the [catalog page](/ui.apps/src/main/content/jcr_root/apps/core/cif/components/structure/catalogpage/v1/catalogpage):

1. `./magentoRootCategoryId` - the Magento root category id used as an entry point for the navigation
2. `./showMainCategories` - show the main top level categories directly or use catalog page as entry point

## BEM Description

```
BLOCK navigation
    ELEMENT navigation__root
    ELEMENT navigation__body
    ELEMENT navigation__header

BLOCK categoryTree
    ELEMENT categoryTree__root
    ELEMENT categoryTree__root--shadow
    ELEMENT categoryTree__tree

BLOCK categoryLeaf
    ELEMENT categoryLeaf__root
    ELEMENT categoryLeaf__root--box
    ELEMENT categoryLeaf__root--link
    ELEMENT categoryLeaf__text

BLOCK trigger__root
    ELEMENT trigger

BLOCK icon
    ELEMENT icon__root
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
