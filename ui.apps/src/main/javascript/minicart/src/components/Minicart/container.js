
import React, { useEffect, useState } from 'react';
import { useQuery } from '@magento/peregrine';

import Minicart from './minicart';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import getCurrencyCode from '../../utils/getCurrencyCode';

const CART_ID = 'hx7geWblhhU0znC4rFPR166UvNy2Mp1k';

const Container = props => {
    const [isOpen, setIsOpen] = useState(false);
    const [isEditing, setIsEditing] = useState(false);

    const [queryResult, queryApi] = useQuery(CART_DETAILS_QUERY);
    const { data, error, loading } = queryResult;
    const { resetState, runQuery, setLoading } = queryApi;

    useEffect(() => {
        console.log(`Running the query...`);
        setLoading(true), runQuery({ variables: { cartId: CART_ID } });
    }, [runQuery, setLoading, isOpen]);

    useEffect(() => {
        document.addEventListener('aem.cif.open-cart', event => {
            setIsOpen(true);
        });
    });

    const handleCloseCart = () => {
        setIsOpen(false);
    };

    const handleBeginEditing = () => {
        setIsEditing(true);
    };

    const cart = {
        isLoading: loading,
        isEmpty: !data || !data.cart || data.cart.items.length === 0,
        details: data && data.cart,
        currencyCode: data ? getCurrencyCode(data.cart) : ''
    };

    console.log(data);
    console.log(`Is this loading? ${loading}`);
    return <Minicart isOpen={isOpen} handleCloseCart={handleCloseCart} cart={cart} />;
};

export default Container;
