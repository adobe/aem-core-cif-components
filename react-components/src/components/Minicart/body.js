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
import React from 'react';
import { useTranslation } from 'react-i18next';

import LoadingIndicator from '../LoadingIndicator';

import EmptyMinicartBody from './emptyMinicartBody';
import classes from './body.css';
import ProductList from './productList';
import CartOptions from './cartOptions';
import Error from './error';
import CouponForm from './couponForm';
import CouponItem from './couponItem';

import { useCartState } from './cartContext';
import DiscountList from './discountList';

const CartLoadingIndicator = () => {
    const [t] = useTranslation('cart');

    return <LoadingIndicator>{t('cart:fetching-data', 'Fetching cart data...')}</LoadingIndicator>;
};

const Body = () => {
    const [{ isEditing, cart, isLoading, errorMessage }] = useCartState();
    const isEmpty = cart && Object.entries(cart).length > 0 ? cart.items.length === 0 : true;

    if (isLoading) {
        return <CartLoadingIndicator />;
    }

    if (errorMessage) {
        return <Error />;
    }

    if (isEmpty) {
        return <EmptyMinicartBody />;
    }

    if (isEditing) {
        return <CartOptions />;
    }

    const cartItems = cart.items;
    const couponFragment = !cart.applied_coupon ? <CouponForm /> : <CouponItem />;
    const discountFragment =
        cart.prices.discounts && cart.prices.discounts.length > 0 ? (
            <DiscountList discounts={cart.prices.discounts} />
        ) : (
            <div />
        );

    return (
        <div className={classes.root}>
            <ProductList cartItems={cartItems} />
            {discountFragment}
            {couponFragment}
        </div>
    );
};

export default Body;
