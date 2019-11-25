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

import classes from './mask.css';
import { useCartState } from '../Minicart/cartContext';

const Mask = () => {
    const [{ isOpen }, dispatch] = useCartState();
    const className = isOpen ? classes.root_active : classes.root;

    return (
        <button
            data-role="mask"
            className={className}
            onClick={() => {
                dispatch({ type: 'close' });
            }}
        />
    );
};

Mask.propTypes = {
    classes: PropTypes.shape({
        root: PropTypes.string,
        root_active: PropTypes.string
    })
};

export default Mask;
