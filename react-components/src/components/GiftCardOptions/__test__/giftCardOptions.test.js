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
import React from 'react';
import { fireEvent, wait, screen } from '@testing-library/react';
import { render } from 'test-utils';
import GiftCardProductOptions from '../giftCardOptions';
import mockResponse from './graphqlMockGiftCardQuery';
import mockAddToCartMutation from './graphqlMockAddToCartMutation';

const config = {
    storeView: 'default',
    graphqlEndpoint: 'endpoint',
    graphqlMethod: 'GET',
    mountingPoints: {
        giftCardProductOptionsContainer: '#gift-card-product-options'
    }
};

describe('GiftCardProductOptions', () => {
    const eventHandler = jest.fn().mockImplementation(event => {
        return event.detail;
    });
    document.addEventListener('aem.cif.add-to-cart', eventHandler);
    document.addEventListener('aem.cif.add-to-wishlist', eventHandler);

    afterEach(() => {
        eventHandler.mockClear();
    });

    it('renders the component with no sku', () => {
        const { asFragment } = render(<GiftCardProductOptions />, { config: config });
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with sku', async () => {
        const gitfCardProductOptionsContainer = document.createElement('div');

        const { asFragment } = render(<GiftCardProductOptions sku="gift-card" />, {
            config: config,
            container: document.body.appendChild(gitfCardProductOptionsContainer),
            mocks: [mockResponse]
        });

        expect(await screen.findByText(/cart/i)).toBeInTheDocument();

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with wishlist', async () => {
        const gitfCardProductOptionsContainer = document.createElement('div');

        const { asFragment } = render(<GiftCardProductOptions sku="gift-card" showAddToWishList={true} />, {
            config: config,
            container: document.body.appendChild(gitfCardProductOptionsContainer),
            mocks: [mockResponse]
        });

        expect(await screen.findByText(/cart/i)).toBeInTheDocument();

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with full options', async () => {
        const gitfCardProductOptionsContainer = document.createElement('div');

        const { asFragment, getByRole, getByLabelText } = render(
            <GiftCardProductOptions sku="gift-card" showQuantity={true} />,
            {
                config: config,
                container: document.body.appendChild(gitfCardProductOptionsContainer),
                mocks: [mockResponse, mockAddToCartMutation]
            }
        );

        expect(await screen.findByText(/cart/i)).toBeInTheDocument();

        expect(asFragment()).toMatchSnapshot();

        // Click add to cart which should be disabled
        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));
        expect(eventHandler).toHaveBeenCalledTimes(0);

        // Add the required inputs
        fireEvent.change(getByLabelText(/amount/i), {
            target: { value: 'Z2lmdGNhcmQvZ2lmdGNhcmRfYW1vdW50LzEyLjAwMDA=' }
        });
        fireEvent.change(getByLabelText(/sender name/i), { target: { value: 'sender-name' } });
        fireEvent.change(getByLabelText(/sender email/i), { target: { value: 'sender-email' } });
        fireEvent.change(getByLabelText(/recipient name/i), { target: { value: 'recipient-name' } });
        fireEvent.change(getByLabelText(/recipient email/i), { target: { value: 'recipient-email' } });

        fireEvent.click(getByRole('button', { name: 'Add to Cart' }));

        // Add to cart should be called just once since the first click was on a disabled button
        await wait(() => expect(eventHandler).toHaveBeenCalledTimes(1));

        // The mock dispatchEvent function returns the CustomEvent detail
        expect(eventHandler).toHaveReturnedWith([
            {
                sku: 'gift-card',
                parentSku: 'gift-card',
                virtual: false,
                giftCard: true,
                quantity: 1,
                entered_options: [
                    {
                        uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX25hbWU=',
                        value: 'sender-name'
                    },
                    {
                        uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfc2VuZGVyX2VtYWls',
                        value: 'sender-email'
                    },
                    {
                        uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X25hbWU=',
                        value: 'recipient-name'
                    },
                    {
                        uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfcmVjaXBpZW50X2VtYWls',
                        value: 'recipient-email'
                    },
                    {
                        uid: 'Z2lmdGNhcmQvZ2lmdGNhcmRfbWVzc2FnZQ==',
                        value: ''
                    }
                ],
                selected_options: ['Z2lmdGNhcmQvZ2lmdGNhcmRfYW1vdW50LzEyLjAwMDA=']
            }
        ]);
    });

    it('renders add to wish list button', async () => {
        const gitfCardProductOptionsContainer = document.createElement('div');

        const { asFragment, getByRole } = render(<GiftCardProductOptions sku="gift-card" showAddToWishList={true} />, {
            config: config,
            container: document.body.appendChild(gitfCardProductOptionsContainer),
            mocks: [mockResponse, mockAddToCartMutation]
        });

        expect(await screen.findByText(/cart/i)).toBeInTheDocument();

        expect(asFragment()).toMatchSnapshot();

        // Click add to wish list button
        fireEvent.click(getByRole('button', { name: 'Add to Wish List' }));

        // Add to cart should be called just once since the first click was on a disabled button
        await wait(() => expect(eventHandler).toHaveBeenCalledTimes(1));

        expect(eventHandler).toHaveReturnedWith([
            {
                sku: 'gift-card',
                quantity: 1
            }
        ]);
    });
});
