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
import React, { useCallback, useContext, useReducer, Suspense } from 'react';
import { object } from 'prop-types';
import { useTranslation } from 'react-i18next';

import LoadingIndicator from '../components/LoadingIndicator';
import { useEventListener } from '../utils/hooks';
import * as NavigationActions from '../actions/navigation';

const ancestors = {
    CREATE_ACCOUNT: 'SIGN_IN',
    FORGOT_PASSWORD: 'SIGN_IN',
    ACCOUNT_CREATED: 'MENU',
    MY_ACCOUNT: 'MENU',
    CHANGE_PASSWORD: 'MY_ACCOUNT',
    CUSTOMER_ORDERS: 'MY_ACCOUNT',
    SIGN_IN: 'MENU',
    MENU: null
};

const NavigationContext = React.createContext();

const reducerFactory = () => {
    return (state, action) => {
        switch (action.type) {
            case 'changeView': {
                return {
                    ...state,
                    view: action.view
                };
            }
            default:
                return state;
        }
    };
};

const NavigationContextProvider = props => {
    const initialState = props.initialState || {
        view: 'MENU'
    };

    const [navigationState, dispatch] = useReducer(reducerFactory(), initialState);
    const { view } = navigationState;
    const [t] = useTranslation('account');

    const showSignIn = () => NavigationActions.showSignIn({ dispatch, t });

    const showMenu = () => NavigationActions.showMenu({ dispatch, t });

    const showMyAccount = () => NavigationActions.showMyAccount({ dispatch, t });

    const showChangePassword = () => NavigationActions.showChangePassword({ dispatch, t });

    const showCustomerOrders = () => NavigationActions.showCustomerOrders({ dispatch, t });

    const showForgotPassword = () => NavigationActions.showForgotPassword({ dispatch, t });

    const showCreateAccount = () => NavigationActions.showCreateAccount({ dispatch, t });

    const showAccountCreated = () => NavigationActions.showAccountCreated({ dispatch, t });

    const handleBack = useCallback(() => {
        if (navigationState.view === null) {
            return;
        }
        const parent = ancestors[view];
        if (parent === 'MENU') {
            // no parent view means we're out of the account management process and back to navigation
            // so we're resetting the title
            showMenu();
            return;
        }
        if (parent === 'MY_ACCOUNT') {
            showMyAccount();
            return;
        }
        dispatch({ type: 'changeView', view: parent });
    }, [view]);

    useEventListener(document, 'aem.navigation.back', handleBack);

    const { children } = props;
    const contextValue = [
        navigationState,
        {
            dispatch,
            showSignIn,
            showMenu,
            showMyAccount,
            showChangePassword,
            showCustomerOrders,
            showForgotPassword,
            showCreateAccount,
            showAccountCreated
        }
    ];
    return <NavigationContext.Provider value={contextValue}>{children}</NavigationContext.Provider>;
};

NavigationContextProvider.propTypes = {
    initialState: object
};

const withSuspense = NavigationContextProvider => {
    let WithSuspense = props => {
        return (
            <Suspense fallback={<LoadingIndicator />}>
                <NavigationContextProvider {...props} />
            </Suspense>
        );
    };
    WithSuspense.displayName = `withSuspense(${NavigationContextProvider.displayName ||
        NavigationContextProvider.name})`;
    return WithSuspense;
};

export default withSuspense(NavigationContextProvider);

export const useNavigationContext = () => useContext(NavigationContext);
