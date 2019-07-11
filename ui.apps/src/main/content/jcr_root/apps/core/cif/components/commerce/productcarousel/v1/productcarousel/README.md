<!--/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ 
  ~ Copyright 2019 Adobe. All rights reserved.
  ~ This file is licensed to you under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License. You may obtain a copy
  ~ of the License at http://www.apache.org/licenses/LICENSE-2.0
  ~ 
  ~ Unless required by applicable law or agreed to in writing, software distributed under
  ~ the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  ~ OF ANY KIND, either express or implied. See the License for the specific language
  ~ governing permissions and limitations under the License.
  ~ 
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/-->
Product Carousel (v1)
====
Product Carousel component is a component written in HTL and Javascript. The products 
are retrieved from Magento via GraphQL. Component can be used on experience and landing page to
provide visitors a glimpse of products. 

## Features

* Displays product name, product image and default price.
* Site visitor  can click on the product which is linked to  PDP.
* Products can be selected using Product Picker.
* Displays Sorted list of products on the basis of products selected in Dialog Box.


### Use Object
The Product Carousel component uses the `com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel` Sling model as its Use-object.


### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./product` - identifies the Product selected using Product Picker

## BEM Description
```
BLOCK productcarousel
    ELEMENT productcarousel__root
    ELEMENT productcarousel__parent
    ELEMENT productcarousel__cardscontainer
    ELEMENT productcarousel__btn
    ELEMENT productcarousel__btn--next
    ELEMENT productcarousel__btn--prev
    
BLOCK product
    ELEMENT product__card--image
    ELEMENT product__image
    ELEMENT product__price

```

## Information
* **Vendor**: Adobe
* **Version**: v1
* **Compatibility**: AEM 6.4 / 6.5
* **Status**: production-ready