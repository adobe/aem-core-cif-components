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
export default (value, values) => {
    const nonce = value;
    const paymentMethod = values.payment_method;
    switch (paymentMethod) {
        case 'braintree': {
            return !nonce || nonce.length < 11 ? 'Please provide your credit card details.' : undefined;
        }
        case 'braintree_paypal': {
            return !nonce || nonce.length < 11 ? 'Please provide your PayPal details.' : undefined;
        }
        default: {
            // No nonce needed for any other payment methods
            return undefined;
        }
    }
};
