/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
import QUERY_COUNTRIES from '../../queries/query_countries.graphql';

export default {
    request: {
        query: QUERY_COUNTRIES
    },
    result: {
        data: {
            countries: [
                {
                    id: 'RO',
                    full_name_locale: 'Romania',
                    available_regions: [
                        { id: 835, code: 'AB', name: 'Alba' },
                        { id: 838, code: 'AR', name: 'Arad' }
                    ]
                },
                {
                    id: 'US',
                    full_name_locale: 'United States',
                    available_regions: [
                        { id: 4, code: 'AL', name: 'Alabama' },
                        { id: 7, code: 'AK', name: 'Alaska' }
                    ]
                }
            ]
        }
    }
};
