import React from 'react';
import ReactDOM from 'react-dom';

import ApolloClient from 'apollo-boost';
import { ApolloProvider } from 'react-apollo';
import { ApolloContext } from 'react-apollo/ApolloContext';

import Cart from './components/Minicart';

//require('dotenv').configure();

const App = () => {
    const client = new ApolloClient({
        //uri: process.env.MAGENTO_GRAPHQL_ENDPOINT
        uri: 'http://localhost/magento/graphql'
    });

    return (
        <ApolloContext.Provider value={client}>
            <ApolloProvider client={client}>
                <Cart />
            </ApolloProvider>
        </ApolloContext.Provider>
    );
};

window.onload = function() {
    const element = document.getElementById('minicart');
    ReactDOM.render(<App />, element);
};
