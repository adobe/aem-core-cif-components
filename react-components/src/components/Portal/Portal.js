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
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';

const Portal = props => {
    const { selector, children } = props;
    const elem = document.querySelector(selector);

    if (elem) {
        // Only render children if mounting point is available
        return ReactDOM.createPortal(children, elem);
    }

    return null;
};

Portal.propTypes = {
    selector: PropTypes.string.isRequired
};

export default Portal;
