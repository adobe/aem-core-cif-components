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
# Featured category list (v1)

Featured category list is a component for displaying list of categories. Authors can confgure multiple categories via `Category Picker` and use them on pages for displaying featured categories.

## Features

- Selection of multiple categories via category picker. 
- Listing of categories with images.
- Clicking on a category directs user to specific category page.
- Style System support.

## API

Featured Category List component uses graphql query to fetch categories to display image and name of the category .

### Dependencies

This component has several dependencies on internal client-side modules.

### Usage prerequisites
Make sure you have dispatcher running with magento url configured .See `dispatcher/README.md`. You should have the category images configured in your Magento instance.


### Use Object
The Featured category list component uses the `com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList` Sling model as its Use-object.


### Edit Dialog Properties
The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

 `./categoryIds` - Category ids in an Array of string saved by Category Picker  .

### CSS API (BEM)

The component is styled using CSS classes. The CSS class structure is the following:

```
BLOCK .cmp-categorylist 
    ELEMENT cmp-categorylist__content
    ELEMENT cmp-categorylist__anchor
    ELEMENT cmp-categorylist__imagewrapper
    ELEMENT cmp-categorylist__image
    ELEMENT cmp-categorylist__name
    ELEMENT cmp-categorylist__title 

BLOCK placeholder__empty
```

## License information

* Vendor: Adobe
* Version: v1
* Compatibility: AEM 6.4 / 6.5
* Status: production-ready
