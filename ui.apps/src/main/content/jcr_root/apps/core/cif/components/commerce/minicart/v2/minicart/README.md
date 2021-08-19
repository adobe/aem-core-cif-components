# MiniCart (v2)

The MiniCart is a client-side component written in React which renders the shopping cart and the checkout form

## Features

-   CRUD operations for shopping cart items
-   Complete checkout process in three steps:
    -   Shipping address
    -   Payment method / Billing Address
    -   Shipping Method

## Implementation

This AEM component only renders a container `div` for the [React component](/react-components/src/components/Minicart).

## Using the MiniCart

### URL routing

This component requires the [AEM Dispatcher with GraphQL routing](/dispatcher) set up. By default Magento GraphQL endpoint will be accessed via relative URL `/magento/graphql`.

### Adding a product to cart

You can add a new product to the cart by triggering the `aem.cif.add-to-cart` DOM event, like in the following example

```javascript 1.6
const customEvent = new CustomEvent('aem.cif.add-to- cart', {
    detail: { sku: 'VA03-LL-S', quantity: 2 }
});
document.dispatchEvent(customEvent);
```

The call above adds two products with the sku `VA03-LL-S` to the cart.

## BEM Description

```
BLOCK minicart
    ELEMENT minicart__root
    ELEMENT minicart__trigger
    ELEMENT minicart__body    
```

## License information

-   Vendor: Adobe
-   Version: v2
-   Compatibility: AEM as a Cloud Service / 6.5
-   Status: production-ready
