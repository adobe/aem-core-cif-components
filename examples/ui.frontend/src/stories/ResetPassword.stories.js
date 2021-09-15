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
import { ConfigContextProvider, ResetPassword } from '@adobe/aem-core-cif-react-components';
import { withQuery } from '@storybook/addon-queryparams';
import { MockedProvider } from '@apollo/client/testing';

import { generateGithubLink } from './utils';
import MUTATION_RESET_PASSWORD from 'Queries/mutation_reset_password.graphql';

export default {
    title: 'Commerce/ResetPassword',
    component: ResetPassword,
    decorator: [withQuery],
    parameters: {
        docs: {
            description: {
                component: `The component is a client-side React component which displays a reset password form for customer accounts.<br /><br />
                    To display the form, a token is required to be passed as in the token query parameter with the URL. The URL can be requested using the Forgot Password component and is sent via e-mail by Magento.<br /><br />
                    After submitting the form, the component uses a Magento GraphQL mutation to change the password to the new one provided in the form.<br /><br />
                    <b>Example:</b> Try changing the password to <code>NewPassword123</code> using the e-mail <code>chuck@example.com</code>.<br /><br />
                    ${generateGithubLink(
                        'https://github.com/adobe/aem-core-cif-components/tree/master/react-components/src/components/ResetPassword'
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
                <MockedProvider mocks={args.mocks}>
                    <ResetPassword />
                </MockedProvider>
            </ConfigContextProvider>
        </IntlProvider>
    );
};

export const WithToken = Template.bind({});
WithToken.parameters = {
    query: {
        token: 'my-token'
    }
};
WithToken.args = {
    mocks: [
        {
            request: {
                query: MUTATION_RESET_PASSWORD,
                variables: {
                    email: 'chuck@example.com',
                    resetPasswordToken: 'my-token',
                    newPassword: 'NewPassword123'
                }
            },
            result: {
                data: { resetPassword: true }
            }
        }
    ]
};

export const MissingToken = Template.bind({});
MissingToken.parameters = {
    query: {
        token: undefined
    },
    docs: {
        description: {
            story: 'If the `token` query parameter is not passed via the URL, the component displays an error message.'
        }
    }
};
