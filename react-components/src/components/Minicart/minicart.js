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
import React, { Suspense } from 'react';
import { useMutation } from '@apollo/client';

import MUTATION_CREATE_CART from '../../queries/mutation_create_guest_cart.graphql';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import QUERY_CART_DETAILS from '../../queries/query_cart_details.graphql';
import MUTATION_ADD_VIRTUAL_TO_CART from '../../queries/mutation_add_virtual_to_cart.graphql';
import MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART from '../../queries/mutation_add_simple_and_virtual_to_cart.graphql';
import MUTATION_ADD_BUNDLE_TO_CART from '../../queries/mutation_add_bundle_to_cart.graphql';

import Mask from '../Mask';
import Header from './header';
import Body from './body';
import Footer from './footer';
import useMinicart from './useMinicart';
import LoadingIndicator from '../LoadingIndicator';
import { useCheckoutState } from '../Checkout/checkoutContext';
import { useEventListener, useAwaitQuery } from '../../utils/hooks';

import classes from './minicart.css';

const MiniCart = () => {
    const [createCartMutation] = useMutation(MUTATION_CREATE_CART);
    const [addToCartMutation] = useMutation(MUTATION_ADD_TO_CART);
    const [addVirtualItemMutation] = useMutation(MUTATION_ADD_VIRTUAL_TO_CART);
    const [addSimpleAndVirtualItemMutation] = useMutation(MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART);
    const [addBundleItemMutation] = useMutation(MUTATION_ADD_BUNDLE_TO_CART);
    const cartDetailsQuery = useAwaitQuery(QUERY_CART_DETAILS);
    const [{ flowState }] = useCheckoutState();

    const [{ cart, isOpen, isLoading, isEditing, errorMessage }, { addItem, dispatch }] = useMinicart({
        queries: {
            createCartMutation,
            addToCartMutation,
            cartDetailsQuery,
            addVirtualItemMutation,
            addBundleItemMutation,
            addSimpleAndVirtualItemMutation
        }
    });

    useEventListener(document, 'aem.cif.open-cart', () => {
        dispatch({ type: 'open' });
    });
    useEventListener(document, 'aem.cif.add-to-cart', addItem);

    const rootClass = isOpen ? classes.root_open : classes.root;
    const isEmpty = cart && Object.entries(cart).length > 0 ? cart.items.length === 0 : true;
    const showFooter = !(isLoading || isEmpty || isEditing || errorMessage) || flowState === 'receipt';
    const footer = showFooter ? <Footer /> : null;

    return (
        <>
            <Mask isOpen={isOpen} onClickHandler={() => dispatch({ type: 'close' })} />
            <aside className={rootClass}>
                <Suspense fallback={<LoadingIndicator />}>
                    <Header />
                    <Body />
                    {footer}
                </Suspense>
            </aside>
        </>
    );
};

export default MiniCart;
