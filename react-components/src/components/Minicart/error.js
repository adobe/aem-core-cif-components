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
import { useTranslation } from 'react-i18next';

import Button from '../Button';

import classes from './error.css';
import { useCartState } from './cartContext';

const Error = () => {
    const [{ errorMessage }, dispatch] = useCartState();
    const [t] = useTranslation('common');

    return (
        <div className={classes.root}>
            <h2>Error</h2>
            <p>{errorMessage}</p>
            <div className={classes.action}>
                <Button
                    priority="high"
                    onClick={() => {
                        dispatch({ type: 'discardError' });
                    }}>
                    <span>{t('common:close', 'Close')}</span>
                </Button>
            </div>
        </div>
    );
};

export default Error;
