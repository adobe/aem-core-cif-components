/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
export const searchRequestEvent = {
    type: 'SEARCH_REQUEST',
    payload: {
        query: 'selena',
        refinements: [
            {
                attribute: 'category_id',
                value: new Set(['Bottoms,11']),
                isRange: false
            },
            {
                attribute: 'fashion_color',
                value: new Set(['Rain,34', 'Mint,25']),
                isRange: false
            }
        ],
        sort: {
            attribute: 'relevance',
            order: 'DESC'
        },
        pageSize: 12,
        currentPage: 1
    }
};

export const searchbarRequestEvent = {
    type: 'SEARCHBAR_REQUEST',
    payload: {
        query: 'selena',
        currentPage: 1,
        pageSize: 3,
        refinements: []
    }
};
