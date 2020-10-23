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
import React from 'react';
import { fireEvent } from '@testing-library/react';
import { render } from '../../../utils/test-utils';
import Dialog from '../dialog';

jest.mock('../../Portal', () => ({
    // eslint-disable-next-line react/display-name
    Portal: props => <portal-mock>{props.children}</portal-mock>
}));

const CONTAINER_ID = 'root';

describe('<Dialog>', () => {
    beforeEach(() => {
        const container = document.createElement('div');
        container.setAttribute('id', CONTAINER_ID);
        document.body.appendChild(container);
    });

    afterEach(() => {
        const container = document.getElementById(CONTAINER_ID);
        container.parentElement.removeChild(container);
    });
    it('renders an empty dialog', () => {
        const { asFragment } = render(<Dialog rootContainerSelector={`#${CONTAINER_ID}`} />);

        expect(asFragment()).toMatchSnapshot();
    });

    it('renders the dialog with disabled buttons', () => {
        const { asFragment } = render(
            <Dialog rootContainerSelector={`#${CONTAINER_ID}`} shouldDisableAllButtons={true} />
        );

        expect(asFragment()).toMatchSnapshot();
    });
    it('renders the dialog with disabled confirm button', () => {
        const { asFragment } = render(
            <Dialog rootContainerSelector={`#${CONTAINER_ID}`} shouldDisableConfirmButton={true} />
        );

        expect(asFragment()).toMatchSnapshot();
    });

    it('calls the "Confirm" handler', () => {
        const confirmFn = jest.fn();
        const { container } = render(
            <Dialog rootContainerSelector={`#${CONTAINER_ID}`} confirmText={'Save'} onConfirm={confirmFn} />
        );

        let submit = container.querySelector('button[type=submit]');
        fireEvent.click(submit);

        expect(confirmFn).toHaveBeenCalled();
    });

    it('calls the "Cancel" handler', () => {
        const cancelFn = jest.fn();
        const { container } = render(
            <Dialog rootContainerSelector={`#${CONTAINER_ID}`} confirmText={'Save'} onCancel={cancelFn} />
        );

        let reset = container.querySelector('button[type=reset]');
        fireEvent.click(reset);

        expect(cancelFn).toHaveBeenCalled();
    });
});
