/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useIntl } from 'react-intl';
import { useMutation } from '@apollo/client';
import { useCartContext } from '@magento/peregrine/lib/context/cart';
import { productMapper, bundledProductMapper } from '../Minicart/useMinicart';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import MUTATION_ADD_BUNDLE_TO_CART from '../../queries/mutation_add_bundle_to_cart.graphql';
import MUTATION_ADD_VIRTUAL_TO_CART from '../../queries/mutation_add_virtual_to_cart.graphql';
import MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART from '../../queries/mutation_add_simple_and_virtual_to_cart.graphql';

const PRODUCT_DATA_UPDATE = 'aem.cif.internal.add-to-cart.state-changed';

const useEventListener = props => {
    const { eventName, eventListener } = props;
    const ref = useRef(null);
    const callback = useCallback(node => {
        // remove listener from previous ref, if any
        if (ref.current) {
            ref.current.removeEventListener(eventName, eventListener);
        }
        // add listner to new ref, if any
        if (node) {
            node.addEventListener(eventName, eventListener);
        }
        // setRef
        ref.current = node;
    });
    return [callback];
};

const AddToCart = props => {
    const intl = useIntl();
    const [items, setItems] = useState([]);
    const [disabled, setDisabled] = useState(true);
    const handleProductDataUpdate = event => setItems(event.detail);
    const [buttonRef] = useEventListener({ eventName: PRODUCT_DATA_UPDATE, eventListener: handleProductDataUpdate });
    const [{ cartId }] = useCartContext();
    const [addToCartMutation] = useMutation(MUTATION_ADD_TO_CART);
    const [addBundleItemMutation] = useMutation(MUTATION_ADD_BUNDLE_TO_CART);
    const [addVirtualItemMutation] = useMutation(MUTATION_ADD_VIRTUAL_TO_CART);
    const [addSimpleAndVirtualItemMutation] = useMutation(MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART);

    const buttonContent = props.children ? (
        props.children
    ) : (
        <span>{intl.formatMessage({ id: 'add-to-cart:label', defaultMessage: 'Add to Cart' })}</span>
    );

    const handleAddToCart = useCallback(async () => {
        const physicalCartItems = items.filter(item => !item.virtual).map(productMapper);
        const virtualCartItems = items.filter(item => item.virtual).map(productMapper);
        const bundleCartItems = items.filter(item => item.bundle).map(bundledProductMapper);

        if (bundleCartItems.length > 0) {
            await addBundleItemMutation({
                variables: {
                    cartId,
                    cartItems: bundleCartItems
                }
            });
        } else if (virtualCartItems.length > 0 && physicalCartItems.length > 0) {
            await addSimpleAndVirtualItemMutation({
                variables: {
                    cartId,
                    virtualCartItems: virtualCartItems,
                    simpleCartItems: physicalCartItems
                }
            });
        } else if (virtualCartItems.length > 0) {
            await addVirtualItemMutation({
                variables: {
                    cartId,
                    cartItems: virtualCartItems
                }
            });
        } else if (physicalCartItems.length > 0) {
            await addToCartMutation({
                variables: {
                    cartId,
                    cartItems: physicalCartItems
                }
            });
        }

        if (props.onAddToCart) {
            props.onAddToCart(items);
        } else if (items.length > 0) {
            // dispatch the event that other wise the product detail add to cart function would have
            // fired
            document.dispatchEvent(
                new CustomEvent('aem.cif.add-to-cart', {
                    detail: items
                })
            );
        }
    }, [props.onAddToCart, items, cartId]);

    useEffect(() => {
        const newItems = typeof props.items === 'string' ? JSON.parse(props.items) : props.items;
        setItems(newItems);
    }, [props.items]);

    useEffect(() => {
        setDisabled(props.disabled || items.length === 0);
    }, [props.disabled, items]);

    return (
        <button
            ref={buttonRef}
            className="button__root_highPriority button__root clickable__root button__filled"
            type="button"
            onClick={handleAddToCart}
            disabled={disabled}>
            <span className="button__content">{buttonContent}</span>
        </button>
    );
};

export default AddToCart;
