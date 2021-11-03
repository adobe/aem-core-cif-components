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
package com.adobe.cq.commerce.core.components.internal.services.sitemap;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.sling.sitemap.builder.Url;
import org.junit.Test;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SitemapGeneratorBaseTest {

    private final SitemapGeneratorBase subject = new SitemapGeneratorBase();

    @Test
    public void testLastModifiedAddedFromProductInterface() {
        // given
        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("mock");
        when(product.getCreatedAt()).thenReturn("2021-10-12 17:20:15");
        when(product.getUpdatedAt()).thenReturn("2021-10-12 17:30:15");
        Url url = mock(Url.class);

        // when
        subject.addLastModified(url, product);

        // then
        Instant expected = ZonedDateTime.of(2021, 10, 12, 17, 30, 15, 0, ZoneId.of("UTC"))
            .toInstant();
        verify(url).setLastModified(expected);
    }

    @Test
    public void testLastModifiedAddedFromProductInterfaceCreatedAt() {
        // given
        ProductInterface product = mock(ProductInterface.class);
        when(product.getSku()).thenReturn("mock");
        when(product.getCreatedAt()).thenReturn("2021-10-12 17:20:15");
        Url url = mock(Url.class);

        // when
        subject.addLastModified(url, product);

        // then
        Instant expected = ZonedDateTime.of(2021, 10, 12, 17, 20, 15, 0, ZoneId.of("UTC"))
            .toInstant();
        verify(url).setLastModified(expected);
    }

    @Test
    public void testLastModifiedAddedFromCategoryInterface() {
        // given
        CategoryInterface product = mock(CategoryInterface.class);
        when(product.getUrlPath()).thenReturn("mock/mock/mock");
        when(product.getCreatedAt()).thenReturn("2021-10-12 17:20:15");
        when(product.getUpdatedAt()).thenReturn("2021-10-12 17:30:15");
        Url url = mock(Url.class);

        // when
        subject.addLastModified(url, product);

        // then
        Instant expected = ZonedDateTime.of(2021, 10, 12, 17, 30, 15, 0, ZoneId.of("UTC"))
            .toInstant();
        verify(url).setLastModified(expected);
    }

    @Test
    public void testLastModifiedNotAdded() {
        // given
        CategoryInterface product = mock(CategoryInterface.class);
        when(product.getUrlPath()).thenReturn("mock/mock/mock");
        Url url = mock(Url.class);

        // when
        subject.addLastModified(url, product);

        // then
        verify(url, never()).setLastModified(any());
    }
}
