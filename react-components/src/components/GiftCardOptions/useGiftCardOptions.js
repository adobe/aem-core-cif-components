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
import { useEffect, useState } from 'react';
import { useAwaitQuery } from '../../utils/hooks';
import GIFT_CARD_PRODUCT_QUERY from '../../queries/query_gift_card_product.graphql';

const OPEN_AMOUNT = 'open_amount';

const useGiftCardOptions = props => {
    const { sku, useUid } = props;

    const [giftCardState, setGiftCardState] = useState(null);
    const giftCardProductQuery = useAwaitQuery(GIFT_CARD_PRODUCT_QUERY);

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
    }, [sku, giftCardProductQuery]);

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

    const canAddToCart = () => {
        if (giftCardState === null) return false;

        const {
            giftCardOptions: { gift_card_options, open_amount_min, open_amount_max },
            giftCardValues: { open_amount, custom_amount, selected_amount, entered_options }
        } = giftCardState;

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

        for (const option of gift_card_options.filter(
            o => o.required && o.title.toLowerCase() !== 'custom giftcard amount'
        )) {
            if (entered_options[option.value.uid].trim() === '') {
                return false;
            }
        }

        return true;
    };

    const addToCart = () => {
        if (canAddToCart()) {
            const {
                giftCardValues: {
                    quantity,
                    open_amount,
                    custom_amount,
                    custom_amount_uid,
                    selected_amount,
                    entered_options
                }
            } = giftCardState;

            const productData = {
                useUid,
                sku,
                parentSku: sku,
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
        }
    };

    const addToWishlist = () => {
        const {
            giftCardValues: { quantity }
        } = giftCardState;
        const productData = { sku, quantity };
        const customEvent = new CustomEvent('aem.cif.add-to-wishlist', {
            detail: [productData]
        });
        document.dispatchEvent(customEvent);
    };

    return [
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
    ];
};

export default useGiftCardOptions;
