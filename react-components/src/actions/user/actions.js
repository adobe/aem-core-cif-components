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

/**
 * Re-fetches a customer cart.
 *
 * @param {Object} payload a parameters object with the following structure:
 *     fetchCustomerCartQuery - the query object to execute to retrieve the cart details
 *     dispatch - the dispatch callback for the user context
 */
export const resetCustomerCart = async ({ dispatch, fetchCustomerCartQuery }) => {
    const { data } = await fetchCustomerCartQuery({
        fetchPolicy: 'network-only'
    });
    const cartId = data.customerCart.id;
    dispatch({ type: 'setCartId', cartId });
};

export const signOutUser = async ({ revokeCustomerToken, setCartCookie, setUserCookie, dispatch }) => {
    try {
        await revokeCustomerToken();
        setCartCookie('', 0);
        setUserCookie('', 0);
        dispatch({ type: 'signOut' });
    } catch (error) {
        console.error('An error occurred during sign-out', error);
        dispatch({ type: 'error', error: error.toString() });
    }
};

export const createAddress = async ({ createCustomerAddress, variables, resetFields, dispatch }) => {
    try {
        const { data } = await createCustomerAddress({ variables: variables });
        dispatch({ type: 'postCreateAddress', address: data.createCustomerAddress, resetFields });
    } catch (error) {
        console.error('An error occurred during creating customer address', error);
        dispatch({ type: 'setAddressFormError', error: error.toString() });
    }
};

export const updateAddress = async ({ updateCustomerAddress, variables, resetFields, dispatch }) => {
    try {
        const { data } = await updateCustomerAddress({ variables: variables });
        dispatch({ type: 'postUpdateAddress', address: data.updateCustomerAddress, resetFields });
    } catch (error) {
        console.error('An error occurred during updating customer address', error);
        dispatch({ type: 'setAddressFormError', error: error.toString() });
    }
};

export const deleteAddress = async ({ deleteCustomerAddress, address, dispatch }) => {
    try {
        await deleteCustomerAddress({ variables: { id: address.id } });
        dispatch({ type: 'postDeleteAddress', address });
    } catch (error) {
        console.error('An error occurred during deleting customer address', error);
        dispatch({ type: 'deleteAddressError', error: error.toString() });
    }
};
