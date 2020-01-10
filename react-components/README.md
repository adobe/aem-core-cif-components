# CIF Core Components - React Components

The React Components project is the code base for all the CIF Core Components built using [React](https://reactjs.org/). The following components are exposed by this library:

## Components related to the shopping cart

### Cart

An implementation of the "minicart" component, complete with checkout experience.

### CartProvider

A context provider for a cart component which provides state management for cart operations

### CartInitializer

A component which provides cart operations such as "Add to cart" and "Remove from cart"

### CheckoutProvider

A context provider for a cart component which provides state management for checkout operations

## Components related to user authentication / account management

### AuthBar

The account management components - Sign In, Forgot Password, Change Password and My Account, all wrapped into one component

### UserContextProvider

A context provider for user operations - sign in / sign-out, create user

## CommerceApp

A convenience wrapper for React components, encapsulating all the required contexts to run the app

## Building the project

The project is built using the command `npm run build`. The build process bundles all the code into one client library which is placed in `../ui.apps/src/main/content/jcr_root/apps/core/cif/clientlibs/react-components/dist`.

## Testing the components

This project uses [Jest](https://jestjs.io/) for running unit tests and [React Testing Library](https://testing-library.com/docs/react-testing-library/intro) as the testing framework. This framework allows you to test the behavior of the components rather than the implementations.

The unit tests are run using the `npm run test` command. To run the tests during development you can use `npm run test:watch` to start Jest in watch mode.

## Development

### Prerequisites

The React components access the Magento backend directly, so the calls have to be proxied in order to avoid CORS issues. For this you have to have the Dispacher proxy configured (see [the dispacher configuration](../dispatcher)) for details) and you have to access AEM through the dispatcher (i.e. use https://localhost instead of http://localhost:4502).

### Building

To build the components you can use

```
npm run build
```
