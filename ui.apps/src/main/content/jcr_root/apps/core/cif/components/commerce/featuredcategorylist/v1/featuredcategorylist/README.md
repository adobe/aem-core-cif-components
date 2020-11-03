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

Featured category list is a component for displaying list of categories. Authors can configure multiple categories via `Multifield` and `Category Picker` and use them on pages for displaying featured categories.

## Features

- Selection of multiple categories via multifield and category picker. 
- Listing of categories with images.
- Override category images with AEM assets.
- Clicking on a category directs user to specific category page.
- Style System support.

## API

Featured Category List component uses graphql query to fetch categories to display image and name of the category .

### Dependencies

This component has several dependencies on internal client-side modules.

### Usage prerequisites

Make sure you have dispatcher running with Magento url configured. See `dispatcher/README.md`. You should have the category images configured in your Magento instance.

### Use Object

The Featured category list component uses the `com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList` Sling model as its Use-object.

### Component Policy Configuration Properties
The following configuration properties are used:

1. `./type` - defines the default HTML heading element type (`h1` - `h6`) this component will use for its rendering

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./jcr:title` - Optional title text
2. `./categoryIds` - Category ids in an Array of string saved by Category Picker.
3. `./titleType` - will store the HTML heading element type which will be used for rendering; if no value is defined, the component will fallback
to the `type` value defined by the component's policy. The property of the policy is called `type` so we can reuse the `core/wcm/components/commons/datasources/allowedheadingelements/v1` Servlet from the WCM components.

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
```

## License information

* Vendor: Adobe
* Version: v1
* Compatibility: AEM 6.4 / 6.5
* Status: production-ready
