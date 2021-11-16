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
import React, { useEffect, useState } from 'react';
import { defineMessages, useIntl } from 'react-intl';

import LoadingIndicator from '../LoadingIndicator';
import { useAwaitQuery } from '../../utils/hooks';
import GIFT_CARD_PRODUCT_QUERY from '../../queries/query_gift_card_product.graphql';
import { useConfigContext } from '../../context/ConfigContext';
import Price from '../Price';

const OPEN_AMOUNT = 'open_amount';

const messages = defineMessages({
    amountSelectionLabel: {
        id: 'product:gift-card-amount-selection',
        defaultMessage: 'Amount'
    },
    chooseAmount: {
        id: 'product:gift-card-choose-amount',
        defaultMessage: 'Choose an Amount'
    },
    otherAmount: {
        id: 'product:gift-card-other-amount',
        defaultMessage: 'Other Amount'
    },
    customAmountLabel: {
        id: 'product:gift-card-custom-amount',
        defaultMessage: 'Amount in'
    },
    customPriceMinimum: {
        id: 'product:gift-card-custom-ammount-minimum',
        defaultMessage: 'Minimum'
    },
    customPriceMaximum: {
        id: 'product:gift-card-custom-ammount-maximum',
        defaultMessage: 'Maximum'
    }
});

