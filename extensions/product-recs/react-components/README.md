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

### AccountContainer

The same as AuthBar, but rendered in a dropdown instead of a panel.

### AddressBook

A component that allows authenticated shoppers to manage their address books.

### UserContextProvider

A context provider for user operations - sign in / sign-out, create user

## General purpose components

## CommerceApp

A convenience wrapper for React components, encapsulating all the required contexts to run the app

## graphqlAuthLink

An ApolloLink instance that handles authorization on certain graphql requests (e.g.: cart mutations).
This is intended to be used when creating a new ApolloClient instance:

```javascript
import { ApolloClient, from, HttpLink, InMemoryCache } from '@apollo/client';
import { graphqlAuthLink } from '@adobe/aem-core-cif-react-components';

const client = new ApolloClient({
    link: from([graphqlAuthLink, new HttpLink({ uri: graphqlEndpoint, headers: { Store: storeView } })]),
    cache: new InMemoryCache()
});
```

### Portal

A component that allows rendering a React tree inside of a Portal. Uses `ReactDOM.createPortal()` under the hood.

###

## Building the project

The project is built using the command `npm run build`. The build process bundles all the code into one client library which is placed in `../ui.apps/src/main/content/jcr_root/apps/core/cif/clientlibs/react-components/dist`.

## Testing the components

This project uses [Jest](https://jestjs.io/) for running unit tests and [React Testing Library](https://testing-library.com/docs/react-testing-library/intro) as the testing framework. This framework allows you to test the behavior of the components rather than the implementations.

The unit tests are run using the `npm run test` command. To run the tests during development you can use `npm run test:watch` to start Jest in watch mode.

## Development

### Prerequisites

For development, please have [node.js](https://nodejs.org/) (v12+) and [npm](https://www.npmjs.com/get-npm) (v6+) installed.

The React components access the Magento GraphQL endpoint directly, so all calls have to either be served from the same endpoint as AEM or served via a proxy that adds CORS headers.

To start a local proxy server, you can use the following command:

```
npx local-cors-proxy --proxyUrl https://my.magento.cloud --port 3002 --proxyPartial ''
```

The GraphQL endpoint is then available at `http://localhost:3002/graphql`.

If you develop for AEM on-prem installations, a proxy is included in our sample Dispatcher configuration (see [the dispacher configuration](../dispatcher) for details). You have to access AEM through the dispatcher (i.e. use https://localhost instead of http://localhost:4502).

### Building

To build the components you can use

```
npm run build
```
