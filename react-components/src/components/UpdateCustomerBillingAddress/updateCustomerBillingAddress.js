/*******************************************************************************
 *
 *    Sagepath custom cif component
 *
 ******************************************************************************/
import React from 'react';

import { useUserContext } from '../../context/UserContext';
import { useMutation } from '@apollo/react-hooks';

import { useCountries } from '../../utils/hooks';

import { func } from 'prop-types';
import { Form } from 'informed';

import Field from '../Field';
import TextInput from '../TextInput';
import Button from '../Button';
import combine from '../../utils/combineValidators';
import { isRequired, hasLengthExactly, validatePhoneUS, validateZip, validateRegionCode } from '../../utils/formValidators';
import parseError from '../../utils/parseError';
import LoadingIndicator from '../LoadingIndicator';

import MUTATION_UPDATE_CUSTOMER_ADDRESS from '../../queries/mutation_update_customer_address.graphql';
import MUTATION_CREATE_CUSTOMER_ADDRESS from '../../queries/mutation_create_customer_address.graphql';

import classes from './updateCustomerBillingAddress.css';

const UpdateCustomerBillingAddress = props => {
    const { showMyAccount } = props;
    const [{ currentUser, token }, { getUserDetails }] = useUserContext();
    const { error: countriesError, countries } = useCountries();
    // TODO move this out and pass down? need to handle error either way
    const address = currentUser.addresses.find(isDefaultBilling);
    const id = address ? address.id : 0;
    // If customer doesn't have default billing address, create instead of update
    const [doUpdateCustomerBillingAddress, { data, loading, error }] = useMutation(address ? MUTATION_UPDATE_CUSTOMER_ADDRESS : MUTATION_CREATE_CUSTOMER_ADDRESS);

    const handleSubmit = async (formValues) => {
        let targetCountry = countries.find(obj => obj.id == "US");
        let region = targetCountry.available_regions.find(obj => obj.code === formValues.region_code);
        // 1. update customer billing address
        doUpdateCustomerBillingAddress({
            variables: {
                id: id,
                city: formValues.city,
                company: formValues.company,
                country_code: "US",
                firstname: formValues.firstname,
                lastname: formValues.lastname,
                postcode: formValues.postcode,
                region: region.name,
                region_code: formValues.region_code,
                region_id: region.id,
                street: formValues.street0,
                telephone: formValues.telephone,
                default_billing: true,
                default_shipping: false
            },
            context: {
                headers: {
                    authorization: `Bearer ${token && token.length > 0 ? token : ''}`
                }
            }
        });

    };


    if (loading) {
        return (
            <div>
                <LoadingIndicator>Loading</LoadingIndicator>
            </div>
        );
    }

    if (data) {
        // 2. refresh user details
        getUserDetails();

        return (
            <div className="updateAddress">
                <p>Your billing address was changed.</p>
                <div>
                    <Button priority="high" onClick={showMyAccount}>
                        {'Back'}
                    </Button>
                </div>
            </div>
        );
    }

    if (!data) {
        return (
            <div>
                <Form className={classes.root} onSubmit={handleSubmit}>
                    <Field label="First Name" required={true}>
                        <TextInput
                            field="firstname"
                            type="text"
                            validate={combine([isRequired])}
                            validateOnBlur
                            aria-label="firstname"
                            initialValue={`${!address ? '' : address.firstname ? address.firstname : ''}`}
                        />
                    </Field>
                    <Field label="Last Name" required={true}>
                        <TextInput
                            field="lastname"
                            type="text"
                            validate={combine([isRequired])}
                            validateOnBlur
                            aria-label="lastname"
                            initialValue={`${!address ? '' : address.lastname ? address.lastname : ''}`}
                        />
                    </Field>
                    <Field label="Company" required={true}>
                        <TextInput
                            field="company"
                            type="text"
                            validate={combine([isRequired])}
                            validateOnBlur
                            aria-label="company"
                            initialValue={`${!address ? '' : address.company ? address.company : ''}`}
                        />
                    </Field>
                    <Field label="Phone" required={true}>
                        <TextInput
                            id="phone"
                            field="telephone"
                            validateOnBlur
                            validate={combine([isRequired, validatePhoneUS])}
                            initialValue={`${!address ? '' : address.telephone ? address.telephone : ''}`}
                        />
                    </Field>
                    <Field label="Street Address" required={true}>
                        <TextInput
                            id="address"
                            field="street0"
                            validateOnBlur
                            validate={isRequired}
                            initialValue={`${!address ? '' : address.street ? address.street : ''}`}
                        />
                    </Field>
                    <Field label="City" required={true}>
                        <TextInput
                            id="city"
                            field="city"
                            validateOnBlur
                            validate={isRequired}
                            initialValue={`${!address ? '' : address.city ? address.city : ''}`}
                        />
                    </Field>
                    <Field label="State" required={true}>
                        <TextInput
                            id="state"
                            field="region_code"
                            validateOnBlur
                            validate={combine([isRequired, [hasLengthExactly, 2], [validateRegionCode, countries]])}
                            initialValue={`${!address ? '' : address.region.region_code ? address.region.region_code : ''}`}
                        />
                    </Field>
                    <Field label="ZIP" required={true}>
                        <TextInput
                            id="zip"
                            field="postcode"
                            validateOnBlur
                            validate={combine([isRequired, validateZip])}
                            initialValue={`${!address ? '' : address.postcode ? address.postcode : ''}`}
                        />
                    </Field>
                    {error && <div>{parseError(error)}</div>}
                    <div>
                        <Button className={classes.buttonContainer} type="submit" priority="high" aria-label="submit">
                            {'Change Address'}
                        </Button>
                    </div>
                </Form>
            </div>
        );
    }
}

function isDefaultBilling(addresses) {
    return addresses.default_billing === true;
}

UpdateCustomerBillingAddress.propTypes = {
    showMyAccount: func.isRequired
};

export default UpdateCustomerBillingAddress;