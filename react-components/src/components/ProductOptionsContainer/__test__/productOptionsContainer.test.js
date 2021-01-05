/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import { render } from 'test-utils';
import { fireEvent, waitForDomChange } from '@testing-library/react';
import ProductOptionsContainer from '../productOptionsContainer';
import bundleData from './bundle-data.json';
import customizableOptions from './customizable-options.json';
import bundleCustomizableOptions from './bundle-with-customizable-options.json';

const config = {
    graphqlMethod: 'GET',
    storeView: 'default',
    graphqlEndpoint: 'endpoint',
    mountingPoints: {
        productOptionsContainer: '#product-options'
    }
};

describe('<ProductOptionsContainer>', () => {
    it('renders the component with no data', () => {
        const productOptionsContainer = document.createElement('div');
        productOptionsContainer.id = 'product-options';

        const { asFragment } = render(<ProductOptionsContainer />, {
            config: config,
            container: document.body.appendChild(productOptionsContainer)
        });
        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with bundle data', async () => {
        const productOptionsContainer = document.createElement('div');
        productOptionsContainer.id = 'product-options';
        productOptionsContainer.dataset.raw = JSON.stringify(bundleData);
        productOptionsContainer.dataset.sku = '24-WG080';
        productOptionsContainer.dataset.currency = 'USD';

        const { asFragment, container, getByRole } = render(<ProductOptionsContainer />, {
            config: config,
            container: document.body.appendChild(productOptionsContainer)
        });
        expect(asFragment()).toMatchSnapshot();

        const domChange = waitForDomChange({ container });
        fireEvent.click(getByRole('button', { name: 'Customize' }));

        await domChange;

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with customizable options', async () => {
        const productOptionsContainer = document.createElement('div');
        productOptionsContainer.id = 'product-options';
        productOptionsContainer.dataset.raw = JSON.stringify(customizableOptions);
        productOptionsContainer.dataset.sku = '24-UG06';
        productOptionsContainer.dataset.currency = 'USD';

        const { asFragment } = render(<ProductOptionsContainer />, {
            config: config,
            container: document.body.appendChild(productOptionsContainer)
        });

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the component with customizable bundle data', async () => {
        const productOptionsContainer = document.createElement('div');
        productOptionsContainer.id = 'product-options';
        productOptionsContainer.dataset.raw = JSON.stringify(bundleCustomizableOptions);
        productOptionsContainer.dataset.sku = 'test-bundle';
        productOptionsContainer.dataset.currency = 'USD';

        const { asFragment, container, getByRole } = render(<ProductOptionsContainer />, {
            config: config,
            container: document.body.appendChild(productOptionsContainer)
        });
        expect(asFragment()).toMatchSnapshot();

        const domChange = waitForDomChange({ container });
        fireEvent.click(getByRole('button', { name: 'Customize' }));

        await domChange;

        expect(asFragment()).toMatchSnapshot();
    });
});
