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
import React from 'react';
import { render, wait } from '@testing-library/react';

jest.mock('@magento/peregrine/lib/talons/Wishlist/AddToListButton/addToListButton.gql', () => ({
    addProductToWishlistMutation: 'default'
}));
jest.mock('../../../queries/query_countries.graphql', () => 'default');
jest.mock('@apollo/client', () => ({
    useMutation: jest.fn().mockImplementation(fn => [fn]),
    useQuery: jest.fn()
}));

import { useMutation } from '@apollo/client';
import useAddToWishlistEvent from '../useAddToWishlistEvent';

describe('useAddToCartEvent', () => {
    const testEventDetails = async (mockFn, result) => {
        await wait(() => {
            expect(mockFn).toHaveBeenCalledTimes(1);
            expect(mockFn).toHaveBeenCalledWith({
                variables: { wishlistId: '0', itemOptions: result }
            });
        });
    };

    const dispatchEvent = items =>
        document.dispatchEvent(
            new CustomEvent('aem.cif.add-to-wishlist', {
                detail: items
            })
        );

    const MockComponent = props => {
        useAddToWishlistEvent(props);
        return <></>;
    };

    it('uses default operation', async () => {
        render(<MockComponent />);
        expect(useMutation).toHaveBeenCalledWith('default');
    });

    it('uses custom operation', async () => {
        render(<MockComponent operations={{ addProductToWishlistMutation: 'custom' }} />);
        expect(useMutation).toHaveBeenCalledWith('custom');
    });

    it('handles event with string details', async () => {
        const addProductToWishlistMutationMock = jest.fn();
        render(<MockComponent operations={{ addProductToWishlistMutation: addProductToWishlistMutationMock }} />);

        dispatchEvent('[{"sku": "bar", "quantity": 1, "parent_sku": "test", "extra": "ignore"}]');

        await testEventDetails(addProductToWishlistMutationMock, {
            sku: 'bar',
            quantity: 1,
            parent_sku: 'test'
        });
    });

    it('handles event with JSON details', async () => {
        const addProductToWishlistMutationMock = jest.fn();
        render(<MockComponent operations={{ addProductToWishlistMutation: addProductToWishlistMutationMock }} />);

        dispatchEvent([{ sku: 'bar', quantity: 1, extra: 'ignore' }]);

        await testEventDetails(addProductToWishlistMutationMock, {
            sku: 'bar',
            quantity: 1
        });
    });
});
