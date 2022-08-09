/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
const canHandle = event => event.type === 'CHECKOUT_PLACE_ORDER_BUTTON_CLICKED';

const handle = (sdk, event) => {
    const { payload } = event;

    const grandTotal = payload.amount.grand_total.value;

    const { payment, shipping } = payload;

    const orderContext = {
        grandTotal: grandTotal,
        orderType: 'checkout',
        payments: [
            {
                paymentMethodCode: payment.title,
                paymentMethodName: payment.title,
                total: grandTotal
            }
        ],
        shipping: {
            shippingMethod: shipping[0].method_title,
            shippingAmount: shipping[0].amount.value
        }
    };

    sdk.context.setOrder(orderContext);
};

export default {
    canHandle,
    handle
};
