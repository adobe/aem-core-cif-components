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

import React, { Suspense } from 'react';
import PropTypes from 'prop-types';

import { I18nextProvider } from 'react-i18next';
import { CommerceApp, ConfigContextProvider, ResetPassword } from '@adobe/aem-core-cif-react-components';
import { withQuery } from '@storybook/addon-queryparams';

import i18n from './i18n';

import './styles/main.scss';

export default {
    title: 'Commerce/ResetPassword',
    component: ResetPassword,
    decorator: [withQuery],
    parameters: {
        docs: {
            description: {
                component:
                    'The `ResetPassword` component displays a form to reset a password after clicking on a generated reset password link received via e-mail.'
            }
        }
    }
};

const Template = (args, context) => {
    return (
        <I18nextProvider i18n={i18n} defaultNS="common">
            <ConfigContextProvider
                config={{
                    storeView: context.parameters.cifConfig.storeView,
                    graphqlEndpoint: context.parameters.cifConfig.graphqlEndpoint
                }}>
                <CommerceApp>
                    <ResetPassword />
                </CommerceApp>
            </ConfigContextProvider>
        </I18nextProvider>
    );
};

export const WithToken = Template.bind({});
WithToken.parameters = {
    query: {
        token: 'my-token'
    }
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
