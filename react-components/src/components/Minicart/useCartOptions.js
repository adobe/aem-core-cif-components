/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import { useCartState } from './cartContext';
import { updateCartItem } from '../../actions/cart';
import { useStorefrontEvents } from '../../utils/hooks';

const useCartOptions = ({ updateCartItemMutation, cartDetailsQuery }) => {
    const [{ editItem, cartId }, dispatch] = useCartState();
    const mse = useStorefrontEvents();

    const updateCart = async newQuantity => {
        dispatch({ type: 'beginLoading' });
        await updateCartItem({
            cartDetailsQuery,
            updateCartItemMutation,
            cartId,
            cartItemUid: editItem.uid,
            itemQuantity: newQuantity,
            dispatch
        });

        mse && mse.publish.updateCart();

        dispatch({ type: 'endLoading' });
    };

    const data = { editItem, cartId };
    const api = { dispatch, updateCartItem: updateCart };
    return [data, api];
};

export default useCartOptions;
