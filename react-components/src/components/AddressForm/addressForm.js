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
import React, { useCallback, useMemo, useState } from 'react';
import { Form, useFieldState, useFormState } from 'informed';
import { array, bool, func, object, shape, string, number } from 'prop-types';
import { useTranslation } from 'react-i18next';

import Button from '../Button';
import Select from '../Select';
import classes from './addressForm.css';
import { validateEmail, isRequired } from '../../utils/formValidators';
import combine from '../../utils/combineValidators';

import { useAddressForm } from './useAddressForm';

import AddressSelect from './addressSelect';
import Checkbox from '../Checkbox';
import Field from '../Field';
import TextInput from '../TextInput';

const AddressForm = props => {
    const { parseAddressFormValues } = useAddressForm({});
    const [submitting, setIsSubmitting] = useState(false);
    const {
        cancel,
        countries,
        formErrorMessage,
        formHeading,
        isAddressInvalid,
        invalidAddressMessage,
        initialAddressSelectValue,
        initialValues,
        onAddressSelectValueChange,
        showAddressSelect,
        showDefaultAddressCheckbox,
        showEmailInput,
        showSaveInAddressBookCheckbox,
        submit,
        submitButtonLabel
    } = props;
    const validationMessage = isAddressInvalid ? invalidAddressMessage : null;
    const errorMessage = formErrorMessage ? formErrorMessage : null;
    const [t] = useTranslation(['account', 'checkout', 'common']);

    const displayCountries = useMemo(() => {
        if (!countries || countries.length === 0) {
            return [];
        }
        return countries.map(country => {
            return {
                label: country['full_name_locale'],
                value: country['id']
            };
        });
    }, [countries]);

    const handleSubmit = useCallback(
        values => {
            if (!values['region_code'] || values['region_code'].length === 0) {
                // add an empty `region_code` value since
                // the form doesn't provide one if you leave the field empty
                values['region_code'] = '';
            }
            setIsSubmitting(true);
            // Convert street back to array
            submit({ ...values, street: [values.street0] });
        },
        [submit]
    );

    const Regions = () => {
        const { value: countryCode } = useFieldState('country_code');
        const { values: formValues } = useFormState();
        const country = countries.find(({ id }) => countryCode === id);

        if (!country || !country.available_regions) {
            return <TextInput id={classes.region_code} field="region_code" />;
        }

        // US do not have regions with unique codes
        const uniqueRegions = [...new Set(country.available_regions.map(item => item.code))];
        const displayRegions = uniqueRegions.map(id => {
            let entry = country.available_regions.find(({ code }) => code === id);
            return {
                value: entry.code,
                label: entry.name
            };
        });

        return (
            <Select
                id={classes.region_code}
                field="region_code"
                items={[{ value: '', label: '' }, ...displayRegions]}
                validate={isRequired}
            />
        );
    };

    return (
        <Form className={classes.root} initialValues={parseAddressFormValues(initialValues)} onSubmit={handleSubmit}>
            {({ formApi }) => (
                <>
                    <div className={classes.body}>
                        <h2 className={classes.heading}>{formHeading}</h2>
                        {showAddressSelect && (
                            <div className={classes.address_select}>
                                <Field label={t('checkout:address-use-saved-address', 'Use Saved Address')}>
                                    <AddressSelect
                                        initialValue={initialAddressSelectValue}
                                        onValueChange={value => onAddressSelectValueChange(value, formApi)}
                                    />
                                </Field>
                            </div>
                        )}
                        <div className={classes.firstname}>
                            <Field label={t('checkout:address-firstname', 'First Name')}>
                                <TextInput
                                    id={classes.firstname}
                                    field="firstname"
                                    validateOnBlur
                                    validate={isRequired}
                                />
                            </Field>
                        </div>
                        <div className={classes.lastname}>
                            <Field label={t('checkout:address-lastname', 'Last Name')}>
                                <TextInput
                                    id={classes.lastname}
                                    field="lastname"
                                    validateOnBlur
                                    validate={isRequired}
                                />
                            </Field>
                        </div>
                        {showEmailInput && (
                            <div className={classes.email}>
                                <Field label={t('checkout:address-email', 'E-Mail')}>
                                    <TextInput
                                        id={classes.email}
                                        field="email"
                                        validateOnBlur
                                        validate={combine([isRequired, validateEmail])}
                                    />
                                </Field>
                            </div>
                        )}
                        <div className={classes.street0}>
                            <Field label={t('checkout:address-street', 'Street')}>
                                <TextInput id={classes.street0} field="street0" validateOnBlur validate={isRequired} />
                            </Field>
                        </div>
                        <div className={classes.city}>
                            <Field label={t('checkout:address-city', 'City')}>
                                <TextInput id={classes.city} field="city" validateOnBlur validate={isRequired} />
                            </Field>
                        </div>
                        <div className={classes.country}>
                            <Field label={t('checkout:country', 'Country')}>
                                <Select field="country_code" items={displayCountries} />
                            </Field>
                        </div>
                        <div className={classes.region_code}>
                            <Field label={t('checkout:address-state', 'State')}>
                                <Regions />
                            </Field>
                        </div>
                        <div className={classes.postcode}>
                            <Field label={t('checkout:address-postcode', 'ZIP')}>
                                <TextInput
                                    id={classes.postcode}
                                    field="postcode"
                                    validateOnBlur
                                    validate={isRequired}
                                />
                            </Field>
                        </div>
                        <div className={classes.telephone}>
                            <Field label={t('checkout:address-phone', 'Phone')}>
                                <TextInput
                                    id={classes.telephone}
                                    field="telephone"
                                    validateOnBlur
                                    validate={isRequired}
                                />
                            </Field>
                        </div>
                        {showDefaultAddressCheckbox && (
                            <div className={classes.default_shipping}>
                                <Checkbox
                                    id={classes.default_shipping}
                                    label={t('checkout:address-default-address', 'Make my default address')}
                                    field="default_shipping"
                                />
                            </div>
                        )}
                        {showSaveInAddressBookCheckbox && (
                            <div className={classes.save_in_address_book}>
                                <Checkbox
                                    id={classes.save_in_address_book}
                                    label={t('checkout:address-save-in-address-book', 'Save in address book')}
                                    field="save_in_address_book"
                                />
                            </div>
                        )}
                        <div className={classes.validation}>{validationMessage}</div>
                        <div className={classes.error}>{errorMessage}</div>
                    </div>
                    <div className={classes.footer}>
                        <Button onClick={cancel}>{t('common:cancel', 'Cancel')}</Button>
                        <Button type="submit" priority="high" disabled={submitting}>
                            {submitButtonLabel}
                        </Button>
                    </div>
                </>
            )}
        </Form>
    );
};

