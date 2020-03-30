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
import { useCartState } from '../../components/Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';
import { useAwaitQuery } from '../../utils/hooks';
import QUERY_CUSTOMER_CART from '../../queries/query_customer_cart.graphql';

export default () => {
    const [, cartDispatch] = useCartState();
    const [{ order }, dispatch] = useCheckoutState();
    const [{ isSignedIn }, { dispatch: userDispatch }] = useUserContext();
    const fetchCustomerCart = useAwaitQuery(QUERY_CUSTOMER_CART);

    const continueShopping = async () => {
        // if it's signed in reset the cart
        if (isSignedIn) {
            const { data: customerCartData } = await fetchCustomerCart({
                fetchPolicy: 'network-only'
            });
            const customerCartId = customerCartData.customerCart.id;
            userDispatch({ type: 'setCartId', cartId: customerCartId });
        }
        // reset the cart from the cart state and the checkout state
        cartDispatch({ type: 'reset' });
        dispatch({ type: 'reset' });
    };
    return [{ orderId: order.order_id }, continueShopping];
};
