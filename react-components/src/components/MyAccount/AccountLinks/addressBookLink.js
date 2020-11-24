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
import { Book as BookIcon } from 'react-feather';
import { useTranslation } from 'react-i18next';

import { useConfigContext } from '../../../context/ConfigContext';
import AccountLink from '../accountLink';

const AddressBookLink = () => {
    const { pagePaths } = useConfigContext();
    const [t] = useTranslation('account');
    return (
        <AccountLink
            onClick={() => {
                window.location.href = pagePaths.addressBook;
            }}>
            <BookIcon size={18} />
            {t('account:address-book', 'Address Book')}
        </AccountLink>
    );
};

export default AddressBookLink;
