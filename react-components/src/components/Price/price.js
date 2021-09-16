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
import { number, string } from 'prop-types';
import { useIntl } from 'react-intl';

const Price = props => {
    const { value, currencyCode, className } = props;
    const intl = useIntl();

    return <span className={className}>{intl.formatNumber(value, { style: 'currency', currency: currencyCode })}</span>;
};

Price.propTypes = {
    /**
     * The numeric price
     */
    value: number.isRequired,
    /**
     * A string with any of the currency code supported by Intl.NumberFormat
     */
    currencyCode: string.isRequired,
    /**
     * Class name to use when styling this component
     */
    className: string
};

Price.defaultProps = {
    className: ''
};

export default Price;
