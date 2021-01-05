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
import { func, bool, number, string } from 'prop-types';
import { useTranslation } from 'react-i18next';

const TextField = props => {
    const [t] = useTranslation('common');
    const { textarea, option_id, max_characters, customization, handleSelectionChange } = props;

    const onChange = event => {
        const { value } = event.target;
        if (max_characters > 0) {
            handleSelectionChange(option_id, value.substr(0, max_characters));
        } else {
            handleSelectionChange(option_id, value);
        }
    };

    return (
        <div className="productOptionSelect__root">
            {textarea ? (
                <textarea className="field__textarea" rows={5} value={customization} onChange={onChange}></textarea>
            ) : (
                <input className="field__input" value={customization} onChange={onChange} />
            )}
            {max_characters > 0 && (
                <small>{`${t('common:maximum', 'Maximum')} ${max_characters} ${t(
                    'common:characters',
                    'characters'
                ).toLowerCase()}`}</small>
            )}
        </div>
    );
};

TextField.propTypes = {
    textarea: bool,
    option_id: number.isRequired,
    customization: string.isRequired,
    max_characters: number.isRequired,
    currencyCode: string.isRequired,
    handleSelectionChange: func.isRequired
};

export default TextField;
