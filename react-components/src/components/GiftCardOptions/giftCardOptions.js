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
import React from 'react';
import PropTypes from 'prop-types';
import { defineMessages, useIntl } from 'react-intl';
import LoadingIndicator from '../LoadingIndicator';
import Price from '../Price';
import useGiftCardOptions from './useGiftCardOptions';
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

const GiftCardOptions = props => {
    const { sku, showAddToWishList, useUid } = props;
    const [
        giftCardState,
        {
            changeAmountSelection,
            changeCustomAmount,
            changeOptionValue,
            changeQuantity,
            canAddToCart,
            addToCart,
            addToWishlist
        }
    ] = useGiftCardOptions({ sku, useUid });

    const intl = useIntl();

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
        giftCardValues: { quantity, open_amount, custom_amount, selected_amount, entered_options }
    } = giftCardState;

    return (
        <>
            {giftcard_amounts.length > 0 && (
                <section className="productFullDetail__section productFullDetail__giftCardProduct">
                    <h3 className="option__title">
                        <span>{intl.formatMessage(messages.amountSelectionLabel)}</span>{' '}
                        <span className="required"> *</span>
                    </h3>
                    <div>
                        <div className="giftCardOptionSelect__root">
                            <span className="fieldIcons__root">
                                <span className="fieldIcons__input">
                                    <select
                                        aria-label={intl.formatMessage(messages.amountSelectionLabel)}
                                        className="select__input field__input giftCardProduct__option"
                                        value={selected_amount}
                                        onChange={changeAmountSelection}>
                                        <option value="">{intl.formatMessage(messages.chooseAmount)}...</option>
                                        {giftcard_amounts.map(o => (
                                            <option key={o.uid} value={o.uid}>
                                                {o.value}
                                            </option>
                                        ))}
                                        {allow_open_amount && (
                                            <option value={OPEN_AMOUNT}>
                                                {intl.formatMessage(messages.otherAmount)}...
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
                            {intl.formatMessage(messages.customAmountLabel)} {currency}
                        </span>{' '}
                        <span className="required"> *</span>
                    </h3>
                    <div>
                        <div className="giftCardOptionSelect__root">
                            <span>
                                <input
                                    type="number"
                                    aria-label={intl.formatMessage(messages.customAmountLabel)}
                                    className="field__input"
                                    min={open_amount_min}
                                    max={open_amount_max}
                                    value={custom_amount}
                                    onChange={changeCustomAmount}
                                />
                            </span>
                            <span>
                                {intl.formatMessage(messages.customPriceMinimum)}:{' '}
                                <Price currencyCode={currency} value={open_amount_min} />{' '}
                                {intl.formatMessage(messages.customPriceMaximum)}:{' '}
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
                    <span>{intl.formatMessage({ id: 'cart:quantity', defaultMessage: 'Quantity' })}</span>
                </h2>
                <div className="quantity__root">
                    <span className="fieldIcons__root" style={{ '--iconsBefore': 0, '--iconsAfter': 1 }}>
                        <span className="fieldIcons__input">
                            <select
                                aria-label={intl.formatMessage({
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
            <section className="productFullDetail__cartActions productFullDetail__actions productFullDetail__section">
                <button
                    className="button__root_highPriority button__root clickable__root button__filled"
                    type="button"
                    disabled={!canAddToCart()}
                    onClick={addToCart}>
                    <span className="button__content">
                        <span>{intl.formatMessage({ id: 'product:add-item', defaultMessage: 'Add to Cart' })}</span>
                    </span>
                </button>
                {showAddToWishList && (
                    <button
                        className="button__root_normalPriority button__root clickable__root"
                        type="button"
                        onClick={addToWishlist}>
                        <span className="button__content">
                            <span>
                                {intl.formatMessage({
                                    id: 'product:add-to-wishlist',
                                    defaultMessage: 'Add to Wish List'
                                })}
                            </span>
                        </span>
                    </button>
                )}
            </section>
        </>
    );
};

GiftCardOptions.propTypes = {
    sku: PropTypes.string.required,
    showAddToWishList: PropTypes.bool,
    useUid: PropTypes.bool
};

export default GiftCardOptions;
