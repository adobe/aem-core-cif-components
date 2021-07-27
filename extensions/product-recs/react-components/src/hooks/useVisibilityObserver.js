/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

let cleared = {};

export const useVisibilityObserver = props => {
    const {
        options = {
            threshold: [0.0, 0.5]
        }
    } = props || {};

    const callback = (entries, isVisibleCallback) => {
        entries.forEach(entry => {
            const { target, isIntersecting, intersectionRatio } = entry;

            if (!isIntersecting) {
                cleared[target] = true;
            }

            if (cleared[target] !== false && intersectionRatio >= 0.5) {
                cleared[target] = false;
                isVisibleCallback();
            }
        });
    };

    const observeElement = (element, isVisibleCallback) => {
        if (!element) {
            return;
        }

        const observer = new IntersectionObserver(entries => callback(entries, isVisibleCallback), options);
        observer.observe(element);
    };

    return { observeElement };
};
