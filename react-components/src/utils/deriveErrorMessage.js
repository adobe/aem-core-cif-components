/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

/**
 * Maps an error to a string message
 *
 * @param {Error} error the error to map
 * @return {String} error message
 */
const toString = error => {
    const { graphQLErrors, message } = error;

    return graphQLErrors && graphQLErrors.length ? graphQLErrors.map(({ message }) => message).join(', ') : message;
};

/**
 * A function to derive an error string from an array of errors.
 */
export const deriveErrorMessage = errors => {
    const errorCollection = [];
    for (const error of errors) {
        if (error) {
            errorCollection.push(toString(error));
        }
    }

    return errorCollection.join(', ');
};
