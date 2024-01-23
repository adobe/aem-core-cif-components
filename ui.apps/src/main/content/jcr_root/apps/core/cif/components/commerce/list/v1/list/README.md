<!--
Copyright 2022 Adobe

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
Commerce List (v1)
====
Commerce List component written in HTL that renders a configurable collection of pages.

## Features
* Multiple sources:
  * List page children
  * List tagged items
  * List query result
  * List static items
  * List pages associated to product
  * List pages associated to category
* Ordering and limit
* Styles

### Use Object
The Commerce List component uses the `com.adobe.cq.wcm.core.components.models.List` Sling model as its Use-object.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./dateFormat` - defines the formatting string for when the list items are set to render their last modification date;
2. `./disableChildren` - allows to disable the ability to build a list from the child pages of a root page
3. `./disableStatic` - allows to disable the ability to build a list with static elements
4. `./disableSearch` - allows to disable the ability to build a list using search results
5. `./disableTags` - allows to disable the ability to build a list using the tagged child pages of a root page
6. `./disableProductAssociation` - allows to disable the ability to build a list using pages associated to a product
7. `./disableCategoryAssociation` - allows to disable the ability to build a list using pages associated to a category

### Edit Dialog Properties
The following properties are written to JCR for this List component and are expected to be available as `Resource` properties:

1. `./listFrom` - defines the source of this List; possible values:
  * `children` - the list is built from the child pages of a root page
  * `static` - the list is built from a statically defined collection of pages
  * `search` - the list is built from the search results of a query
  * `tags` - the list is built from the tagged children pages of a root page
  * `productAssociation` - the list is built from the pages associated to a product
  * `categoryAssociation` - the list is built from the pages associated to a category
2. `./parentPage` - defines the root page when the `./listFrom` property is set to `children`
3. `./childDepth` - defines the max depth for children pages, when the `./listFrom` property is set to `children`
4. `./pages` - defines the pages to be rendered, when the `./listFrom` property is set to `static`
5. `./query` - defines the search query, when the `./listFrom` property is set to `search`
6. `./searchIn` - defines where to start the search, when the `./listFrom` property is set to `search`
7. `./tagsSearchRoot` - defines the root path of the tag search, when the `./listFrom` property is set to `tags`
8. `./tags` - defines the tags list to search for, when the `./listFrom` property is set to `tags`
9. `./tagsMatch` - defines if the results of the tag search have to match all tags or just some of them,
when the `./listFrom` property is set to `tags`; possible values: `any` and `all`
10. `./product` - defines the product when `./listFrom` property is set to `productAssociation` 
11. `./category` - defines the category when `./listFrom` property is set to `categoryAssociation` 
12. `./orderBy` - defines what criterion is used for ordering the list items: the item's title or the
last modification date of the item; possible values: `title`, `modified`
13. `./sortOrder` - defines the sorting order; possible values: `asc`, `desc`
14. `./maxItems` - defines the maximum number of items rendered by the list
15. `./linkItems` - if set to `true` the list will link all items to the corresponding pages
16. `./showDescription` - if set to `true` each item's description will be rendered
17. `./showModificationDate` - if set to `true` each item's last modification date will be rendered
18. `./id` - defines the component HTML ID attribute.

## Client Libraries
The component uses the `core.wcm.components.list.v2.editor` editor client library category that includes JavaScript
handling for dialog interaction. It is already included by its edit dialog.

## BEM Description
```
BLOCK cmp-list
    ELEMENT cmp-list__item
    ELEMENT cmp-list__item-link
    ELEMENT cmp-list__item-title
    ELEMENT cmp-list__item-date
```

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.3
* **Status**: production-ready