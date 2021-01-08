/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
const dataLayerEnabled = document.body.hasAttribute('data-cmp-data-layer-enabled');
const dataLayer = dataLayerEnabled ? (window.adobeDataLayer = window.adobeDataLayer || []) : undefined;

export const generateDataLayerId = async (prefix, idData, separator = "-") => {
    const msgUint8 = new TextEncoder().encode(idData);                            // encode as (utf-8) Uint8Array
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);           // hash the message
    const hashArray = Array.from(new Uint8Array(hashBuffer));                     // convert buffer to byte array
    const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join(''); // convert bytes to hex string
    return prefix + separator + hashHex.substring(0, 10);
}

// https://github.com/adobe/adobe-client-data-layer/wiki#push
export const pushData = data => {
    if (dataLayerEnabled) {
        dataLayer.push(data);
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#push
export const pushEvent = (eventName, eventData) => {
    if (dataLayerEnabled) {
        dataLayer.push({
            event: eventName,
            eventInfo: eventData
        });
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#getstate
export const getState = reference => {
    if (dataLayerEnabled) {
        return dataLayer.getState(reference);
    }
    return null;
};

// https://github.com/adobe/adobe-client-data-layer/wiki#addeventlistener
export const addEventListener = (eventName, handler) => {
    if (dataLayerEnabled) {
        dataLayer.addEventListener(eventName, handler);
    }
};

// https://github.com/adobe/adobe-client-data-layer/wiki#removeeventlistener
export const removeEventListener = (eventName, handler) => {
    if (dataLayerEnabled) {
        dataLayer.removeEventListener(eventName, handler);
    }
};
