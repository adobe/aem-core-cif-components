# MiniCart (v1)

The MiniCart is a client-side component which renders a shopping cart and exposes an API to interact with the cart items.

## Features

-   CRUD operations for shopping cart items

## API

The MiniCart component works with the Magento REST API for guest carts. It exposes a Javascript API so that consumers can use the shopping cart features in the AEM site.

### Dependencies

This component has several dependencies on internal client-side modules.

#### PageContext

This module holds context data for the whole page, such as the cart information (id) and the state of the masking overlay. It's accessible via the `window.CIF.PageContext` global.

#### CommerceApi

This module exposes an API which uses the Magento REST API to interact with the shopping cart. It's accessible via the `window.CIF.CommerceApi` global.

#### CommerceGraphqlApi

This module uses the Magento GrapqhQL API to retrieve data about the products. It's accessible via the `window.CIF.CommerceGraphqlApi` global.

#### Handlebars

The component uses Handlebars templates to render its different states (empty, editing etc.). The templates can be found in the [templates](./clientlib/js/templates) directory.

### Obtaining a MiniCart instance

The MiniCart instance is accessible via the `window.CIF.MiniCart` global.

### Creating a guest shopping cart

The component automatically creates a guest shopping cart when the page loads. The cart information (`cartId` and `cartQuote`) is stored in the `cif.cart` cookie (the format is `cartId#cartQuote`).

### Adding a product to cart

You can add a new product to the cart using the `addItem` API method of the MiniCart component.

```javascript
window.CIF.MiniCart.addItem(data);
```

The `data` argument is an object with two mandatory properties, `sku` - the SKU of the product variant and `qty` - the quantify.

Example:

```javascript 1.6
window.CIF.MiniCart.addItem({sku: 'VA03-LL-S', qty: '2'});
```

The call above adds two products with the sku `VA03-LL-S` to the cart.

This method returns a promise.

```javascript
{
    quantity: N; // the number of items in cart after the addition
}
```

### Removing a product from the cart.

You can remove a product from the cart by using the `removeItemHandler` API method.

```javascript
window.CIF.MiniCart.removeItemHandler(itemId);
```

The `itemId` is the ID of the shopping cart item (_NOT_ the SKU of the item).

This method returns a promise.

```javascript
{
    quantity: N; //the number of products left in the cart after the removal.
}
```

### Events

The component triggers the following DOM events:

-   `aem.cif.cart-intialized` - when the component is initialized. The number of items in the cart is sent as an event payload
-   `aem.cif.product-removed-from-cart` - when a product is removed from the cart. The number of items in the cart is sent as an event payload
-   `aem.cif.product-added-to-cart` - when a product is added to cart. The number of items in the cart is sent as an event payload

### CSS API (BEM)

The component is styled using CSS classes. The CSS class structure is the following:

```
BLOCK miniCart
    ELEMENT miniCart__root              
    ELEMENT miniCart__header
    ELEMENT miniCart__title
    ELEMENT miniCart__body
    ELEMENT miniCart__totals
    ELEMENT miniCart__subtotalLabel
    ELEMENT miniCart__subtotalValue

BLOCK product
    ELEMENT product__image
    ELEMENT product__name
    ELEMENT product__quantity
    ELEMENT product__quantityOperator
    ELEMENT product__price
    ELEMENT product__mask

BLOCK emptyMiniCart
    ELEMENT emptyMiniCart__root
    ELEMENT emptyMiniCart__emptyTitle
    ELEMENT emptyMiniCart__continue

```

## License information

* Vendor: Adobe
* Version: v1
* Compatibility: AEM 6.4 / 6.5
* Status: production-ready
