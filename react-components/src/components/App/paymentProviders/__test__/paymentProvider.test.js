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
import { render } from '@testing-library/react';

jest.mock('informed', () => ({
    useFormState: jest.fn(),
    Text: function Text() {
        return <input type="text" />;
    }
}));
jest.mock('../braintree', () => {
    return function Braintree(props) {
        return <div data-testid="braintree-cmp">{props.accept}</div>; // eslint-disable-line react/prop-types
    };
});

import { useFormState } from 'informed';
import PaymentProvider from '../paymentProvider';

describe('<PaymentProvider />', () => {
    it('renders the Braintree credit card component', () => {
        useFormState.mockImplementation(() => ({
            values: {
                payment_method: 'braintree'
            },
            errors: {}
        }));

        let { getByTestId } = render(<PaymentProvider />);

        expect(getByTestId('braintree-cmp').textContent).toBe('card');
    });

    it('renders the Braintree paypal component', () => {
        useFormState.mockImplementation(() => ({
            values: {
                payment_method: 'braintree_paypal'
            },
            errors: {}
        }));

        let { getByTestId } = render(<PaymentProvider />);

        expect(getByTestId('braintree-cmp').textContent).toBe('paypal');
    });

    it('renders no component when no payment method is selected', () => {
        useFormState.mockImplementation(() => ({
            values: {
                payment_method: 'check'
            },
            errors: {}
        }));

        let { queryByTestId } = render(<PaymentProvider />);

        expect(queryByTestId('braintree-cmp')).toBe(null);
    });

    it('renders the form error message', () => {
        useFormState.mockImplementation(() => ({
            values: {
                payment_method: 'braintree'
            },
            errors: {
                payment_nonce: 'Invalid payment nonce'
            }
        }));

        let { baseElement } = render(<PaymentProvider />);

        expect(baseElement.querySelector('.error_message').textContent).toBe('Invalid payment nonce');
    });
});
