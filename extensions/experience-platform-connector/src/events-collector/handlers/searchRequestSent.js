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
const canHandle = event => ['SEARCH_REQUEST', 'SEARCHBAR_REQUEST'].includes(event.type);

const handle = (sdk, event) => {
    const { payload } = event;

    const { query, pageSize, currentPage, refinements, sort } = payload;

    const filter = refinements.map(refinement => {
        const { attribute, value } = refinement;
        return {
            attribute: attribute,
            in: Array.from(value.values())
        };
    });

    const requestContext = {
        units: [
            {
                searchUnitId: 'productPage',
                queryTypes: ['products'],
                phrase: query,
                pageSize: pageSize,
                currentPage: currentPage,
                filter: filter,
                sort: [{ attribute: sort?.attribute, direction: sort?.order }]
            }
        ]
    };

    sdk.context.setSearchInput(requestContext);
    sdk.publish.searchRequestSent();
};

export default {
    canHandle,
    handle
};
