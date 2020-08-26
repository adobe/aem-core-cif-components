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

import { useState, useCallback } from 'react';
import { useUserContext } from '../../context/UserContext';

const useForgotPasswordForm = props => {
    const { onClose, onCancel } = props;
    const [, { resetPassword }] = useUserContext();

    const [submitting, setSubmitting] = useState(false);
    const [formEmail, setFormEmail] = useState(null);

    const handleFormSubmit = useCallback(
        async ({ email }) => {
            setSubmitting(true);
            setFormEmail(email);
            await resetPassword(email);
        },
        [resetPassword]
    );

    const handleCancel = useCallback(() => {
        if (onCancel) {
            setSubmitting(false);
            onCancel();
        }
    }, [onCancel]);

    const handleContinue = useCallback(() => {
        setSubmitting(false);
        onClose();
    }, [onClose]);

    return [
        { submitting, email: formEmail },
        {
            handleFormSubmit,
            handleCancel,
            handleContinue
        }
    ];
};

export default useForgotPasswordForm;
