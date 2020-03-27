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

import MUTATION_CREATE_CART from '../../queries/mutation_create_guest_cart.graphql';

import { useCartState } from './cartContext';
import { useUserContext } from '../../context/UserContext';

export default () => {
    const [{ cartId, addItem }, dispatch] = useCartState();
    const [{ isSignedIn }] = useUserContext();
    const [createCart, { data, error }] = useMutation(MUTATION_CREATE_CART);

    const addItemMaybeCreateCart = async event => {
        // nothing to do if we have a cart id
        if (cartId) {
            addItem(event);
        }

        // if we're signed in there's nothing to do - we'd be having a cart already
        if (!isSignedIn) {
            dispatch({ type: 'createEmptyCart' });
        }
    };

    return [addItemMaybeCreateCart];
};
