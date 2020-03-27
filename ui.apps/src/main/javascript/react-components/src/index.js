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
import { CommerceApp, Cart, AuthBar } from '@adobe/aem-core-cif-react-components';
import { I18nextProvider } from 'react-i18next';

import i18n from './i18n';

const App = () => {
    const { storeView, graphqlEndpoint } = document.querySelector('body').dataset;
    return (
        <I18nextProvider i18n={i18n} defaultNS="common">
            <CommerceApp uri={graphqlEndpoint} storeView={storeView}>
                <Cart />
                <AuthBar />
            </CommerceApp>
        </I18nextProvider>
    );
};

window.onload = function() {
    const element = document.getElementById('minicart');
    ReactDOM.render(<App />, element);
};
