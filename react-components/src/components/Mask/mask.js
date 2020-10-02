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
import { shape, string, func, bool } from 'prop-types';

import defaultClasses from './mask.css';
import mergeClasses from '../../utils/mergeClasses';

const Mask = ({ onClickHandler, isOpen, customClasses }) => {
    const classes = mergeClasses(defaultClasses, customClasses);
    const className = isOpen ? classes.root_active : classes.root;

    return <button data-role="mask" className={className} onClick={onClickHandler} />;
};

Mask.propTypes = {
    classes: shape({
        root: string,
        root_active: string
    }),
    onClickHandler: func.isRequired,
    isOpen: bool,
    customClasses: shape({
        root: string,
        root_active: string
    })
};

export default Mask;
