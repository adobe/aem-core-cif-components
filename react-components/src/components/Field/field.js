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
import React, { Component } from 'react';
import { bool, node, shape, string } from 'prop-types';

import classes from './field.css';

class Field extends Component {
    static propTypes = {
        children: node,
        classes: shape({
            label: string,
            root: string
        }),
        label: node,
        required: bool
    };

    get requiredSymbol() {
        const { required } = this.props;
        return required ? <span className={classes.requiredSymbol} /> : null;
    }

    render() {
        const { children, label } = this.props;

        return (
            <div className={classes.root}>
                <span className={classes.label}>
                    {this.requiredSymbol}
                    {label}
                </span>
                {children}
            </div>
        );
    }
}

export default Field;
