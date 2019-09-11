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

import CartOptions from '../cartOptions';

describe('<CartOptions>', () => {
    const handleEndEditing = jest.fn();

    const mockProps = {
        handleEndEditing,
        cartId: '123ABC',
        editItem: {
            id: '123',
            quantity: 2,
            product: {
                name: 'Dummy product',
                price: {
                    regularPrice: {
                        amount: {
                            value: 100,
                            currency: 'USD'
                        }
                    }
                }
            }
        }
    };

    it('renders the component properly', () => {
        const { asFragment } = render(<CartOptions {...mockProps} />);
        expect(asFragment()).toMatchSnapshot();
    });
});
