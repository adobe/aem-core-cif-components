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
import { shape, string, array } from 'prop-types';
import Cart from './cart';
import Form from './form';
import classes from './flow.css';
import Receipt from './receipt';
import { useCartState } from '../../utils/state';

const isCartReady = cart => {
    return cart && cart.items.length > 0;
};

const Flow = props => {
    const { cart } = props;
    const [{ cartId }] = useCartState();

    const [flowState, setFlowState] = useState('cart');
    const [order, setOrder] = useState({});

    const beginCheckout = () => {
        setFlowState('form');
    };

    const cancelCheckout = () => {
        setFlowState('cart');
    };

    const orderCreated = order => {
        setOrder(order);
        setFlowState('receipt');
    };

    let child;

    switch (flowState) {
        case 'cart': {
            child = <Cart beginCheckout={beginCheckout} ready={isCartReady(cart)} submitting={false} />;
            break;
        }
        case 'form': {
            child = <Form cancelCheckout={cancelCheckout} cart={{ ...cart, cartId }} receiveOrder={orderCreated} />;
            break;
        }
        case 'receipt': {
            child = <Receipt order={order} />;
            break;
        }
        default: {
            child = null;
        }
    }

    return <div className={classes.root}>{child}</div>;
};

Flow.propTypes = {
    cart: shape({
        shipping_addresses: array,
        email: string
    })
};

export default Flow;
