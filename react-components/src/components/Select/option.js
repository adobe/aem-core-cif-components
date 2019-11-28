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
import PropTypes from 'prop-types';

const Option = props => {
    const { disabled, item, selected } = props;
    const { label, value } = item;
    const text = label != null ? label : value;

    return (
        <option value={value} disabled={disabled} selected={selected}>
            {text}
        </option>
    );
};

Option.propTypes = {
    disabled: PropTypes.bool,
    selected: PropTypes.bool,
    item: PropTypes.shape({
        label: PropTypes.string,
        value: PropTypes.string.isRequired
    }).isRequired
};

export default Option;
