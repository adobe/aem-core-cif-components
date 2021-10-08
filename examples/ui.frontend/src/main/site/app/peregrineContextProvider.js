/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
import { Provider as ReduxProvider } from 'react-redux';
import {
    PeregrineContextProvider as Peregrine,
    ToastContextProvider,
    WindowSizeContextProvider
} from '@magento/peregrine';

import store from './store';

/**
 * List of context providers that are required to run Venia
 *
 * @property {React.Component[]} contextProviders
 */
const contextProviders = [Peregrine, WindowSizeContextProvider, ToastContextProvider];

const ContextProvider = ({ children }) => {
    return (
        <ReduxProvider store={store}>
            {contextProviders.reduceRight((memo, ContextProvider) => {
                return <ContextProvider>{memo}</ContextProvider>;
            }, children)}
        </ReduxProvider>
    );
};

export default ContextProvider;
