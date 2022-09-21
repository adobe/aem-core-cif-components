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

# Experience Platform Connector

This Javascript library collects Storefront events and forwards them to Adobe Experience Platform and/or Adobe Commerce Data Service.

It exports the `EventCollectorContext` React Context which
- loads the magento-storefront-events-sdk and magento-storefront-event-collector libray, 
- initialises them with a given configuration for AEP and/or ACDS
- subscribes to all events from Peregrine and forwards them to the events SDK

## Usage

The `EventCollectorContext` requires the Peregrine user context. So it must be used in a React component that is wrapped inside that context.

```javascript
export const App = () => {
    <PeregrineContext>
        <EventCollectorContextProvider acds={true} aep={{ orgId: '...', datastreamId: '...' }}>
            ...
        </EventCollectorContextProvider>
    <PeregrineContext>
}
```

As the `EventCollectorContext` loads the magento-storefront-events-sdk it is not recommended to use it together with the `core.cif.components.storefront-events.v1` which as well embeds the sdk. Instead the `EventCollectContext` provides the loaded and initialized sdk to descendant components. 

The sdk is still available as global variable on `window.magentoStorefrontEvents`, but it is recommended to use obtain it from the EventCollectorContext and pass it to the other Core CIF React Components as needed. This guarantees that logic that requires the sdk can reliable use it.

The same applies when it is being used together with the Core CIF Components Extension for Product Recommendations. Make sure that you enable the Adobe Commerce Data Service as this is required for Product Recommendations to work.

```javascript
export const AppContent = () => {
    const { sdk: mse } = useEventCollectorContext();

    useCustomUrlEvent({ mse });
    useReferrerEvent({ mse });
    usePageEvent({ mse });
    useAddToCartEvent({ mse });

    return (
        <>
            <StorefrontInstanceContextProvider mse={mse}>
                <PortalPlacer selector={'[data-is-product-recs]'} component={ProductRecsGallery} />
            </StorefrontInstanceContextProvider>
            ...
        </>
    );
}
export const App = () => {
    <PeregrineContext>
        <EventCollectorContextProvider acds={true} aep={{ orgId: '...', datastreamId: '...' }}>
            <AppContent/>
        </EventCollectorContextProvider>
    <PeregrineContext>
}
```

## Known Limitations

- for bundled products only the final price is provided without any information of applied discounts
- add-to-cart events for gift gard products are not yet supported
- add-to-cart events do not support client side price loading yet