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
import React, { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import { useFormState } from 'informed';
import { array, bool, func, shape, string } from 'prop-types';
import { useTranslation } from 'react-i18next';

import BraintreeDropin from './braintreeDropin';
import Button from '../Button';
import Checkbox from '../Checkbox';
import Field from '../Field';
import TextInput from '../TextInput';
import { isRequired, hasLengthExactly, validateRegionCode, validateEmail } from '../../util/formValidators';
import combine from '../../util/combineValidators';

/**
 * This component is meant to be nested within an `informed` form. It utilizes
 * form state to do conditional rendering and submission.
 */
const PaymentsFormItems = props => {
    const [isReady, setIsReady] = useState(false);
    const [t] = useTranslation(['cart', 'common']);

    const { cancel, classes, countries, isSubmitting, setIsSubmitting, submit: submitPaymentData } = props;

    // Currently form state toggles dirty from false to true because of how
    // informed is implemented. This effectively causes this child components
    // to re-render multiple times. Keep tabs on the following issue:
    //   https://github.com/joepuzzo/informed/issues/138
    // If they resolve it or we move away from informed we can probably get some
    // extra performance.
    const formState = useFormState();
    const anchorRef = useRef(null);
    const addressDiffers = formState.values.addresses_same === false;

    const billingAddressFields = addressDiffers ? (
        <Fragment>
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
                    <TextInput id={classes.street0} field="street[0]" validate={isRequired} />
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
                    <TextInput id={classes.postcode} field="postcode" validate={isRequired} />
                </Field>
            </div>
            <div className={classes.telephone}>
                <Field label={t('checkout:address-phone', 'Phone')}>
                    <TextInput id={classes.telephone} field="telephone" validate={isRequired} />
                </Field>
            </div>
            <span ref={anchorRef} />
        </Fragment>
    ) : null;

    const handleError = useCallback(() => {
        setIsSubmitting(false);
    }, [setIsSubmitting]);

    // The success callback. Unfortunately since form state is created first and
    // then modified when using initialValues any component who uses this
    // callback will be rendered multiple times on first render. See above
    // comments for more info.
    const handleSuccess = useCallback(
        value => {
            setIsSubmitting(false);
            const sameAsShippingAddress = formState.values['addresses_same'];
            let billingAddress;
            if (!sameAsShippingAddress) {
                billingAddress = {
                    city: formState.values['city'],
                    email: formState.values['email'],
                    firstname: formState.values['firstname'],
                    lastname: formState.values['lastname'],
                    postcode: formState.values['postcode'],
                    region_code: formState.values['region_code'],
                    street: formState.values['street'],
                    telephone: formState.values['telephone']
                };
            } else {
                billingAddress = {
                    sameAsShippingAddress
                };
            }
            submitPaymentData({
                billingAddress,
                paymentMethod: {
                    code: 'braintree',
                    data: value
                }
            });
        },
        [formState.values, setIsSubmitting, submitPaymentData]
    );

    // When the address checkbox is unchecked, additional fields are rendered.
    // This causes the form to grow, and potentially to overflow, so the new
    // fields may go unnoticed. To reveal them, we scroll them into view.
    useEffect(() => {
        if (addressDiffers) {
            const { current: element } = anchorRef;

            if (element instanceof HTMLElement) {
                element.scrollIntoView({ behavior: 'smooth' });
            }
        }
    }, [addressDiffers]);

    return (
        <Fragment>
            <div className={classes.body}>
                <h2 className={classes.heading}>Billing Information</h2>
                <div className={classes.braintree}>
                    <BraintreeDropin
                        shouldRequestPaymentNonce={isSubmitting}
                        onError={handleError}
                        onSuccess={handleSuccess}
                        onReady={setIsReady}
                    />
                </div>
                <div className={classes.address_check}>
                    <Checkbox
                        field="addresses_same"
                        label={t('checkout:same-as-shipping', 'Billing address same as shipping address')}
                    />
                </div>
                {billingAddressFields}
            </div>
            <div className={classes.footer}>
                <Button className={classes.button} onClick={cancel}>
                    {t('common:cancel', 'Cancel')}
                </Button>
                <Button className={classes.button} priority="high" type="submit" disabled={!isReady || isSubmitting}>
                    {t('checkout:use-card', 'Use Card')}
                </Button>
            </div>
        </Fragment>
    );
};

PaymentsFormItems.propTypes = {
    cancel: func.isRequired,
    classes: shape({
        address_check: string,
        body: string,
        button: string,
        braintree: string,
        firstname: string,
        lastname: string,
        telephone: string,
        city: string,
        footer: string,
        heading: string,
        postcode: string,
        region_code: string,
        street0: string
    }),
    countries: array,
    isSubmitting: bool,
    setIsSubmitting: func.isRequired,
    submit: func.isRequired
};

export default PaymentsFormItems;
