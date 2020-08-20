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

/* eslint-disable react/prop-types */

import React from 'react';
import useNavigationState from '../useNavigationState';
import { render, fireEvent, wait } from '@testing-library/react';

describe('useNavigationState', () => {
    const GenericComponent = ({ view }) => {
        const [currentView, { switchTo }] = useNavigationState();
        return (
            <div>
                <div data-testid="current">{currentView}</div>
                <button onClick={() => switchTo(view)}></button>
            </div>
        );
    };

    const ComponentWithBackNavigation = ({ view }) => {
        const [currentView, { handleBack }] = useNavigationState({ view });
        return (
            <div>
                <div data-testid="current">{currentView}</div>
                <button onClick={handleBack}></button>
            </div>
        );
    };

    it('renders the default view', () => {
        const Component = () => {
            const [currentView] = useNavigationState();

            return <div>{currentView}</div>;
        };

        const { getByText } = render(<Component />);

        expect(getByText('MENU')).not.toBeUndefined();
    });

    it('renders the Sign In view', async () => {
        const { getByTestId, getByRole } = render(<GenericComponent view={'SIGN_IN'} />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            const element = getByTestId('current');
            expect(element.textContent).toEqual('SIGN_IN');
        });
    });

    it('renders a generic view view', async () => {
        const { getByTestId, getByRole } = render(<GenericComponent view={'MY_ACCOUNT'} />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            const element = getByTestId('current');
            expect(element.textContent).toEqual('MY_ACCOUNT');
        });
    });

    it('goes back to the "MENU" view', async () => {
        const { getByTestId, getByRole } = render(<ComponentWithBackNavigation view={'SIGN_IN'} />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            const element = getByTestId('current');
            expect(element.textContent).toEqual('MENU');
        });
    });

    it('goes back to the previous view', async () => {
        const { getByTestId, getByRole } = render(<ComponentWithBackNavigation view={'FORGOT_PASSWORD'} />);

        fireEvent.click(getByRole('button'));
        await wait(() => {
            const element = getByTestId('current');
            expect(element.textContent).toEqual('SIGN_IN');
        });
    });
});
