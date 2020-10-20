<!--
Copyright 2020 Adobe Systems Incorporated

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

# Carousel (v1)

Carousel component is a server-side component written in HTL, allowing to display a list of cards in carousel style.
This component is provided as a generic carousel which can be used for implementing specific carousel components.
Two carousel implementaions are provided based on this component:

-   [Product Carousel](../../../productcarousel/v1/productcarousel)
-   [Category Carousel](../../../categorycarousel/v1/categorycarousel)
 

## Features


- Display a list of cards
- Display a title 
- Carousel navigation with next/previous buttons
- Style System support.


### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

- `jcr:title` - optional title text
- `titleType` - the HTML heading element type used for rendering the title text. If missing, the component falls back to the type defined in the component policy of the specific carousel implementation

### Parameters

The Carousel component provides an HTL template with the following parameters:

- `carousel` - an object of type `com.adobe.cq.wcm.core.components.models.Component` 
               with the extra properties: `titleType` and `items`. 
               The `titleType`specifies the HTML element used to render the carousel title 
               and falls back to `h2` if missing.
               The `items` property holds the carousel items to display if the `items` 
               template parameter is missing. 
- `items` - the collection of items to display. If missing, `carousel.items` is used.
- `componentType` - a prefix for the HTML classes used in the component with default value `carousel`  


## BEM Description

```
BLOCK carousel
    ELEMENT carousel__btn
    ELEMENT carousel__btn--next
    ELEMENT carousel__btn--prev
    ELEMENT carousel__card
    ELEMENT carousel__cardscontainer
    ELEMENT carousel__cardsroot
    ELEMENT carousel__container    
    ELEMENT carousel__parent    
    ELEMENT carousel__title    
```

## Information

- **Vendor**: Adobe
- **Version**: v1
- **Compatibility**: AEM 6.4 / 6.5
- **Status**: production-ready
