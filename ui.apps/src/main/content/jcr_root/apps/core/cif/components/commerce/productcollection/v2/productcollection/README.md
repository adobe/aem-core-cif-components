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
Product Collection (v2)
====
Product Collection component is a server side component written in HTL, 
allowing to display a collection of products. 

## Features

* Support for pagination
* Configurable number of products on one page
* Client-side loaded prices using GraphQL. Can be disabled in the design dialog.
* Style System support.

### Use Object
The Product Collection component uses the `com.adobe.cq.commerce.core.components.models.productcollection.ProductCollection` Sling model as its Use-object.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./loadClientPrice` - enables client-side price fetching
2. `./enableAddToCart` - displays the 'Add to Cart' button on the products (default `false`) 
3. `./enableAddToWishList` - displays the 'Add to Wish List' button on the products (default `false`)  
4. `./paginationType` - the pagination type, either `paginationbar` or `loadmorebutton`.

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./pageSize` - the number of products shown on one page
2. `./defaultSortField` - the default sort field for products
3. `./defaultSortOrder` - the default sort order for products
4. `./id` - defines the component HTML ID attribute

## BEM Description
```
BLOCK productcollection
    ELEMENT productcollection__root
    ELEMENT productcollection__results-count
    ELEMENT productcollection__filters
    ELEMENT productcollection__filters-header
    ELEMENT productcollection__filters-title
    ELEMENT productcollection__filters-body
    ELEMENT productcollection__current-filters
    ELEMENT productcollection__current-filter
    ELEMENT productcollection__current-filter-icon
    ELEMENT productcollection__filter
    ELEMENT productcollection__filter-toggler
    ELEMENT productcollection__filter-header
    ELEMENT productcollection__filter-icon
        MOD productcollection__filter-icon--open
        MOD productcollection__filter-icon--closed
    ELEMENT productcollection__filter-items
    ELEMENT productcollection__filter-item
    ELEMENT productcollection__filter-title
    ELEMENT productcollection__items
    ELEMENT productcollection__item
        MOD productcollection__item--staged
    ELEMENT productcollection__item-actions
    ELEMENT productcollection__item-button
        MOD productcollection__item-button--add-to-cart
        MOD productcollection__item-button--add-to-wish-list
    ELEMENT productcollection__item-button-content
    ELEMENT productcollection__item-images
    ELEMENT productcollection__item-image
        MOD productcollection__item-image--placeholder
    ELEMENT productcollection__item-staged
    ELEMENT productcollection__item-title
    ELEMENT productcollection__sort
    ELEMENT productcollection__sort-fields
    ELEMENT productcollection__sort-title
    ELEMENT productcollection__sort-keys
    ELEMENT productcollection__sort-order
        MOD productcollection__sort-order--asc
        MOD productcollection__sort-order--desc
    ELEMENT productcollection__loadmore-button
    ELEMENT productcollection__loadmore-spinner
    ELEMENT productcollection__pagination
    ELEMENT productcollection__pagination-arrow
        MOD productcollection__pagination-arrow--prev
        MOD productcollection__pagination-arrow--next
        MOD productcollection__pagination-arrow--inactive
    ELEMENT productcollection__pagination-icon
    ELEMENT productcollection__pagination-button
        MOD productcollection__pagination-button--current
        MOD productcollection__pagination-button--inactive
    ELEMENT productcollection__pagination-button-title
```

## Information
* **Vendor**: Adobe
* **Version**: v2
* **Compatibility**: AEM as a Cloud Service / AEM 6.5
* **Status**: production-ready
