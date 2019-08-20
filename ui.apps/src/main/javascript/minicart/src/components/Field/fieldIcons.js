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
import { string, node, object } from 'prop-types';

import classes from './fieldIcons.css';

const FieldIcons = props => {
    const { after, before, children } = props;

    const style = {
        '--iconsBefore': before ? 1 : 0,
        '--iconsAfter': after ? 1 : 0
    };

    return (
        <span className={classes.root} style={style}>
            <span className={classes.input}>{children}</span>
            <span className={classes.before}>{before}</span>
            <span className={classes.after}>{after}</span>
        </span>
    );
};

FieldIcons.propTypes = {
    after: object,
    before: string,
    root: string,
    children: node
};

export default FieldIcons;
