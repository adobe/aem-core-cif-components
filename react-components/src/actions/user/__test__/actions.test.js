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
import { resetCustomerCart, signOutUser, createAddress, updateAddress, deleteAddress } from '../actions';

const setCartCookie = jest.fn();
const setUserCookie = jest.fn();
const dispatch = jest.fn();

// avoid console errors logged during testing
console.error = jest.fn();

describe('User actions', () => {
    beforeEach(() => {
        setCartCookie.mockReset();
        setUserCookie.mockReset();
        dispatch.mockReset();
    });

    it('resets the customer cart', async () => {
        const dispatch = jest.fn();
        const query = jest.fn(() => {
            return { data: { customerCart: { id: 'my-cart-id' } } };
        });

        await resetCustomerCart({ dispatch, fetchCustomerCartQuery: query });

        expect(query).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'setCartId', cartId: 'my-cart-id' });
    });

    it('signs out the user', async () => {
        const revokeCustomerToken = jest.fn(() => {
            return { data: { revokeCustomerToken: { result: true } } };
        });

        await signOutUser({ revokeCustomerToken, setCartCookie, setUserCookie, dispatch });

        expect(revokeCustomerToken).toHaveBeenCalledTimes(1);

        expect(setCartCookie).toHaveBeenCalledTimes(1);
        expect(setCartCookie).toHaveBeenCalledWith('', 0);

        expect(setUserCookie).toHaveBeenCalledTimes(1);
        expect(setUserCookie).toHaveBeenCalledWith('', 0);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'signOut' });
    });

    it('fails to sign out the user', async () => {
        const revokeCustomerToken = jest.fn().mockRejectedValueOnce(new Error('Failed to sign out'));

        await signOutUser({ revokeCustomerToken, setCartCookie, setUserCookie, dispatch });

        expect(revokeCustomerToken).toHaveBeenCalledTimes(1);
        expect(setCartCookie).toHaveBeenCalledTimes(0);
        expect(setUserCookie).toHaveBeenCalledTimes(0);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'error', error: 'Error: Failed to sign out' });
    });

    it('create the customer address', async () => {
        const variables = {
            region: {
                region_code: 'LA'
            },
            street: ['14 Stamford Court'],
            default_shipping: false,
            default_billing: false
        };

        const createCustomerAddress = jest.fn(() => {
            return { data: { createCustomerAddress: variables } };
        });

        await createAddress({ createCustomerAddress, variables, dispatch });

        expect(createCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'postCreateAddress', address: variables });
    });

    it('fails to create the customer address', async () => {
        const variables = {
            region: {
                region_code: 'LA'
            },
            street: ['14 Stamford Court'],
            default_shipping: false,
            default_billing: false
        };

        const createCustomerAddress = jest.fn().mockRejectedValueOnce(new Error('Failed to create the address'));

        await createAddress({ createCustomerAddress, variables, dispatch });

        expect(createCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        dispatch({ type: 'setAddressFormError', error: 'Error: Failed to create the address' });
    });

    it('update the customer address', async () => {
        const variables = {
            id: 'my-address-id',
            region: {
                region_code: 'LA'
            },
            street: ['14 Stamford Court'],
            default_shipping: false,
            default_billing: false
        };

        const updateCustomerAddress = jest.fn(() => {
            return { data: { updateCustomerAddress: variables } };
        });

        await updateAddress({ updateCustomerAddress, variables, dispatch });

        expect(updateCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'postUpdateAddress', address: variables });
    });

    it('fails to update the customer address', async () => {
        const variables = {
            id: 'my-address-id',
            region: {
                region_code: 'LA'
            },
            street: ['14 Stamford Court'],
            default_shipping: false,
            default_billing: false
        };

        const updateCustomerAddress = jest.fn().mockRejectedValueOnce(new Error('Failed to update the address'));

        await updateAddress({ updateCustomerAddress, variables, dispatch });

        expect(updateCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        dispatch({ type: 'setAddressFormError', error: 'Error: Failed to update the address' });
    });

    it('delete the customer address', async () => {
        const address = {
            id: 'my-address-id'
        };

        const deleteCustomerAddress = jest.fn();

        await deleteAddress({ deleteCustomerAddress, address, dispatch });

        expect(deleteCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({ type: 'postDeleteAddress', address });
    });

    it('fails to delete the customer address', async () => {
        const address = {
            id: 'my-address-id'
        };

        const deleteCustomerAddress = jest.fn().mockRejectedValueOnce(new Error('Failed to delete the address'));

        await deleteAddress({ deleteCustomerAddress, address, dispatch });

        expect(deleteCustomerAddress).toHaveBeenCalledTimes(1);

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch).toHaveBeenCalledWith({
            type: 'deleteAddressError',
            error: 'Error: Failed to delete the address'
        });
    });
});
