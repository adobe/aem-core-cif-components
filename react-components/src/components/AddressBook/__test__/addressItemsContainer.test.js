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
import { render } from '../../../utils/test-utils';
import AddressItemsContainer from '../addressItemsContainer';

describe('<AddressItemsContainer>', () => {
    it('renders the component', () => {
        const mockInitialState = {
            currentUser: {
                addresses: [
                    {
                        id: 'my-address-id',
                        region: {
                            region_code: 'LA'
                        },
                        street: ['14 Stamford Court'],
                        default_shipping: false,
                        default_billing: false
                    }
                ]
            }
        };

        const { asFragment } = render(<AddressItemsContainer />, { userContext: mockInitialState });
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with list display type', () => {
        const { asFragment } = render(<AddressItemsContainer displayType={'list'} />);
        expect(asFragment()).toMatchSnapshot();
    });
});
