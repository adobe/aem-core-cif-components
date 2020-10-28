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
import { useTranslation } from 'react-i18next';
import { useMutation } from '@apollo/client';

import { useAwaitQuery } from '../../utils/hooks';

import Price from '../Price';
import Button from '../Button';
import Select from '../Select';
import classes from './cartOptions.css';

import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';
import MUTATION_UPDATE_CART_ITEM from '../../queries/mutation_update_cart_item.graphql';
import useCartOptions from './useCartOptions';

const CartOptions = () => {
    const [updateCartItemMutation] = useMutation(MUTATION_UPDATE_CART_ITEM);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);
    const [t] = useTranslation(['cart', 'common']);

    const [{ editItem }, { dispatch, updateCartItem }] = useCartOptions({
        updateCartItemMutation,
        cartDetailsQuery
    });

    const { product, quantity: initialQuantity, prices } = editItem;
    const { name } = product;
    const { value, currency } = prices.price;

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

    const handleUpdateClick = async () => {
        await updateCartItem(quantity);
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
                        <span>{t('cart:quantity', 'Quantity')}</span>
                    </h2>
                    <Select field="quantity" initialValue={quantity} onValueChange={handleOnChange} items={mockQtys} />
                </section>
            </div>
            <div className={classes.save}>
                <Button
                    onClick={() => {
                        dispatch({ type: 'endEditing' });
                    }}>
                    <span>{t('common:cancel', 'Cancel')}</span>
                </Button>
                <Button priority="high" onClick={handleUpdateClick}>
                    <span>{t('cart:update-cart', 'Update Cart')}</span>
                </Button>
            </div>
        </form>
    );
};

export default CartOptions;
