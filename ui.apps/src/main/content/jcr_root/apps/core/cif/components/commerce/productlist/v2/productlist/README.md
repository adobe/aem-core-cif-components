<!--
Copyright 2021 Adobe Systems Incorporated

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
Product List (v2)
====
The version 2 of the productlist component extends the v2 of productcollection
component and v1 of productlist component model by extending the v1 GraphQL 
query with the `staged` field introduced in Magento 2.4.2 Enterprise Edition (EE). 
This hence requires that the Magento backend is at least version 2.4.2 EE 
because the query with the `staged` field will be rejected by Magento versions
not having this field in the GraphQL schema.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./loadClientPrice` - enables client-side price fetching
2. `./enableAddToCart` - displays the 'Add to Cart' button on the products (default `false`) 
3. `./enableAddToWishList` - displays the 'Add to Wish List' button on the products (default `false`)  
4. `./paginationType` - the pagination type, either `paginationbar` or `loadmorebutton`.

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./category` - the uid of the category to be displayed in the component, if missing the component displays a
                   category based on the page URL 
2. `./pageSize` - the number of products shown on one page
3. `./defaultSortField` - the default sort field for products
4. `./defaultSortOrder` - the default sort order for products
5. `./showTitle` - if true the component displays the category title
6. `./showImage` - if true the component displays the category image
7. `./id` - defines the component HTML ID attribute
8. `./fragments` - this is a multifield allowing configuration of content fragments to be inserted in the product list

## BEM Description

In addition to the elements documented for the version 2 of the productcollection component,
version 2 of productlist introduces these extra elements to display the details of a category 
and a "staged" flag on the category itself or its products. 
Note that this is only relevant for AEM author instances.

This version also introduces the possibility to introduce content fragment placeholders.
Using the edit dialog, placeholders containing the position of the placeholder in the grid
and the fragment "location" property can be configured. 
This will search for fragments configured for the current category and the configured location
and insert them at specified positions.

```
BLOCK category
    ELEMENT category__root
        MOD category__root--staged
        MOD category__root--noimage
        MOD category__root--notitle
    ELEMENT category__header
    ELEMENT category__image
    ELEMENT category__staged
    ELEMENT category__title
```

## Information
* **Vendor**: Adobe
* **Version**: v2
* **Compatibility**: AEM as a Cloud Service / AEM 6.5
* **Status**: production-ready
