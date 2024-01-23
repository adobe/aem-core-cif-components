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
import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useEventingContext } from '@magento/peregrine/lib/context/eventing';
import { useAwaitQuery } from '@magento/peregrine/lib/hooks/useAwaitQuery';
import { useUserContext } from '@magento/peregrine/lib/context/user';
import { default as handleEvent } from '@magento/experience-platform-connector/src/handleEvent';
import { default as callProcessors } from './processors';
import STORE_CONFIG_QUERY from './storeConfigQuery.gql';

export const EventCollectorContext = React.createContext();

export const EventCollectorContextProvider = props => {
    const { aep = null, acds = false } = props;
    const [sdk, setSdk] = useState(null);
    const [{ isSignedIn, currentUser }] = useUserContext();
    const [observable] = useEventingContext();
    const storeConfigQuery = useAwaitQuery(STORE_CONFIG_QUERY);

    useEffect(() => {
        import('@adobe/magento-storefront-events-sdk').then(mse => {
            if (!window.magentoStorefrontEvents) {
                window.magentoStorefrontEvents = mse;
            }

            mse.context.setEventForwarding({
                aep: aep !== null,
                commerce: acds
            });

            if (aep) {
                mse.context.setAEP({
                    imsOrgId: aep.orgId,
                    datastreamId: aep.datastreamId
                });
            }

            // load the collector after the sdk to make sure that
            // the AEP context is set before the connector initializes
            import('@adobe/magento-storefront-event-collector').then(() => setSdk(mse));

            // initialise the storefrontInstance context with some basic data that do not
            // depend on the dataServiceStorefrontInstanceContext query
            storeConfigQuery().then(({ data }) =>
                mse.context.setStorefrontInstance({
                    storeViewCurrencyCode: data.storeConfig.base_currency_code,
                    baseCurrencyCode: data.storeConfig.base_currency_code
                })
            );
        });
    }, []);

    useEffect(() => {
        if (sdk) {
            callProcessors(sdk);

            const sub = observable.subscribe(async event => {
                handleEvent(sdk, event);
            });

            return () => {
                sub.unsubscribe();
            };
        }
    }, [sdk, observable]);

    // Sets shopper context on initial load (when shopper context is null)
    useEffect(() => {
        if (sdk && !sdk.context.getShopper()) {
            if (isSignedIn) {
                sdk.context.setShopper({
                    shopperId: 'logged-in'
                });

                sdk.context.setAccount({
                    firstName: currentUser.firstname,
                    lastName: currentUser.lastname,
                    emailAddress: currentUser.email,
                    accountType: currentUser.__typename
                });
            } else {
                sdk.context.setShopper({
                    shopperId: 'guest'
                });
            }
        }
    }, [sdk, isSignedIn, currentUser]);

    const contextValue = [{ sdk }, {}];

    return <EventCollectorContext.Provider value={contextValue}>{props.children}</EventCollectorContext.Provider>;
};

EventCollectorContextProvider.propTypes = {
    acds: PropTypes.bool,
    aep: PropTypes.shape({
        orgId: PropTypes.string.isRequired,
        datastreamId: PropTypes.string.isRequired
    })
};

export const useEventCollectorContext = () => useContext(EventCollectorContext);
