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

import React, { useState } from 'react';
import { useMutation } from '@apollo/react-hooks';
import { Price } from '@magento/peregrine';

import Button from '../Button';
import Select from '../Select';
import classes from './cartOptions.css';
import LoadingIndicator from '../LoadingIndicator';

import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import MUTATION_UPDATE_CART_ITEM from '../../queries/mutation_update_cart_item.graphql';

import { useCartState } from '../../utils/state';

const CartOptions = () => {
    const [{ editItem, cartId }, dispatch] = useCartState();

    const { product, quantity: initialQuantity } = editItem;
    const { name, price: productPrice } = product;

    const { value, currency } = productPrice.regularPrice.amount;

    const [quantity, setQuantity] = useState(initialQuantity);

    const mockQtys = [
        {
            value: '1'
        },
        {
            value: '2'
        },
        {
            value: '3'
        },
        {
            value: '4'
        }
    ];

    const [updateCart, { loading, error }] = useMutation(MUTATION_UPDATE_CART_ITEM, {
        refetchQueries: [{ query: CART_DETAILS_QUERY, variables: { cartId } }]
    });

    if (error) {
        dispatch({ type: 'error', error: error });
    }

    const modalClass = loading ? classes.modal_active : classes.modal;

    const handleUpdateClick = () => {
        updateCart({ variables: { cartId, cartItemId: editItem.id, quantity } });
        dispatch({ type: 'endEditing' });
    };

    const handleOnChange = newVal => {
        setQuantity(parseInt(newVal));
    };

    return (
        <form className={classes.root}>
            <div className={classes.focusItem}>
                <span className={classes.name}>{name}</span>
                <span className={classes.price}>
                    <Price currencyCode={currency} value={value} />
                </span>
            </div>
            <div className={classes.form}>
                <section className={classes.quantity}>
                    <h2 className={classes.quantityTitle}>
                        <span>Quantity</span>
                    </h2>
                    <Select field="quantity" initialValue={quantity} onValueChange={handleOnChange} items={mockQtys} />
                </section>
            </div>
            <div className={classes.save}>
                <Button
                    onClick={() => {
                        dispatch({ type: 'endEditing' });
                    }}>
                    <span>Cancel</span>
                </Button>
                <Button priority="high" onClick={handleUpdateClick}>
                    <span>Update Cart</span>
                </Button>
            </div>
            <div className={modalClass}>
                <LoadingIndicator>Updating Cart</LoadingIndicator>
            </div>
        </form>
    );
};

export default CartOptions;
