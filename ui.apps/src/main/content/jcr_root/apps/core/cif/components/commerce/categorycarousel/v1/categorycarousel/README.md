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
# Category carousel (v1)

Category carousel is a component for displaying a list of categories in a carousel. Authors can configure multiple categories via `Multifield` and `Category Picker` and use them on pages for displaying featured categories.

## Features

- Display a list of cards
- Display a title 
- Carousel navigation with next/previous buttons
- Selection of multiple categories via multifield and category picker 
- Listing of categories with images
- Override category images with AEM assets
- Clicking on a category directs user to specific category page
- Style System support

## API

Category Carousel component uses graphql query to fetch categories to display image and name of the category .

### Dependencies

This component has several dependencies on internal client-side modules.

### Usage prerequisites

Make sure you have dispatcher running with Magento url configured. See `dispatcher/README.md`. You should have the category images configured in your Magento instance.

### Use Object

The Category Carousel component uses the `com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList` Sling model as its Use-object.

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

BLOCK categorycarousel           
    ELEMENT categorycarousel__anchor
    ELEMENT categorycarousel__card        
    ELEMENT categorycarousel__image   
    ELEMENT categorycarousel__imagewrapper
    ELEMENT categorycarousel__name
```

## License information

* Vendor: Adobe
* Version: v1
* Compatibility: AEM 6.4 / 6.5
* Status: production-ready
