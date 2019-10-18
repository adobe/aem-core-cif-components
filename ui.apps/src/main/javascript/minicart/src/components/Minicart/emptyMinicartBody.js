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
import Trigger from '../Trigger';
import classes from './emptyMiniCartBody.css';
import { useCartState } from '../../utils/state';

const EmptyMinicartBody = () => {
    const [, dispatch] = useCartState();
    return (
        <div className={classes.root} data-testid="empty-minicart">
            <h3 className={classes.emptyTitle}>There are no items in your shopping cart</h3>
            <Trigger
                action={() => {
                    dispatch({ type: 'close' });
                }}>
                <span className={classes.continue}>Continue Shopping</span>
            </Trigger>
        </div>
    );
};

export default EmptyMinicartBody;
