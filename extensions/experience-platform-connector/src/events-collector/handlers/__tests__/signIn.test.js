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
import handler from '../signIn';

describe('canHandle()', () => {
    it('returns true for the correct event type', () => {
        const mockEvent = {
            type: 'USER_SIGN_IN',
            payload: {}
        };
        expect(handler.canHandle(mockEvent)).toBeTruthy();
    });

    it('returns false for non supported event types', () => {
        const mockEvent = {
            type: 'USER_SIGN_OUT',
            payload: {}
        };
        expect(handler.canHandle(mockEvent)).toBeFalsy();
    });
});

describe('handle()', () => {
    it('calls the correct sdk functions with the correct context value', () => {
        const mockSdk = {
            context: {
                setAccount: jest.fn(),
                setShopper: jest.fn()
            },
            publish: {
                signIn: jest.fn()
            }
        };

        const mockEvent = {
            type: 'USER_SIGN_IN',
            payload: {
                email: 'doctor.strange@fake.email',
                firstname: 'Stephen',
                is_subscribed: false,
                lastname: 'Strange',
                __typename: 'Customer'
            }
        };

        handler.handle(mockSdk, mockEvent);

        expect(mockSdk.context.setShopper).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setShopper.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "shopperId": "logged-in",
            }
        `);

        expect(mockSdk.context.setAccount).toHaveBeenCalledTimes(1);
        expect(mockSdk.context.setAccount.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "emailAddress": "doctor.strange@fake.email",
              "firstName": "Stephen",
              "lastName": "Strange",
            }
        `);

        expect(mockSdk.publish.signIn).toHaveBeenCalledTimes(1);
        expect(mockSdk.publish.signIn.mock.calls[0][0]).toMatchInlineSnapshot(`
            Object {
              "personalEmail": Object {
                "address": "doctor.strange@fake.email",
              },
            }
        `);
    });
});