const GiftCartOptions = () => {
    const [giftCardState, setGiftCardState] = useState(null);
    const giftCardProductQuery = useAwaitQuery(GIFT_CARD_PRODUCT_QUERY);

    const { formatMessage } = useIntl();
    const {
        mountingPoints: { giftCardProductOptionsContainer }
    } = useConfigContext();
    const sku = document.querySelector(giftCardProductOptionsContainer)?.dataset?.sku;

    useEffect(() => {
        const fetchGiftCardOptions = async () => {
            if (!sku) {
                console.error('SKU is not present in the dataset of mountpoint element');
                return;
            }
            const { data, error } = await giftCardProductQuery({ variables: { sku }, fetchPolicy: 'network-only' });

            if (error) {
                throw new Error(error);
            }

            const giftCardOptions = data.products.items[0];
            const giftCardValues = {
                quantity: 1,
                open_amount: giftCardOptions.giftcard_amounts.length === 0,
                custom_amount: giftCardOptions.open_amount_min,
                custom_amount_uid: giftCardOptions.gift_card_options.find(
                    o => o.title.toLowerCase() === 'custom giftcard amount'
                )?.value.uid,
                selected_amount: '',
                entered_options: giftCardOptions.gift_card_options
                    .filter(o => o.title.toLowerCase() !== 'custom giftcard amount')
                    .reduce(
                        (prev, curr) => ({
                            ...prev,
                            [curr.value.uid]: ''
                        }),
                        {}
                    )
            };

            setGiftCardState({ giftCardOptions, giftCardValues });
        };

        fetchGiftCardOptions();
    }, []);

    const changeAmountSelection = e => {
        const { value } = e.target;
        setGiftCardState({
            ...giftCardState,
            giftCardValues: {
                ...giftCardState.giftCardValues,
                open_amount: value === OPEN_AMOUNT,
                selected_amount: value
            }
        });
    };

    const changeCustomAmount = e => {
        const { value } = e.target;
        setGiftCardState({
            ...giftCardState,
            giftCardValues: {
                ...giftCardState.giftCardValues,
                custom_amount: value
            }
        });
    };

    const changeOptionValue = (uid, value) => {
        setGiftCardState({
            ...giftCardState,
            giftCardValues: {
                ...giftCardState.giftCardValues,
                entered_options: {
                    ...giftCardState.giftCardValues.entered_options,
                    [uid]: value
                }
            }
        });
    };

    const changeQuantity = e => {
        setGiftCardState({
            ...giftCardState,
            giftCardValues: {
                ...giftCardState.giftCardValues,
                quantity: e.target.value
            }
        });
    };

    if (giftCardState === null) {
        return <LoadingIndicator />;
    }

    const {
        giftCardOptions: {
            giftcard_amounts,
            gift_card_options,
            allow_open_amount,
            open_amount_min,
            open_amount_max,
            price_range: {
                maximum_price: {
                    final_price: { currency }
                }
            }
        },
        giftCardValues: { quantity, open_amount, custom_amount, custom_amount_uid, selected_amount, entered_options }
    } = giftCardState;

    const canAddToCart = () => {
        if (open_amount) {
            if (
                Math.fround(open_amount_min) > Math.fround(custom_amount) ||
                Math.fround(custom_amount) > Math.fround(open_amount_max)
            ) {
                return false;
            }
        } else {
            if (selected_amount === '') {
                return false;
            }
        }

        for (const option of gift_card_options.filter(o => o.required)) {
            if (entered_options[option.value.uid].trim() === '') {
                return false;
            }
        }

        return true;
    };

    const addToCart = () => {
        const productData = {
            sku,
            virtual: false,
            giftCard: true,
            quantity: quantity,
            entered_options: Object.entries(entered_options).map(o => ({ uid: o[0], value: o[1] }))
        };

        if (open_amount) {
            productData.entered_options.push({ uid: custom_amount_uid, value: custom_amount + '' });
        } else {
            productData.selected_options = [selected_amount];
        }

        const customEvent = new CustomEvent('aem.cif.add-to-cart', {
            detail: [productData]
        });
        document.dispatchEvent(customEvent);
    };

    return (
        <>
            {giftcard_amounts.length > 0 && (
                <section className="productFullDetail__section productFullDetail__giftCardProduct">
                    <h3 className="option__title">
                        <span>{formatMessage(messages.amountSelectionLabel)}</span> <span className="required"> *</span>
                    </h3>
                    <div>
                        <div className="giftCardOptionSelect__root">
                            <span className="fieldIcons__root">
                                <span className="fieldIcons__input">
                                    <select
                                        aria-label={formatMessage(messages.amountSelectionLabel)}
                                        className="select__input field__input giftCardProduct__option"
                                        value={selected_amount}
                                        onChange={changeAmountSelection}>
                                        <option value="">{formatMessage(messages.chooseAmount)}...</option>
                                        {giftcard_amounts.map(o => (
                                            <option key={o.uid} value={o.uid}>
                                                {o.value}
                                            </option>
                                        ))}
                                        {allow_open_amount && (
                                            <option value={OPEN_AMOUNT}>
                                                {formatMessage(messages.otherAmount)}...
                                            </option>
                                        )}
                                    </select>
                                </span>
                                <span className="fieldIcons__before"></span>
                                <span className="fieldIcons__after">
                                    <span className="icon__root">
                                        <svg
                                            xmlns="http://www.w3.org/2000/svg"
                                            width="18"
                                            height="18"
                                            viewBox="0 0 24 24"
                                            fill="none"
                                            stroke="currentColor"
                                            strokeWidth="2"
                                            strokeLinecap="round"
                                            strokeLinejoin="round">
                                            <polyline points="6 9 12 15 18 9"></polyline>
                                        </svg>
                                    </span>
                                </span>
                            </span>
                        </div>
                    </div>
                </section>
            )}
            {open_amount && (
                <section className="productFullDetail__section productFullDetail__giftCardProduct">
                    <h3 className="option__title">
                        <span>
                            {formatMessage(messages.customAmountLabel)} {currency}
                        </span>{' '}
                        <span className="required"> *</span>
                    </h3>
                    <div>
                        <div className="giftCardOptionSelect__root">
                            <span>
                                <input
                                    type="number"
                                    aria-label={formatMessage(messages.customAmountLabel)}
                                    className="field__input"
                                    min={open_amount_min}
                                    max={open_amount_max}
                                    value={custom_amount}
                                    onChange={changeCustomAmount}
                                />
                            </span>
                            <span>
                                {formatMessage(messages.customPriceMinimum)}:{' '}
                                <Price currencyCode={currency} value={open_amount_min} />{' '}
                                {formatMessage(messages.customPriceMaximum)}:{' '}
                                <Price currencyCode={currency} value={open_amount_max} />
                            </span>
                        </div>
                    </div>
                </section>
            )}
            {gift_card_options
                .filter(o => o.title.toLowerCase() !== 'custom giftcard amount')
                .map(o => (
                    <section
                        key={o.value.uid}
                        className="productFullDetail__section productFullDetail__giftCardProduct">
                        <h3 className="option__title">
                            <span>{o.title}</span> {o.required && <span className="required"> *</span>}
                        </h3>
                        <div>
                            <div className="giftCardOptionSelect__root">
                                <span>
                                    <input
                                        type="text"
                                        aria-label={o.title}
                                        className="field__input"
                                        value={entered_options[o.value.uid]}
                                        onChange={e => changeOptionValue(o.value.uid, e.target.value)}
                                    />
                                </span>
                            </div>
                        </div>
                    </section>
                ))}
            <section className="productFullDetail__quantity productFullDetail__section">
                <h2 className="productFullDetail__quantityTitle option__title">
                    <span>{formatMessage({ id: 'cart:quantity', defaultMessage: 'Quantity' })}</span>
                </h2>
                <div className="quantity__root">
                    <span className="fieldIcons__root" style={{ '--iconsBefore': 0, '--iconsAfter': 1 }}>
                        <span className="fieldIcons__input">
                            <select
                                aria-label={formatMessage({
                                    id: 'product:quantity-label',
                                    defaultMessage: 'Product quantity'
                                })}
                                className="select__input field__input"
                                name="quantity"
                                value={quantity}
                                onChange={changeQuantity}>
                                <option value="1">1</option>
                                <option value="2">2</option>
                                <option value="3">3</option>
                                <option value="4">4</option>
                            </select>
                        </span>
                        <span className="fieldIcons__before"></span>
                        <span className="fieldIcons__after">
                            <span className="icon__root">
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    width="18"
                                    height="18"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    strokeLinecap="round"
                                    strokeLinejoin="round">
                                    <polyline points="6 9 12 15 18 9"></polyline>
                                </svg>
                            </span>
                        </span>
                    </span>
                    <p className="message-root"></p>
                </div>
            </section>
            <section className="productFullDetail__cartActions productFullDetail__section">
                <button
                    className="button__root_highPriority button__root clickable__root button__filled"
                    type="button"
                    disabled={!canAddToCart()}
                    onClick={addToCart}>
                    <span className="button__content">
                        <span>{formatMessage({ id: 'product:add-item', defaultMessage: 'Add to Cart' })}</span>
                    </span>
                </button>
            </section>
        </>
    );
};

export default GiftCartOptions;
