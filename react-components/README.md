# CIF Core Components - React Components

The React Components project is the code base for all the CIF Core Components built using [React](https://reactjs.org/). The following components are part of this project:

-   Minicart
-   Checkout
-   Sign In
-   My Account
-   Password recovery (Forgot Password)

## Building the project

The project is built using the command `npm run build`. The build process bundles all the code into one client library which is placed in `../ui.apps/src/main/content/jcr_root/apps/core/cif/clientlibs/react-components/dist`.

## Testing the components

This project uses [Jest](https://jestjs.io/) for running unit tests and [React Testing Library](https://testing-library.com/docs/react-testing-library/intro) as the testing framework. This framework allows you to test the behavior of the components rather than the implementations.

The unit tests are run using the `npm run test` command. To run the tests during development you can use `npm run test:watch` to start Jest in watch mode.

## Development

### Prerequisites

The React components access the Magento backend directly, so the calls have to be proxied in order to avoid CORS issues. For this you have to have the Dispacher proxy configured (see [the dispacher configuration](../dispatcher)) for details) and you have to access AEM through the dispatcher (i.e. use https://localhost instead of http://localhost:4502).

### Building

To build the components and install them without having to install the whole content package every time you can use the following command

```
npm run build:clientlib
```

This command calls the `build-scripts/postbuild.js` script which in turn calls the [repo](https://github.com/Adobe-Marketing-Cloud/tools/tree/master/repo) tool to install the client library in AEM, so make you sure you have this tool installed.

On MacOs you can install it using Homebrew

```
brew tap adobe-marketing-cloud/brews
brew install adobe-marketing-cloud/brews/repo
```

For other operating systems consult the tool's README on Github.
