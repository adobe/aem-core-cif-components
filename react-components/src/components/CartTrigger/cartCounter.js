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
import PropTypes from 'prop-types';

import classes from './cartCounter.css';

/**
 * @deprecated replace with peregrine backed component, will be removed with CIF 3.0 latest
 */
const CartCounter = props => {
    const { counter } = props;
    return counter > 0 ? (
        <span className={classes.root} data-testid="cart-counter">
            {counter}
        </span>
    ) : null;
};

CartCounter.propTypes = {
    counter: PropTypes.number.isRequired
};

export default CartCounter;
