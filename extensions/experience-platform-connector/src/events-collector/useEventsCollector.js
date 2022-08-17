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
import { useEventingContext } from '@magento/peregrine/lib/context/eventing';
import { useUserContext } from '@magento/peregrine/lib/context/user';
import { useEffect, useState } from 'react';
import { default as handleEvent } from '@magento/experience-platform-connector/src/handleEvent';

export default ({ aep = null, acds = false }) => {
    const [{ isSignedIn, currentUser }] = useUserContext();
    const [observable] = useEventingContext();

    const [sdk, setSdk] = useState();

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

            import('@adobe/magento-storefront-event-collector').then(msec => {
                msec;
                setSdk(mse);
            });
        });
    }, []);

    useEffect(() => {
        if (sdk) {
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
};
