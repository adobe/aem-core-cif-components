/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
const localStorage = window.localStorage;

export const checkItem = itemKey => {
    return localStorage.getItem(itemKey) !== null;
};

export const getPlainItem = itemKey => {
    return localStorage.getItem(itemKey);
};

export const getJsonItem = itemKey => {
    let result = null;

    try {
        result = JSON.parse(localStorage.getItem(itemKey));
    } catch (e) {
        console.error(`Local Storage item ${itemKey} is not a JSON.`);
    }

    return result;
};
