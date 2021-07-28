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
import { func, number } from 'prop-types';

import { useAddressSelect } from './useAddressSelect';
import Select from '../Select';

import classes from './addressSelect.css';

const AddressSelect = props => {
    const { initialValue, onValueChange } = props;
    const { addressSelectItems } = useAddressSelect();

    return (
        <div className={classes.root}>
            <Select
                items={addressSelectItems}
                field="address_select"
                initialValue={initialValue}
                onValueChange={onValueChange}
            />
        </div>
    );
};

AddressSelect.propTypes = {
    initialValue: number,
    onValueChange: func
};

export default AddressSelect;