AddressForm.propTypes = {
    cancel: func.isRequired,
    classes: shape({
        body: string,
        button: string,
        city: string,
        default_shipping: string,
        email: string,
        firstname: string,
        footer: string,
        heading: string,
        lastname: string,
        postcode: string,
        root: string,
        region_code: string,
        street0: string,
        telephone: string,
        validation: string
    }),
    countries: array,
    formErrorMessage: string,
    formHeading: string,
    invalidAddressMessage: string,
    initialAddressSelectValue: number,
    initialValues: object,
    isAddressInvalid: bool,
    onAddressSelectValueChange: func,
    showAddressSelect: bool,
    showDefaultAddressCheckbox: bool,
    showEmailInput: bool,
    showSaveInAddressBookCheckbox: bool,
    submit: func.isRequired,
    submitting: bool,
    submitButtonLabel: string
};

AddressForm.defaultProps = {
    initialAddressSelectValue: null,
    initialValues: {},
    showAddressSelect: false,
    showDefaultAddressCheckbox: false,
    showEmailInput: false,
    showSaveInAddressBookCheckbox: false
};

export default AddressForm;

/*
const mockAddress = {
    country_id: 'US',
    firstname: 'Veronica',
    lastname: 'Costello',
    street: ['6146 Honey Bluff Parkway'],
    city: 'Calder',
    postcode: '49628-7978',
    region_code: 'MI',
    region: {
        region_code: 'MI'
    },
    telephone: '(555) 229-3326',
    email: 'veronica@example.com'
};
*/
