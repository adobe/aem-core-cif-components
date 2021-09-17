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
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import React, { Suspense } from 'react';

import LoadingIndicator from '../LoadingIndicator';

const withSuspense = Component => {
    let WithSuspense = props => {
        return (
            <Suspense fallback={<LoadingIndicator />}>
                <Component {...props} />
            </Suspense>
        );
    };
    WithSuspense.displayName = `withSuspense(${Component.displayName || Component.name})`;
    return WithSuspense;
};

const Portal = props => {
    let { selector, children } = props;

    let elem;
    if (selector instanceof HTMLElement) {
        elem = selector;
    } else if (typeof selector === 'string') {
        elem = document.querySelector(selector);
    }

    if (elem) {
        // Only render children if mounting point is available
        // Remove any node in the host element first
        while(elem.hasChildNodes()) {
            elem.childNodes[0].remove();
        }
        return ReactDOM.createPortal(children, elem);
    }

    return null;
};

Portal.propTypes = {
    selector: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(HTMLElement)]).isRequired
};

export default withSuspense(Portal);
