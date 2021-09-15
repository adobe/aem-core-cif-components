/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
import React, { useCallback, useState, useRef, useEffect } from 'react';
import { Form } from 'informed';
import { array, bool, shape, string, func, number } from 'prop-types';
import { useIntl } from 'react-intl';

import AddressSelect from '../AddressForm/addressSelect';
import Button from '../Button';
import Select from '../Select';
import Checkbox from '../Checkbox';
import Field from '../Field';
import TextInput from '../TextInput';
import PaymentProvider from './paymentProviders/paymentProvider';

import classes from './paymentsForm.css';
import { isRequired, hasLengthExactly, validateRegionCode, validateEmail } from '../../utils/formValidators';
import combine from '../../utils/combineValidators';

/**
 * A wrapper around the payment form. This component's purpose is to maintain
 * the submission state as well as prepare/set initial values.
 */
const PaymentsForm = props => {
    const {
        allowSame,
        billingAddressSameAsShippingAddress,
        cancel,
        countries,
        initialAddressSelectValue,
        initialPaymentMethod,
        initialValues,
        onAddressSelectValueChange,
        paymentMethods,
        showAddressSelect,
        showEmailInput,
        showSaveInAddressBookCheckbox,
        submit
    } = props;
    const [isSubmitting, setIsSubmitting] = useState(false);
    const intl = useIntl();

    const anchorRef = useRef(null);

    const paymentMethodsItems = paymentMethods.map(item => {
        return {
            label: item.title,
            value: item.code
        };
    });

    const initialPaymentMethodState = !initialPaymentMethod ? paymentMethodsItems[0].value : initialPaymentMethod.code;
    const [paymentMethod] = useState(initialPaymentMethodState);

    let initialFormValues;
    if (allowSame && billingAddressSameAsShippingAddress) {
        // If the addresses are the same, don't populate any fields
        // other than the checkbox with an initial value.
        initialFormValues = {
            addresses_same: true
        };
    } else {
        // Convert street array
        if (initialValues && initialValues.street) {
            initialValues.street.forEach((v, i) => {
                initialValues[`street${i}`] = v;
            });
        }

        // Convert region code
        if (initialValues && initialValues.region) {
            initialValues.region_code = initialValues.region.code;
        }

        // The addresses are not the same, populate the other fields.
        initialFormValues = {
            addresses_same: false,
            ...initialValues
        };
    }

    const [differentAddress, setDifferentAddress] = useState(!initialFormValues.addresses_same);

    const handleSubmit = useCallback(
        formValues => {
            setIsSubmitting(true);
            const sameAsShippingAddress = formValues['addresses_same'];
            let billingAddress = {
                sameAsShippingAddress
            };
            if (!sameAsShippingAddress) {
                billingAddress = {
                    ...billingAddress,
                    ...formValues,
                    street: [formValues.street0]
                };
            }
            submit({
                paymentMethod: paymentMethods.find(v => v.code === formValues['payment_method']),
                paymentNonce: formValues['payment_nonce'],
                billingAddress
            });
        },
        [setIsSubmitting, paymentMethod]
    );

    const billingAddressFields = differentAddress ? (
        <>
            <div className={classes.firstname}>
                <Field label={intl.formatMessage({ id: 'checkout:address-firstname', defaultMessage: 'First Name' })}>
                    <TextInput id={classes.firstname} field="firstname" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.lastname}>
                <Field label={intl.formatMessage({ id: 'checkout:address-lastname', defaultMessage: 'Last Name' })}>
                    <TextInput id={classes.lastname} field="lastname" validate={isRequired} />
                </Field>
            </div>
            {showEmailInput && (
                <div className={classes.email}>
                    <Field label={intl.formatMessage({ id: 'checkout:address-email', defaultMessage: 'E-Mail' })}>
                        <TextInput id={classes.email} field="email" validate={combine([isRequired, validateEmail])} />
                    </Field>
                </div>
            )}
            <div className={classes.street0}>
                <Field label={intl.formatMessage({ id: 'checkout:address-street', defaultMessage: 'Street' })}>
                    <TextInput id={classes.street0} field="street0" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.city}>
                <Field label={intl.formatMessage({ id: 'checkout:address-city', defaultMessage: 'City' })}>
                    <TextInput id={classes.city} field="city" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.region_code}>
                <Field label={intl.formatMessage({ id: 'checkout:address-state', defaultMessage: 'State' })}>
                    <TextInput
                        id={classes.region_code}
                        field="region_code"
                        validate={combine([isRequired, [hasLengthExactly, 2], [validateRegionCode, countries]])}
                    />
                </Field>
            </div>
            <div className={classes.postcode}>
                <Field label={intl.formatMessage({ id: 'checkout:address-postcode', defaultMessage: 'ZIP' })}>
                    <TextInput id={classes.postcode} field="postcode" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.telephone}>
                <Field label={intl.formatMessage({ id: 'checkout:address-phone', defaultMessage: 'Phone' })}>
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
            {({ formApi }) => (
                <>
                    <div className={classes.body}>
                        <h2 className={classes.heading}>Billing Information</h2>
                        <div className={classes.braintree}>
                            <Select
                                items={paymentMethodsItems}
                                field="payment_method"
                                initialValue={paymentMethod}
                                onValueChange={() =>
                                    formApi.getError('payment_nonce') && formApi.resetField('payment_nonce')
                                }
                            />
                        </div>
                        <PaymentProvider />
                        <div className={classes.address_check}>
                            {allowSame && (
                                <Checkbox
                                    field="addresses_same"
                                    label={intl.formatMessage({
                                        id: 'checkout:same-as-shipping',
                                        defaultMessage: 'Billing address same as shipping address'
                                    })}
                                    onClick={ev => {
                                        setDifferentAddress(!ev.target.checked);
                                    }}
                                />
                            )}
                        </div>
                        {differentAddress && showAddressSelect && (
                            <div className={classes.address_select}>
                                <Field
                                    label={intl.formatMessage({
                                        id: 'checkout:address-use-saved-address',
                                        defaultMessage: 'Use Saved Address'
                                    })}>
                                    <AddressSelect
                                        initialValue={initialAddressSelectValue}
                                        onValueChange={value => onAddressSelectValueChange(value, formApi)}
                                    />
                                </Field>
                            </div>
                        )}
                        {billingAddressFields}
                        {differentAddress && showSaveInAddressBookCheckbox && (
                            <div className={classes.save_in_address_book}>
                                <Checkbox
                                    id={classes.save_in_address_book}
                                    label={intl.formatMessage({
                                        id: 'checkout:address-save-in-address-book',
                                        defaultMessage: 'Save in address book'
                                    })}
                                    field="save_in_address_book"
                                />
                            </div>
                        )}
                    </div>
                    <div className={classes.footer}>
                        <Button onClick={cancel}>
                            {intl.formatMessage({ id: 'common:cancel', defaultMessage: 'Cancel' })}
                        </Button>
                        <Button priority="high" type="submit" disabled={isSubmitting}>
                            {intl.formatMessage({
                                id: 'checkout:use-payment-method',
                                defaultMessage: 'Use Payment Method'
                            })}
                        </Button>
                    </div>
                </>
            )}
        </Form>
    );
};

PaymentsForm.propTypes = {
    allowSame: bool,
    billingAddressSameAsShippingAddress: bool,
    cancel: func.isRequired,
    classes: shape({
        root: string
    }),
    countries: array,
    initialAddressSelectValue: number,
    initialPaymentMethod: shape({
        code: string
    }),
    initialValues: shape({
        firstname: string,
        lastname: string,
        telephone: string,
        city: string,
        postcode: string,
        region_code: string,
        street0: string
    }),
    onAddressSelectValueChange: func,
    paymentMethods: array.isRequired,
    showAddressSelect: bool,
    showEmailInput: bool,
    showSaveInAddressBookCheckbox: bool,
    submit: func.isRequired
};

PaymentsForm.defaultProps = {
    allowSame: true,
    billingAddressSameAsShippingAddress: true,
    initialAddressSelectValue: null,
    initialValues: {},
    showAddressSelect: false,
    showEmailInput: false,
    showSaveInAddressBookCheckbox: false
};

export default PaymentsForm;
