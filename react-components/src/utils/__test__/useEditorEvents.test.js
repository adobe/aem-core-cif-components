/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
import React, { useRef } from 'react';
import { render, act } from '@testing-library/react';

const mockUseCookieValue = jest.fn();
jest.mock('../hooks', () => ({
    useCookieValue: () => mockUseCookieValue()
}));

import useEditorEvents from '../useEditorEvents';

describe('useEditorEvents', () => {
    const MockComponent = () => {
        useEditorEvents();
        const counter = useRef(0);

        // Increment render counter
        counter.current = counter.current + 1;

        return <div data-testid="counter">{counter.current}</div>;
    };

    it('re-renders the component after receiving an editor event in edit mode', () => {
        mockUseCookieValue.mockReturnValue(['edit']);

        const { getByTestId } = render(<MockComponent />);

        // Send editor event
        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: {
                    msg: 'cqauthor-cmd'
                },
                origin: window.location.origin
            });
            window.dispatchEvent(messageEvent);
        });

        expect(getByTestId('counter').textContent).toEqual('2');
    });

    it('does not re-render the component if not in edit mode', () => {
        mockUseCookieValue.mockReturnValue(['preview']);

        const { getByTestId } = render(<MockComponent />);

        // Send editor event
        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: {
                    msg: 'cqauthor-cmd'
                },
                origin: window.location.origin
            });
            window.dispatchEvent(messageEvent);
        });

        expect(getByTestId('counter').textContent).toEqual('1');
    });

    it('does not re-render the component for events with wrong origin', () => {
        mockUseCookieValue.mockReturnValue(['edit']);

        const { getByTestId } = render(<MockComponent />);

        // Send editor event
        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: {
                    msg: 'cqauthor-cmd'
                },
                origin: 'http://some-malicious-website.com'
            });
            window.dispatchEvent(messageEvent);
        });

        expect(getByTestId('counter').textContent).toEqual('1');
    });

    it('does not re-render the component for non editor events', () => {
        mockUseCookieValue.mockReturnValue(['edit']);

        const { getByTestId } = render(<MockComponent />);

        // Send editor event
        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: {
                    product: 'sku'
                },
                origin: window.location.origin
            });
            window.dispatchEvent(messageEvent);
        });

        expect(getByTestId('counter').textContent).toEqual('1');
    });

    it('does not re-render the component for editor toggleClass events', () => {
        mockUseCookieValue.mockReturnValue(['edit']);

        const { getByTestId } = render(<MockComponent />);

        // Send editor event
        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: {
                    msg: 'cqauthor-cmd',
                    data: {
                        cmd: 'toggleClass'
                    }
                },
                origin: window.location.origin
            });
            window.dispatchEvent(messageEvent);
        });

        expect(getByTestId('counter').textContent).toEqual('1');
    });
});
