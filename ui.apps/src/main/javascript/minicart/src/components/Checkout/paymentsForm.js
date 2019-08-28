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
import React, { useCallback, useState, useRef, useEffect } from 'react';
import { Form } from 'informed';
import { array, bool, shape, string } from 'prop-types';

import Button from '../Button';
import Select from '../Select';
import Checkbox from '../Checkbox';
import Field from '../Field';
import TextInput from '../TextInput';

import classes from './paymentsForm.css';
import isObjectEmpty from '../../utils/isObjectEmpty';
import { isRequired, hasLengthExactly, validateRegionCode, validateEmail } from '../../utils/formValidators';
import combine from '../../utils/combineValidators';

const DEFAULT_FORM_VALUES = {
    addresses_same: true
};

/**
 * A wrapper around the payment form. This component's purpose is to maintain
 * the submission state as well as prepare/set initial values.
 */
const PaymentsForm = props => {
    const { initialPaymentMethod, initialValues, paymentMethods, cancel, countries, cart, submit } = props;
    const [isSubmitting, setIsSubmitting] = useState(false);

    const anchorRef = useRef(null);

    const paymentMethodsItems = paymentMethods.map(item => {
        return {
            label: item.title,
            value: item.code
        };
    });

    const initialPaymentMethodState = isObjectEmpty(initialPaymentMethod)
        ? paymentMethodsItems[0].value
        : initialPaymentMethod.code;

    const [paymentMethod, setPaymentMethod] = useState(initialPaymentMethodState);

    let initialFormValues;
    if (isObjectEmpty(initialValues)) {
        initialFormValues = DEFAULT_FORM_VALUES;
    } else {
        if (initialValues.sameAsShippingAddress) {
            // If the addresses are the same, don't populate any fields
            // other than the checkbox with an initial value.
            initialFormValues = {
                addresses_same: true
            };
        } else {
            // The addresses are not the same, populate the other fields.
            initialFormValues = {
                addresses_same: false,
                ...initialValues
            };
            delete initialFormValues.sameAsShippingAddress;
        }
    }

    const [differentAddress, setDifferentAddress] = useState(!initialFormValues.addresses_same);

    const handleSubmit = useCallback(
        formValues => {
            setIsSubmitting(true);
            console.log(`Got form values`, formValues);
            console.log(`Got payment method`, paymentMethod);
            const sameAsShippingAddress = formValues['addresses_same'];
            let billingAddress;
            if (!sameAsShippingAddress) {
                billingAddress = {
                    city: formValues['city'],
                    email: formValues['email'],
                    firstname: formValues['firstname'],
                    lastname: formValues['lastname'],
                    postcode: formValues['postcode'],
                    region_code: formValues['region_code'],
                    street: formValues['street'],
                    telephone: formValues['telephone']
                };
            } else {
                billingAddress = {
                    sameAsShippingAddress
                };
            }
            submit({
                paymentMethod: paymentMethods.find(v => v.code === paymentMethod),
                billingAddress
            });
        },
        [setIsSubmitting, paymentMethod]
    );

    const billingAddressFields = differentAddress ? (
        <>
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
            <span ref={anchorRef} />
        </>
    ) : null;

    // When the address checkbox is unchecked, additional fields are rendered.
    // This causes the form to grow, and potentially to overflow, so the new
    // fields may go unnoticed. To reveal them, we scroll them into view.
    useEffect(() => {
        if (differentAddress) {
            const { current: element } = anchorRef;

            if (element instanceof HTMLElement) {
                element.scrollIntoView({ behavior: 'smooth' });
            }
        }
    }, [differentAddress]);

    return (
        <Form className={classes.root} initialValues={initialFormValues} onSubmit={handleSubmit}>
            <div className={classes.body}>
                <h2 className={classes.heading}>Billing Information</h2>
                <div className={classes.braintree}>
                    <Select
                        items={paymentMethodsItems}
                        field="payment_method"
                        initialValue={paymentMethod}
                        handleOnChange={setPaymentMethod}
                        fieldState={{}}
                    />
                </div>
                <div className={classes.address_check}>
                    <Checkbox
                        field="addresses_same"
                        label="Billing address same as shipping address"
                        onClick={ev => {
                            setDifferentAddress(!ev.target.checked);
                        }}
                    />
                </div>
                {billingAddressFields}
            </div>
            <div className={classes.footer}>
                <Button className={classes.button} onClick={cancel}>
                    Cancel
                </Button>
                <Button className={classes.button} priority="high" type="submit" disabled={isSubmitting}>
                    Use Payment Method
                </Button>
            </div>
        </Form>
    );
};

PaymentsForm.propTypes = {
    classes: shape({
        root: string
    }),
    initialValues: shape({
        firstname: string,
        lastname: string,
        telephone: string,
        city: string,
        postcode: string,
        region_code: string,
        sameAsShippingAddress: bool,
        street0: array
    })
};

PaymentsForm.defaultProps = {
    initialValues: {}
};

export default PaymentsForm;
