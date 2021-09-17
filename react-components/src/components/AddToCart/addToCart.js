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

import React, { useCallback, useRef, useState } from 'react';
import { useIntl } from 'react-intl';
import { useMutation } from '@apollo/client';
import { useCartContext } from '@magento/peregrine/lib/context/cart';
import { productMapper, bundledProductMapper } from '../Minicart/useMinicart';
import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';

const PRODUCT_DATA_UPDATE = 'aem.cif.internal.add-to-cart.state-changed';

const AddToCart = props => {
    const intl = useIntl();
    const buttonRef = useRef(null);
    const setButtonRef = useCallback(node => {
        // remove listener from previous ref, if any
        if (buttonRef.current) {
            buttonRef.current.removeEventListener(PRODUCT_DATA_UPDATE, handleProductDataUpdate);
        }
        // add listner to new ref, if any
        if (node) {
            node.addEventListener(PRODUCT_DATA_UPDATE, handleProductDataUpdate);
        }
        // setRef
        buttonRef.current = node;
    }, []);
    const [items, setItems] = useState(props.items || []);
    const [{ cartId }] = useCartContext();
    const [addToCartMutation] = useMutation(MUTATION_ADD_TO_CART);

    const handleProductDataUpdate = event => {
        console.log('received event: ');
        console.log(event);
        setItems(event.detail);
        return;
    }

    const handleAddToCart = async () => {
        const physicalCartItems = items.filter(item => !item.virtual).map(productMapper);
        // TODO: handle other kinds of mutations
        // const virtualCartItems = items.filter(item => item.virtual).map(productMapper);
        // const bundleCartItems = items.filter(item => item.bundle).map(bundledProductMapper);
        await addToCartMutation({ variables: { cartId, cartItems: physicalCartItems } });
    }

    return (
        <button 
            ref={setButtonRef}
            className="button__root_highPriority button__root clickable__root button__filled" 
            type="button"
            onClick={handleAddToCart}
            disabled={items.length === 0}
        >
            <span className="button__content">
                <span>{intl.formatMessage({ id: 'add-to-cart:label', defaultMessage: 'Add to Cart' })}</span>
            </span>
        </button>
    )
}

export default AddToCart;
