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
import { func } from 'prop-types';
import { Info as InfoIcon } from 'react-feather';
import { useTranslation } from 'react-i18next';

import AccountLink from '../accountLink';

const AccountInfoLink = props => {
    const { showAccountInformation } = props;

    const [t] = useTranslation('account');
    return (
        <AccountLink onClick={showAccountInformation}>
            <InfoIcon size={18} />
            {t('account:account-information', 'Account Information')}
        </AccountLink>
    );
};

AccountInfoLink.propTypes = {
    showAccountInformation: func.isRequired
};

export default AccountInfoLink;
