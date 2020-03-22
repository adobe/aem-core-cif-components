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
import { useMutation } from '@apollo/react-hooks';
import { useCallback, useState } from 'react';
import MUTATION_PLACE_ORDER from '../../queries/mutation_place_order.graphql';

import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';

export default () => {
    const [{ shippingAddress, shippingMethod, paymentMethod }, checkoutDispatch] = useCheckoutState();
    const [{ cart, cartId }, cartDispatch] = useCartState();

    const [placeOrder] = useMutation(MUTATION_PLACE_ORDER);
    const [inProgress, setInProgress] = useState(false);

    const submitOrder = async () => {
        setInProgress(true);
        try {
            const { data } = await placeOrder({ variables: { cartId } });
            setInProgress(false);
            checkoutDispatch({ type: 'placeOrder', order: data.placeOrder.order });
        } catch (error) {
            console.error(error);
            setInProgress(false);
            cartDispatch({ type: 'error', error: error.toString() });
        }
    };

    return [
        { shippingAddress, shippingMethod, paymentMethod, inProgress, cart },
        { placeOrder: submitOrder, checkoutDispatch }
    ];
};
