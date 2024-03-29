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
import { func } from 'prop-types';
import React from 'react';
import { Lock as PasswordIcon } from 'react-feather';
import { useIntl } from 'react-intl';

import AccountLink from '../accountLink';

const ChangePasswordLink = props => {
    const { showChangePassword } = props;
    const intl = useIntl();

    return (
        <AccountLink onClick={showChangePassword}>
            <PasswordIcon size={18} />
            {intl.formatMessage({ id: 'account:change-password', defaultMessage: 'Change Password' })}
        </AccountLink>
    );
};

ChangePasswordLink.propTypes = {
    showChangePassword: func.isRequired
};

export default ChangePasswordLink;
