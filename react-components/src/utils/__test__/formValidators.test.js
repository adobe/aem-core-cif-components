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

import { validateRegionCode } from '../formValidators';

describe('Form validators', () => {
    const countries = [
        {
            id: 'US',
            name: 'United States',
            available_regions: [{ code: 'AK' }, { code: 'CA' }]
        },
        { id: 'RO', name: 'Romania', available_regions: [] }
    ];

    it('validates an existing region code for US', () => {
        let values = { countryCode: 'US' };
        let value = 'AK';

        let result = validateRegionCode(value, values, countries);

        expect(result).toBeUndefined();
    });
    it('validates a non-existing region code for US', () => {
        let values = { countryCode: 'US' };
        let value = 'NO';

        let result = validateRegionCode(value, values, countries);
        expect(result).toEqual(`State "${value}" is not an valid state abbreviation.`);
    });
    it('validates a empty region code for US', () => {
        let values = { countryCode: 'US' };
        let value = '';

        let result = validateRegionCode(value, values, countries);
        expect(result).toEqual('This field is mandatory');
    });
    it('validates a empty region code for a country other than US', () => {
        let values = { countryCode: 'RO' };
        let value = '';

        let result = validateRegionCode(value, values, countries);
        expect(result).toBeUndefined();
    });
});
