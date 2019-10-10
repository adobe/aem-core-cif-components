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
import React from 'react';
import ReactDOM from 'react-dom';
import { render, fireEvent } from '@testing-library/react';
import CartTrigger from '../cartTrigger';
import { CartProvider } from '../../../utils/state';

describe('<CartTrigger>', () => {
    beforeAll(() => {
        ReactDOM.createPortal = jest.fn(element => {
            return element;
        });
    });

    afterEach(() => {
        ReactDOM.createPortal.mockClear();
    });

    it('renders the icon', () => {
        const { asFragment } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CartTrigger cartQuantity={2} />
            </CartProvider>
        );
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the quantity', () => {
        const expectedQuantity = '2';
        const { getByTestId } = render(
            <CartProvider initialState={{}} reducerFactory={() => state => state}>
                <CartTrigger cartQuantity={parseInt(expectedQuantity)} />
            </CartProvider>
        );

        expect(getByTestId('cart-counter').textContent).toEqual(expectedQuantity);
    });

    it('calls the handler function when clicked', () => {
        const handler = jest.fn();

        const { getByRole } = render(
            <CartProvider initialState={{}} reducerFactory={() => handler}>
                <CartTrigger cartQuantity={2} />
            </CartProvider>
        );
        fireEvent.click(getByRole('button'));

        expect(handler.mock.calls.length).toEqual(1);
    });
});
