/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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

import i18nMessagesProductRecs from '../../i18n/en.json';
import i18nMessagesCoreComps from '../../../../../react-components/i18n/en.json';

const i18nMessages = { ...i18nMessagesCoreComps, ...i18nMessagesProductRecs };

const ContextWrapper = ({ children }) => {
    return (
        <IntlProvider locale="en" messages={i18nMessages}>
            {children}
        </IntlProvider>
    );
};

export default ContextWrapper;
