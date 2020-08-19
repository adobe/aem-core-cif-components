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

import { useAddressForm } from '../AddressForm/useAddressForm';
import { useAwaitQuery } from '../../utils/hooks';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';

import { setShippingAddressesOnCart as setShippingAddressesOnCartAction, getCartDetails } from '../../actions/cart';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_set_shipping_address.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

export default () => {
    const { parseAddress } = useAddressForm();
    const [{ cartId, cart }, cartDispatch] = useCartState();
    const [{ shippingAddress }, dispatch] = useCheckoutState();
    const [{ currentUser, isSignedIn }] = useUserContext();
    const [setShippingAddressesOnCart] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const beginCheckout = async () => {
        if (cart && ((cart.shipping_addresses && cart.shipping_addresses[0]) || cart.billing_address)) {
            // if shipping address is set previously on cart, fetch cart details to update it
            cartDispatch({ type: 'beginLoading' });
            await getCartDetails({ cartDetailsQuery, cartId, dispatch: cartDispatch });
            cartDispatch({ type: 'endLoading' });
        } else if (isSignedIn && !shippingAddress && currentUser.addresses.length > 0) {
            // If user is signed in but shipping address is not currently set via shipping address form nor on cart,
            // then use default address as initial address for address form if
            // there is one, otherwise use the first one of the saved addresses
            const address = currentUser.addresses.find(address => address.default_shipping) || currentUser.addresses[0];

            cartDispatch({ type: 'beginLoading' });
            await setShippingAddressesOnCartAction({
                cartDetailsQuery,
                setShippingAddressesOnCart,
                cartId,
                address: parseAddress(address),
                dispatch: cartDispatch
            });
            cartDispatch({ type: 'endLoading' });
        }
        dispatch({ type: 'beginCheckout' });
    };

    return { beginCheckout };
};
