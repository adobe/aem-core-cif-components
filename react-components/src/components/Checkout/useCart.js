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
import { useMutation } from '@apollo/client';

import { useAddressForm } from '../AddressForm/useAddressForm';
import { useAwaitQuery } from '../../utils/hooks';
import { useCartState } from '../Minicart/cartContext';
import { useCheckoutState } from './checkoutContext';
import { useUserContext } from '../../context/UserContext';

import { getCartDetails } from '../../actions/cart';

import MUTATION_SET_SHIPPING_ADDRESS from '../../queries/mutation_set_shipping_address.graphql';
import CART_DETAILS_QUERY from '../../queries/query_cart_details.graphql';

export default () => {
    const { parseAddress, isSameAddress } = useAddressForm();
    const [{ cartId, cart }, cartDispatch] = useCartState();
    const [{ shippingAddress }, dispatch] = useCheckoutState();
    const [{ currentUser, isSignedIn }] = useUserContext();
    const [setShippingAddressesOnCart] = useMutation(MUTATION_SET_SHIPPING_ADDRESS);
    const cartDetailsQuery = useAwaitQuery(CART_DETAILS_QUERY);

    const beginCheckout = async () => {
        cartDispatch({ type: 'beginLoading' });

        if (
            cart &&
            ((cart.shipping_addresses && cart.shipping_addresses[0]) || (cart.is_virtual && cart.billing_address))
        ) {
            await getCartDetails({ cartDetailsQuery, cartId, dispatch: cartDispatch });

            const cartShippingAddress = cart.shipping_addresses && cart.shipping_addresses[0];
            const cartBillingAddress = cart.billing_address;
            if (cartShippingAddress && cartBillingAddress && !isSameAddress(cartShippingAddress, cartBillingAddress)) {
                dispatch({ type: 'setBillingAddressSameAsShippingAddress', same: false });
            }
        } else if (isSignedIn && currentUser.addresses.length > 0 && cart && !cart.is_virtual && !shippingAddress) {
            const address = currentUser.addresses.find(address => address.default_shipping) || currentUser.addresses[0];
            const addressVariables = { variables: { cartId, country_code: 'US', ...parseAddress(address) } };
            await setShippingAddressesOnCart(addressVariables);
            await getCartDetails({ cartDetailsQuery, dispatch: cartDispatch, cartId });
        }

        cartDispatch({ type: 'endLoading' });
        dispatch({ type: 'beginCheckout' });
    };

    return { beginCheckout };
};
