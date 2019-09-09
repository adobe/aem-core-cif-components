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
import { Form } from 'informed';
import { array, bool, func, object, shape, string } from 'prop-types';

import Button from '../Button';
import classes from './addressForm.css';
import { validateEmail, isRequired, hasLengthExactly, validateRegionCode } from '../../utils/formValidators';
import combine from '../../utils/combineValidators';
import TextInput from '../TextInput';
import Field from '../Field';

const fields = ['city', 'email', 'firstname', 'lastname', 'postcode', 'region_code', 'street', 'telephone'];

const AddressForm = props => {
    const [submitting, setIsSubmitting] = useState(false);
    const { cancel, countries, isAddressInvalid, invalidAddressMessage, initialValues, submit } = props;
    const validationMessage = isAddressInvalid ? invalidAddressMessage : null;

    const values = useMemo(
        () =>
            fields.reduce((acc, key) => {
                acc[key] = initialValues[key];
                return acc;
            }, {}),
        [initialValues]
    );

    const handleSubmit = useCallback(
        values => {
            setIsSubmitting(true);
            submit(values);
        },
        [submit]
    );
    return (
        <Form className={classes.root} initialValues={values} onSubmit={handleSubmit}>
            <div className={classes.body}>
                <h2 className={classes.heading}>Shipping Address</h2>
                <div className={classes.firstname}>
                    <Field label="First Name">
                        <TextInput id={classes.firstname} field="firstname" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.lastname}>
                    <Field label="Last Name">
                        <TextInput id={classes.lastname} field="lastname" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.email}>
                    <Field label="Email">
                        <TextInput id={classes.email} field="email" validate={combine([isRequired, validateEmail])} />
                    </Field>
                </div>
                <div className={classes.street0}>
                    <Field label="Street">
                        <TextInput id={classes.street0} field="street[0]" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.city}>
                    <Field label="City">
                        <TextInput id={classes.city} field="city" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.region_code}>
                    <Field label="State">
                        <TextInput
                            id={classes.region_code}
                            field="region_code"
                            validate={combine([isRequired, [hasLengthExactly, 2], [validateRegionCode, countries]])}
                        />
                    </Field>
                </div>
                <div className={classes.postcode}>
                    <Field label="ZIP">
                        <TextInput id={classes.postcode} field="postcode" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.telephone}>
                    <Field label="Phone">
                        <TextInput id={classes.telephone} field="telephone" validate={isRequired} />
                    </Field>
                </div>
                <div className={classes.validation}>{validationMessage}</div>
            </div>
            <div className={classes.footer}>
                <Button className={classes.button} onClick={cancel}>
                    Cancel
                </Button>
                <Button className={classes.button} type="submit" priority="high" disabled={submitting}>
                    Use Address
                </Button>
            </div>
        </Form>
    );
};

AddressForm.propTypes = {
    cancel: func.isRequired,
    classes: shape({
        body: string,
        button: string,
        city: string,
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
    invalidAddressMessage: string,
    initialValues: object,
    isAddressInvalid: bool,
    submit: func.isRequired,
    submitting: bool
};

AddressForm.defaultProps = {
    initialValues: {}
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
    region_id: 33,
    region_code: 'MI',
    region: 'Michigan',
    telephone: '(555) 229-3326',
    email: 'veronica@example.com'
};
*/
