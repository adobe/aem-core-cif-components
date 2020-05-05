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

import React from 'react';
import { MockedProvider } from '@apollo/react-testing';
import { render, waitForElement, fireEvent } from '@testing-library/react';

import MUTATION_CREATE_CART from '../../../queries/mutation_create_guest_cart.graphql';
import MUTATION_GENERATE_TOKEN from '../../../queries/mutation_generate_token.graphql';
import MUTATION_MERGE_CARTS from '../../../queries/mutation_merge_carts.graphql';
import QUERY_CUSTOMER_CART from '../../../queries/query_customer_cart.graphql';
import QUERY_CUSTOMER_DETAILS from '../../../queries/query_customer_details.graphql';

import { useCartState, CartProvider } from '../cartContext';
import CartInitializer from '../cartInitializer';
import UserContextProvider, { useUserContext } from '../../../context/UserContext';

const queryMocks = [
    {
        request: {
            query: MUTATION_GENERATE_TOKEN
        },
        result: {
            data: {
                generateCustomerToken: {
                    token: 'token123'
                }
            }
        }
    },
    {
        request: {
            query: QUERY_CUSTOMER_CART
        },
        result: {
            data: {
                customerCart: {
                    id: 'customercart'
                }
            }
        }
    },
    {
        request: {
            query: MUTATION_MERGE_CARTS,
            variables: {
                sourceCartId: 'guest123',
                destinationCartId: 'customercart'
            }
        },
        result: {
            data: {
                mergeCarts: {
                    id: 'customercart'
                }
            }
        }
    },
    {
        request: {
            query: QUERY_CUSTOMER_DETAILS
        },
        result: {
            data: {
                customer: {
                    firstname: 'Iris',
                    lastname: 'McCoy',
                    email: 'imccoy@weretail.net'
                }
            }
        }
    },
    {
        request: {
            query: MUTATION_CREATE_CART
        },
        result: {
            data: {
                createEmptyCart: 'guest123'
            }
        }
    }
];

const DummyCart = () => {
    const [{ cartId }] = useCartState();
    const [{ isSignedIn }, { signIn }] = useUserContext();
    if (!cartId || cartId.length === 0) {
        return <div>No cart</div>;
    }

    // we add this element to detect if the cart id has been updated.
    // This basically moves the assertion to the component
    let successElement = cartId === 'customercart' ? <div data-testid="success"></div> : '';
    return (
        <div>
            <div data-testid="cart-details">{cartId}</div>
            {successElement}
            {isSignedIn && <div data-testid="signed-in"></div>}
            <button
                data-testid="sign-in"
                onClick={() => {
                    signIn('', '');
                }}>
                Sign in
            </button>
        </div>
    );
};

describe('<CartInitializer />', () => {
    it('retrieves the cartId from the cookie', async () => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=cart-from-cookie;path=/;domain=http://localhost;Max-Age=3600'
        });

        const { getByTestId } = render(
            <MockedProvider mocks={[]} addTypename={false}>
                <UserContextProvider>
                    <CartProvider
                        initialState={{ cartId: null }}
                        reducerFactory={() => (state, action) => {
                            if (action.type == 'cartId') {
                                return { ...state, cartId: action.cartId };
                            }
                            return state;
                        }}>
                        <CartInitializer>
                            <DummyCart />
                        </CartInitializer>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );
        const cartIdNode = await waitForElement(() => getByTestId('cart-details'));

        expect(cartIdNode.textContent).toEqual('cart-from-cookie');
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: ''
        });
    });

    it('resets the cart id after a checkout', async () => {
        Object.defineProperty(window.document, 'cookie', {
            writable: true,
            value: 'cif.cart=oldcustomercart;path=/;domain=http://localhost;Max-Age=3600'
        });
        const ResetCartComponent = () => {
            const [{ cartId }, dispatch] = useCartState();

            if (!cartId || cartId.length === 0) {
                return <div data-testid="cart-id">none</div>;
            }
            return (
                <div>
                    <button
                        onClick={() => {
                            dispatch({ type: 'reset' });
                        }}>
                        Reset
                    </button>
                </div>
            );
        };

        const { getByTestId, getByRole } = render(
            <MockedProvider mocks={queryMocks} addTypename={false}>
                <UserContextProvider>
                    <CartProvider>
                        <CartInitializer>
                            <ResetCartComponent />
                        </CartInitializer>
                    </CartProvider>
                </UserContextProvider>
            </MockedProvider>
        );

        fireEvent.click(getByRole('button'));
        const cartIdNode = await waitForElement(() => {
            return getByTestId('cart-id');
        });
        expect(cartIdNode.textContent).toEqual('none');
    });
});
