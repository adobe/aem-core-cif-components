import { useEventingContext } from '@magento/peregrine/lib/context/eventing';
import { useUserContext } from '@magento/peregrine/lib/context/user';
import { useEffect, useState } from 'react';
import { default as handleEvent } from './handleEvent';

export default ({ aep = null, snowPlow = null }) => {
    const [{ isSignedIn, currentUser }] = useUserContext();
    const [observable] = useEventingContext();

    const [sdk, setSdk] = useState();

    useEffect(() => {
        import('@adobe/magento-storefront-events-sdk').then(mse => {
            if (!window.magentoStorefrontEvents) {
                window.magentoStorefrontEvents = mse;
            }

            const event = new CustomEvent('mse-loaded');
            document.dispatchEvent(event);

            mse.context.setEventForwarding({
                aep: aep !== null,
                commerce: snowPlow !== null
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
