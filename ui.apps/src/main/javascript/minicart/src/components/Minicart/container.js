/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
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
