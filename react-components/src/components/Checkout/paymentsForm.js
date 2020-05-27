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
import { Form, useFormApi, useFormState } from 'informed';
import { array, bool, shape, string, func } from 'prop-types';
import { useTranslation } from 'react-i18next';

import Button from '../Button';
import Select from '../Select';
import Checkbox from '../Checkbox';
import Field from '../Field';
import TextInput from '../TextInput';
import PaymentProvider from './paymentProviders/paymentProvider';
import { useCheckoutState } from './checkoutContext';

import classes from './paymentsForm.css';
import { isRequired, hasLengthExactly, validateRegionCode, validatePhoneUS, validateZip, validateEmail } from '../../utils/formValidators';
import combine from '../../utils/combineValidators';

/**
 * A wrapper around the payment form. This component's purpose is to maintain
 * the submission state as well as prepare/set initial values.
 */
const PaymentsForm = props => {
    const { initialPaymentMethod, initialValues, paymentMethods, cancel, countries, submit, allowSame } = props;
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [t] = useTranslation(['checkout', 'common']);

    const [{ anetToken, anetApiId }, dispatch] = useCheckoutState();
    const formState = useFormState();

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
    if (allowSame && (!initialValues || initialValues.sameAsShippingAddress)) {
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
        delete initialFormValues.sameAsShippingAddress;
    }

    const [differentAddress, setDifferentAddress] = useState(!initialFormValues.addresses_same);

    const handleSubmit = useCallback(
        formValues => {
            console.log("handle payment form submit");
            setIsSubmitting(true);
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
                    street: [formValues['street0']],
                    telephone: formValues['telephone']
                };
            } else {
                billingAddress = {
                    sameAsShippingAddress
                };
            }
            submit({
                paymentMethod: paymentMethods.find(v => v.code === formValues['payment_method']),
                paymentNonce: formValues['payment_nonce'],
                opaqueDataDescriptor: formValues['dataDescriptor'],
                ccLast4: formValues['ccLast4'],
                ccType: formValues['ccType'],
                ccExpYear: formValues['expYear'],
                ccExpMonth: formValues['expMonth'],
                ccCid: formValues['cardCode'],
                billingAddress
            });
        },
        [setIsSubmitting, paymentMethod]
    );

    const billingAddressFields = differentAddress ? (
        <>
            <div className={classes.firstname}>
                <Field label={t('checkout:address-firstname', 'First Name')}>
                    <TextInput id={classes.firstname} field="firstname" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.lastname}>
                <Field label={t('checkout:address-lastname', 'Last Name')}>
                    <TextInput id={classes.lastname} field="lastname" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.email}>
                <Field label={t('checkout:address-email', 'E-Mail')}>
                    <TextInput id={classes.email} field="email" validate={combine([isRequired, validateEmail])} />
                </Field>
            </div>
            <div className={classes.street0}>
                <Field label={t('checkout:address-street', 'Street')}>
                    <TextInput id={classes.street0} field="street0" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.city}>
                <Field label={t('checkout:address-city', 'City')}>
                    <TextInput id={classes.city} field="city" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.region_code}>
                <Field label={t('checkout:address-state', 'State')}>
                    <TextInput
                        id={classes.region_code}
                        field="region_code"
                        validate={combine([isRequired, [hasLengthExactly, 2], [validateRegionCode, countries]])}
                    />
                </Field>
            </div>
            <div className={classes.postcode}>
                <Field label={t('checkout:address-postcode', 'ZIP')}>
                    <TextInput id={classes.postcode} field="postcode" validate={combine([isRequired, validateZip])} />
                </Field>
            </div>
            <div className={classes.telephone}>
                <Field label={t('checkout:address-phone', 'Phone')}>
                    <TextInput id={classes.telephone} field="telephone" validate={combine([isRequired, validatePhoneUS])} />
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

    function secureData(formApi) {
        var authData = {};
        // TODO figure out better way to store... env variables?
        authData.clientKey = anetToken;
        authData.apiLoginID = anetApiId;
        var cardData = {};
        cardData.cardNumber = formApi.getValue('cardNumber');
        cardData.month = formApi.getValue('expMonth');
        cardData.year = formApi.getValue('expYear');
        cardData.cardCode = formApi.getValue('cardCode');
        var secureData = {};
        secureData.authData = authData;
        secureData.cardData = cardData;

        return secureData;
    }

    function anetData(formApi, e) {
        if (formApi.getValue('payment_method') == "authnetcim") {
            e.preventDefault();
            console.log("anet on click");

            submitPayment(secureData(formApi))
                .then(response => {
                    console.log("payment form response", response);
                    if (response.messages.resultCode == 'Error') {
                        var i = 0;
                        while (i < response.messages.message.length) {
                            console.log(
                                response.messages.message[i].code + ": " +
                                response.messages.message[i].text
                            );

                            // only show the user the first error message
                            if (i == 0) {
                                var error = response.messages.message[i].text;
                                console.error("Error", error);
                                formApi.setValue('anetError', error);
                            }
                            i = i + 1;
                        }
                        formApi.submitForm();
                        return;
                    }
                    // get anet submit data
                    formApi.setValue('payment_nonce', response.opaqueData.dataValue);
                    formApi.setValue('dataDescriptor', response.opaqueData.dataDescriptor);
                    let ccNumber = formApi.getValue('cardNumber');
                    formApi.setValue('ccLast4', parseInt(ccNumber.slice(-4)));
                    formApi.setValue('ccType', getCcType(ccNumber));
                    formApi.submitForm();
                    return;
                })
                .catch(error => {
                    console.error(error);
                    formApi.validate();
                })
        }
        else {
            formApi.submitForm();
            return;
        }

    }

    function submitPayment(data) {
        return new Promise(response => {
            Accept.dispatchData(data, response);
        })
    }
    function getCcType(ccNumber) {
        // the regular expressions check for possible matches as you type, hence the OR operators based on the number of chars
        // regexp string length {0} provided for soonest detection of beginning of the card numbers this way it could be used for BIN CODE detection also

        //JCB
        let jcb_regex = new RegExp('^(?:2131|1800|35)[0-9]{0,}$'); //2131, 1800, 35 (3528-3589)
        // American Express
        let amex_regex = new RegExp('^3[47][0-9]{0,}$'); //34, 37
        // Diners Club
        let diners_regex = new RegExp('^3(?:0[0-59]{1}|[689])[0-9]{0,}$'); //300-305, 309, 36, 38-39
        // Visa
        let visa_regex = new RegExp('^4[0-9]{0,}$'); //4
        // MasterCard
        let mastercard_regex = new RegExp('^(5[1-5]|222[1-9]|22[3-9]|2[3-6]|27[01]|2720)[0-9]{0,}$'); //2221-2720, 51-55
        let maestro_regex = new RegExp('^(5[06789]|6)[0-9]{0,}$'); //always growing in the range: 60-69, started with / not something else, but starting 5 must be encoded as mastercard anyway
        //Discover
        let discover_regex = new RegExp('^(6011|65|64[4-9]|62212[6-9]|6221[3-9]|622[2-8]|6229[01]|62292[0-5])[0-9]{0,}$');
        ////6011, 622126-622925, 644-649, 65


        // get rid of anything but numbers
        ccNumber = ccNumber.replace(/\D/g, '');

        // checks per each, as their could be multiple hits
        //fix: ordering matter in detection, otherwise can give false results in rare cases
        var ccType = "unknown";
        if (ccNumber.match(jcb_regex)) {
            ccType = "JCB";
        } else if (ccNumber.match(amex_regex)) {
            ccType = "AE";
        } else if (ccNumber.match(diners_regex)) {
            ccType = "DN";
        } else if (ccNumber.match(visa_regex)) {
            ccType = "VI";
        } else if (ccNumber.match(mastercard_regex)) {
            ccType = "MC";
        } else if (ccNumber.match(discover_regex)) {
            ccType = "DI";
        } else if (ccNumber.match(maestro_regex)) {
            if (ccNumber[0] == '5') { //started 5 must be mastercard
                ccType = "MC";
            } else {
                ccType = "MC"; //maestro is all 60-69 which is not something else, thats why this condition in the end, not in magento defaulting to MC 
            }
        }

        return ccType;
    }

    const ComponentUsingFieldApi = () => {
        const formApi = useFormApi();
        return (
            <Button onClick={(e) => anetData(formApi, e)} priority="high" type="submit" disabled={isSubmitting}>
                {t('checkout:use-payment-method', 'Use Payment Method')}
            </Button>
        );
    };

    return (
        <Form className={classes.root} initialValues={initialFormValues} onSubmit={handleSubmit} id="paymentForm">
            <div className={classes.body}>
                <h2 className={classes.heading}>Billing Information</h2>
                <div className={classes.braintree}>
                    <Select items={paymentMethodsItems} field="payment_method" initialValue={paymentMethod} />
                </div>
                <PaymentProvider />
                <div className={classes.address_check}>
                    {allowSame && (
                        <Checkbox
                            field="addresses_same"
                            label={t('checkout:same-as-shipping', 'Billing address same as shipping address')}
                            onClick={ev => {
                                setDifferentAddress(!ev.target.checked);
                            }}
                        />
                    )}
                </div>
                {billingAddressFields}
            </div>
            <div className={classes.footer}>
                <Button onClick={cancel}>{t('common:cancel', 'Cancel')}</Button>
                <ComponentUsingFieldApi />
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
        street0: string
    }),
    allowSame: bool,
    cancel: func.isRequired,
    submit: func.isRequired,
    initialPaymentMethod: shape({
        code: string
    }),
    paymentMethods: array.isRequired,
    countries: array
};

PaymentsForm.defaultProps = {
    initialValues: {},
    allowSame: true
};

export default PaymentsForm;
