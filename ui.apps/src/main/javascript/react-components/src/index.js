import React from 'react';
import ReactDOM from 'react-dom';
import ApolloClient from 'apollo-boost';
import { ApolloProvider } from '@apollo/react-hooks';

import {
    CartProvider,
    initialState,
    reducerFactory,
    CheckoutProvider,
    initialState as initialCheckoutState,
    reducer,
    CartInitializer,
    AuthBar
} from '@adobe/aem-core-cif-react-components';

const App = () => {
    const storeView = document.querySelector('body').dataset.storeView || 'default';

    const client = new ApolloClient({
        uri: '/magento/graphql',
        headers: { Store: storeView }
    });

    return (
        <ApolloProvider client={client}>
            <CartProvider initialState={initialState} reducerFactory={reducerFactory}>
                <UserContextProvider>
                    <CartInitializer>
                        <CheckoutProvider initialState={initialCheckoutState} reducer={reducer}>
                            <Cart />
                            <AuthBar />
                        </CheckoutProvider>
                    </CartInitializer>
                </UserContextProvider>
            </CartProvider>
        </ApolloProvider>
    );
};

window.onload = function() {
    const element = document.getElementById('minicart');
    ReactDOM.render(<App />, element);
};
