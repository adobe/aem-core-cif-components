/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import { useIntl } from 'react-intl';

import Trigger from '../Trigger';
import { useUserContext } from '../../context/UserContext';
import classes from './createAccountSuccess.css';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const CreateAccountSuccess = props => {
    const { showSignIn } = props;
    const [{ createAccountEmail }, { dispatch }] = useUserContext();
    const intl = useIntl();

    const handleSignIn = () => {
        dispatch({ type: 'cleanupAccountCreated' });
        showSignIn();
    };

    return (
        <div className={classes.root}>
            <div className={classes.body}>
                <h2 className={classes.header}>
                    {intl.formatMessage({
                        id: 'account:account-created-title',
                        defaultMessage: 'Your account was successfully created'
                    })}
                </h2>
                <div className={classes.textBlock}>
                    {intl.formatMessage(
                        {
                            id: 'account:email-confirmation-info',
                            defaultMessage:
                                'You will receive a link at {email}. Access that link to confirm your email address.'
                        },
                        { email: createAccountEmail }
                    )}
                </div>
                <div className={classes.actions}>
                    <Trigger action={handleSignIn}>
                        <span className={classes.signin}>
                            {intl.formatMessage({ id: 'account:sign-in', defaultMessage: 'Sign In' })}
                        </span>
                    </Trigger>
                </div>
            </div>
        </div>
    );
};

CreateAccountSuccess.propTypes = {
    showSignIn: func.isRequired
};

export default CreateAccountSuccess;
