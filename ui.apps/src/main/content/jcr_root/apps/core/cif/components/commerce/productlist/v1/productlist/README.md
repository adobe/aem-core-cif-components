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
Product List (v1)
====
Product List component is a server side component written in HTL, allowing to display product listings. The product listings
for a category are retrieved from Magento via GraphQL. The main usage of this component would be on a category page. 

## Features

* Support for pagination
* Configurable number of products on one page
* Configurable category title display
* Displays category image, if set in Magento and enabled in design dialog
* Client-side loaded prices using GraphQL. Can be disabled in the design dialog.
* Style System support.

### Use Object
The Product List component uses the `com.adobe.cq.commerce.core.components.models.productlist.ProductList` Sling model as its Use-object.

### Selectors & Request Parameters
This component is targeted for a category page listing products of a category.
1. The category identifier is retrieved form the first URL selector. 
2. `page` optional parameter to control the page cursor, default = 1

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./showTitle` - controls the visibility of the product category title
2. `./showImage` - controls the visibility of the product category image
3. `./loadClientPrice` - enables client-side price fetching
4. `./paginationType` - the pagination type, either `paginationbar` or `loadmorebutton`.

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./pageSize` - the number of products shown on one page

## BEM Description
```
BLOCK category
    ELEMENT category__root
    ELEMENT category__root-message
    ELEMENT category__title
    ELEMENT category__pagination
    ELEMENT category__categoryTitle
    ELEMENT category__image
    
BLOCK gallery
    ELEMENT gallery__root
    ELEMENT gallery__items

BLOCK item    
    ELEMENT item__root
    ELEMENT item__images
    ELEMENT item__image
    ELEMENT item__imagePlaceholder
    ELEMENT item__name
    ELEMENT item__price
```

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.4 / 6.5
* **Status**: production-ready