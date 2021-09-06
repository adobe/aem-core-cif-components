package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.magento.graphql.CurrencyEnum;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.PriceRange;
import com.adobe.cq.commerce.magento.graphql.ProductDiscount;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductPrice;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProductListItemImplTest {

    @Rule
    public final AemContext aemContext = new AemContext();

    private Page page;

    @Before
    public void setup() {
        page = aemContext.create().page("/my/page");
    }

    @Test
    public void testGenerateIdForEmptySku() {
        // test for null empty or blank
        String[] skus = new String[] { null, "", "    " };
        String expected = "foobar-item-e3b0c44298";
        // create a mock price range
        Money money = new Money();
        money.setCurrency(CurrencyEnum.USD);
        money.setValue(1.0);
        ProductDiscount discount = new ProductDiscount();
        discount.setAmountOff(0.0);
        ProductPrice price = new ProductPrice();
        price.setFinalPrice(money);
        price.setRegularPrice(money);
        price.setDiscount(discount);
        PriceRange priceRange = new PriceRange();
        priceRange.setMinimumPrice(price);
        priceRange.setMaximumPrice(price);

        for (String sku : skus) {
            // test all constructors
            CommerceIdentifier commerceIdentifier = mock(CommerceIdentifier.class);
            when(commerceIdentifier.getType()).thenReturn(CommerceIdentifier.IdentifierType.SKU);
            when(commerceIdentifier.getValue()).thenReturn(sku);
            ProductListItemImpl item = new ProductListItemImpl(commerceIdentifier, "foobar", page);
            assertEquals(expected, item.generateId());

            item = new ProductListItemImpl(sku, null, null, null, null, null, page, null, null, null, "foobar", false);
            assertEquals(expected, item.generateId());

            ProductInterface product = mock(ProductInterface.class);
            when(product.getSku()).thenReturn(sku);
            when(product.getPriceRange()).thenReturn(priceRange);
            item = new ProductListItemImpl(product, page, null, null, null, "foobar");
            assertEquals(expected, item.generateId());
        }
    }
}
