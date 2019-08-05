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

import React, { Component, createRef } from 'react';
import { shape, string } from 'prop-types';
import classes from './kebab.css';

import MoreVerticalIcon from 'react-feather/dist/icons/more-vertical';

class Kebab extends Component {
    static propTypes = {
        classes: shape({
            dropdown: string,
            dropdown_active: string,
            kebab: string,
            root: string
        })
    };

    constructor(props) {
        super(props);
        this.kebabButtonRef = createRef();

        this.state = {
            isOpen: false
        };
    }

    componentDidMount() {
        document.addEventListener('click', this.handleDocumentClick);
        document.addEventListener('touchend', this.handleDocumentClick);
    }

    handleDocumentClick = event => {
        this.kebabButtonRef.current.contains(event.target)
            ? this.setState({ isOpen: true })
            : this.setState({ isOpen: false });
    };

    componentWillUnmount() {
        document.removeEventListener('click', this.handleDocumentClick);
        document.removeEventListener('touchend', this.handleDocumentClick);
    }

    render() {
        const { children, ...restProps } = this.props;

        const toggleClass = this.state.isOpen ? classes.dropdown_active : classes.dropdown;

        return (
            <div {...restProps} className={classes.root}>
                <button className={classes.kebab} ref={this.kebabButtonRef}>
                    <MoreVerticalIcon />
                </button>
                <ul className={toggleClass}>{children}</ul>
            </div>
        );
    }
}

export default Kebab;
