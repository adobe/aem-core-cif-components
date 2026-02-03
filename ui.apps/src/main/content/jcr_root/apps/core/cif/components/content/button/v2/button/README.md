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

# Button(v2) 

Button Component v2 meant to be placed on AEM pages and have the option to link it to 
- AEM pages 
- Product 
- Category 
- or an External Link.

## Features

* Component is avaialble for Authors in editor
* Display text on the button and provide edit capabilities to change text on the button.
  Display default text as Label
* Default button color: black
* Dialog can be used to configure Label and Link Type
* Style System support.

### Implementing class
com.adobe.cq.commerce.core.components.internal.models.v2.button.ButtonImpl

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:
1) `./linkType` identifies the Link Type to be connected with Button
2) `./linkTo`  identifies the Link To AEM page property
3) `./productSku` identifies the Product to be connected with Button
4) `./categoryId`  identifies the  Category to be connected with Button
5) `./externalLink` identifies the External Link to be connected with Button
6) `./categoryIdType` identifies the Category Id Type to be connected with Button

## BEM Description

```
BLOCK cmp-button
    ELEMENT cmp-button__text
    ELEMENT cmp-button__icon
        MOD cmp-button__icon--<icon>
```

## Information

* **Vendor**: Adobe
* **Version**: v2
* **Compatibility**: AEM as a Cloud Service / AEM 6.5
* **Status**: production-ready
