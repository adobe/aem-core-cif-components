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
# Product Recommendations (v1)

Product Recommendations is a client-side component written in React which fetches and renders product recommendations for Adobe Commerce.

This AEM component only renders a container `div` for the [React component](/extensions/product-recs/react-components/src/components/ProductRecsGallery). 
All configurations, either of the component or it's content policy are made available as data attributes to this host element:

- `data-title` - the configured title of the component.
- `data-preconfigured` - will be set if the product recommendations component should use the recommendations pre-configured by Adobe Commerce. 
- `data-recommendation-type` - contains one of the allowed recommendation types, if pre-configured is not set.
- `data-category-inclusions` - contains categories to include, if pre-configured is not set and if using category inclusion filter.
- `data-category-exclusions` - contains categories to exclude, if pre-configured is not set and if using category exclusion filter.
- `data-include-min-price` - contains the minimum price to include, if pre-configured is not set and if using price range inclusion filter.
- `data-include-max-price` - contains the maximum price to include, if pre-configured is not set and if using price range inclusion filter.
- `data-exclude-min-price` - contains the minimum price to exclude, if pre-configured is not set and if using price range exclusion filter.
- `data-exclude-max-price` - contains the maximum price to exclude, if pre-configured is not set and if using price range exclusion filter.
- `data-show-add-to-wish-list` - will be set if the add to favourites list button is enabled.

## Features

* Allows to use pre-configured recommendation settings, or to configure different recommendation types
* Allows to configure one of category or price range filters 
* Style System support.

### Use Object

The Product Recommendations component uses the `com.adobe.cq.commerce.extensions.recommendations.models.productrecommendations.ProductRecommendations` Sling model as its Use-object.

### Component Policy Configuration Properties

The following configuration properties are used:

1. `./allowedRecTypes` - a list of allowed recommendation types, empty per default
2. `./enableAddToWishList` - enables/disables the add to favorites list button, disabled per default

### Edit Dialog Properties

The following properties are written to JCR for this component and are expected to be available as `Resource` properties:

1. `./preconfigured` - if enabled, the product recommendations as configured in Adobe Commerce will be shown, enabled per default
2. `./jcr:title` - sets the headline of the product recommendations gallery
3. `./recommendationType` - sets the recommendation type to query from a list of allowed recommendation types
4. `./usedFilter` - sets the filter to be used, one of `none`, `./includedCategories`, `./includedPriceRange`, `./excludedCategories` or `./excludedPriceRange`
5. `./includedCategories` - sets the categories to include when the category inclusion filter is used
6. `./excludedCategories` - sets the categories to exclude when the category exclusion filter is used
7. `./includedPriceRangeMin` - sets the minimum price to include when the price range inclusion filter is used, type Double
8. `./includedPriceRangeMax` - sets the maximum price to include when the price range inclusion filter is used, type Double
9. `./excludedPriceRangeMin` - sets the minimum price to exclude when the price range exclusion filter is used
10. `./excludedPriceRangeMax` - sets the maximum price to exclude when the price range exclusion filter is used

## BEM Description

```
BLOCK productrecommendations    
```

## License information

-   Vendor: Adobe
-   Version: v1
-   Compatibility: AEM as a Cloud Service / 6.5
-   Status: production-ready
