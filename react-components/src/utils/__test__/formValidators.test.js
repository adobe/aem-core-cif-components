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

import { hasLengthAtLeast, isNotEqualToField, validateConfirmPassword, hasLengthAtMost } from '../formValidators';

describe('Form validation', () => {
    it('[isNotEqualToField] checks that a field has a different value than other', () => {
        const formValues = {
            password: '1235',
            otherPassword: '1234'
        };

        expect(isNotEqualToField('1234', formValues, 'password')).toBeUndefined();
        expect(isNotEqualToField('1235', formValues, 'password')).toEqual('password must be different');
    });

    it('[validateConfirmPassword] validates the password confirmation field', () => {
        const formValues = {
            password: 'Qwert'
        };

        expect(validateConfirmPassword('Qwert', formValues, 'password')).toBeUndefined();
        expect(validateConfirmPassword('Qwerty', formValues, 'password')).toEqual('Passwords must match.');
    });

    it('[hasLengthAtLeast] validates the minimum length of the field', () => {
        expect(hasLengthAtLeast('12345678', {}, 8)).toBeUndefined();
        expect(hasLengthAtLeast(null, {}, 8)).toEqual(`Must contain at least 8 character(s).`);
        expect(hasLengthAtLeast('12', {}, 8)).toEqual(`Must contain at least 8 character(s).`);
    });

    it('[hasLengthAtMost] validates the maximum length of a field', () => {
        expect(hasLengthAtMost('1234', {}, 4)).toBeUndefined();
        expect(hasLengthAtMost('1234', {}, 4)).toBeUndefined();
        expect(hasLengthAtMost(null, {}, 2)).toBeUndefined();
        expect(hasLengthAtMost('1234', {}, 2)).toEqual('Must not exceed 2 character(s).');
    });
});
