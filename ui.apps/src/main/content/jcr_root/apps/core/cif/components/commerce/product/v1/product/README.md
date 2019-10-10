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
Product (v1)
====
Product component is a server side component written in HTL, allowing to display product details. The product details are retrieved from Magento via GraphQL using the product slug provided in the URL. The main usage of this component would be on a product page.

## Features

* Supports simple and configurable products in Magento with variant selection.
* Displays product gallery.
* Displays "add to cart" button to add a simple product or selected variant to the cart.
* Client-side loaded prices using GraphQL. Can be disabled in the design dialog.
* Variant selection respects variant SKUs in location hash (e.g. `#variant-a`) and supports the browser history API.
* Style System support.

### Use Object
The Product component uses the `com.adobe.cq.commerce.core.components.models.product.Product` Sling model as its Use-object.

### Selectors & Request Parameters
This component is targeted for a product page showing details of a single simple or configurable product.
1. The product SKU is retrieved form the first URL selector. 

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./loadClientPrice` - enables client-side price fetching

## BEM Description
```
BLOCK product
    ELEMENT productFullDetail__root
    ELEMENT productFullDetail__title
    ELEMENT productFullDetail__productName
    ELEMENT productFullDetail__quantity
    ELEMENT productFullDetail__section
    ELEMENT productFullDetail__sectionTitle
    ELEMENT productFullDetail__description
    ELEMENT productFullDetail__descriptionTitle
    ELEMENT richText__root
    ELEMENT productFullDetail__details
    ELEMENT productFullDetail__detailsTitle
    ELEMENT productFullDetail__productPrice

BLOCK gallery
    ELEMENT productFullDetail__imageCarousel
    ELEMENT carousel__root
    ELEMENT carousel__imageContainer
    ELEMENT carousel__chevron-left
    ELEMENT carousel__chevron-right
    ELEMENT icon__root
    ELEMENT carousel__currentImage
    ELEMENT thumbnailList__root
    ELEMENT thumbnail__root
    ELEMENT thumbnail__image

BLOCK variantselector
    ELEMENT productFullDetail__options
    ELEMENT option__root
    ELEMENT option__title
    ELEMENT swatchList__root
    ELEMENT swatch__root
    ELEMENT clickable__root
    ELEMENT tileList__root
    ELEMENT tile__root

BLOCK quantity
    ELEMENT productFullDetail__quantityTitle
    ELEMENT quantity__root
    ELEMENT fieldIcons__root
    ELEMENT fieldIcons__input
    ELEMENT select__input
    ELEMENT field__input
    ELEMENT fieldIcons__before
    ELEMENT fieldIcons__after

BLOCK addtocart
    ELEMENT productFullDetail__cartActions
    ELEMENT button__root_highPriority
    ELEMENT button__root clickable__root
    ELEMENT button__filled
    ELEMENT button__content
```

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.4 / 6.5
* **Status**: production-ready