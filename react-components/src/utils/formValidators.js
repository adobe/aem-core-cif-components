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

const SUCCESS = undefined;

export const hasLengthAtLeast = (value, values, minimumLength) => {
    if (!value || value.length < minimumLength) {
        return `Must contain at least ${minimumLength} character(s).`;
    }

    return SUCCESS;
};

export const hasLengthAtMost = (value, values, maximumLength) => {
    if (value && value.length > maximumLength) {
        return `Must not exceed ${maximumLength} character(s).`;
    }

    return SUCCESS;
};

export const hasLengthExactly = (value, values, length) => {
    if (value && value.length !== length) {
        return `Must contain exactly ${length} character(s).`;
    }

    return SUCCESS;
};

export const isRequired = value => {
    return (value || '').trim() ? SUCCESS : 'The field is required.';
};

export const validateEmail = value => {
    const regex = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

    return regex.test(value) ? SUCCESS : 'Please enter a valid email address (Ex: johndoe@domain.com).';
};

export const validateRegionCode = (value, values, countries) => {
    const selectedCountry = values.countryCode;
    if (selectedCountry !== 'US') {
        // not validating the state for countries other than US
        // this is actually more complex since on the Magento side
        // you can define this in the store configuration
        // ...but we don't read the store configuration now.
        return SUCCESS;
    }
    if (!value || value.length === 0) {
        return 'This field is mandatory';
    }

    let lengthValidation = hasLengthExactly(value, values, 2);
    if (lengthValidation) {
        return lengthValidation;
    }

    const country = countries.find(({ id }) => id === 'US');

    const { available_regions: regions } = country;

    if (!(Array.isArray(regions) && regions.length)) {
        return 'Country "US" does not contain any available regions.';
    }
    const region = regions.find(({ code }) => code === value);
    if (!region) {
        return `State "${value}" is not an valid state abbreviation.`;
    }

    return SUCCESS;
};

export const validatePassword = value => {
    const count = {
        lower: 0,
        upper: 0,
        digit: 0,
        special: 0
    };

    for (const char of value) {
        if (/[a-z]/.test(char)) count.lower++;
        else if (/[A-Z]/.test(char)) count.upper++;
        else if (/\d/.test(char)) count.digit++;
        else if (/\S/.test(char)) count.special++;
    }

    if (Object.values(count).filter(Boolean).length < 3) {
        return 'A password must contain at least 3 of the following: lowercase, uppercase, digits, special characters.';
    }

    return SUCCESS;
};

export const validateConfirmPassword = (value, values, passwordKey = 'password') => {
    return value === values[passwordKey] ? SUCCESS : 'Passwords must match.';
};

export const isNotEqualToField = (value, values, otherField) => {
    return value !== values[otherField] ? SUCCESS : `${otherField} must be different`;
};
