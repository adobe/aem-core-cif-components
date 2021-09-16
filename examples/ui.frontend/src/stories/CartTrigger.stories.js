/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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

import { IntlProvider } from 'react-intl';
import { MockedProvider } from '@apollo/client/testing';
import { CartTrigger, ConfigContextProvider, CartProvider } from '@adobe/aem-core-cif-react-components';

import { generateGithubLink } from './utils';

export default {
    title: 'Commerce/CartTrigger',
    component: CartTrigger,
    parameters: {
        docs: {
            description: {
                component: `The component is a client-side React component which displays a button to open and close the cart component and to indicate the cart state by displaying the number of items in the cart.<br /><br />
                The cart data is read from the <code>CartProvider</code> context, which will also receive a method call when the button is clicked.<br /><br />
                ${generateGithubLink(
                    'https://github.com/adobe/aem-core-cif-components/tree/master/react-components/src/components/CartTrigger'
                )}`
            }
        }
    }
};

const Template = (args, context) => {
    return (
        <IntlProvider locale="en">
            <ConfigContextProvider
                config={{
                    storeView: context.parameters.cifConfig.storeView,
                    graphqlEndpoint: context.parameters.cifConfig.graphqlEndpoint,
                    graphqlMethod: context.parameters.cifConfig.graphqlMethod
                }}>
                <MockedProvider>
                    <CartProvider initialState={{ cart: { total_quantity: args.quantity } }}>
                        <CartTrigger />
                    </CartProvider>
                </MockedProvider>
            </ConfigContextProvider>
        </IntlProvider>
    );
};

export const WithItems = Template.bind({});
WithItems.args = { quantity: 4 };

export const Empty = Template.bind({});
Empty.args = { quantity: 0 };
