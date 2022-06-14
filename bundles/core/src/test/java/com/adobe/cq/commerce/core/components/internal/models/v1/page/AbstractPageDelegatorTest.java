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
package com.adobe.cq.commerce.core.components.internal.models.v1.page;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.HtmlPageItem;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.adobe.cq.wcm.core.components.models.Page;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.google.common.collect.ImmutableMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractPageDelegatorTest {

    protected Page testDelegationCreateSubject(Page mock) {
        return new AbstractPageDelegator() {
            @Override
            protected Page getDelegate() {
                return mock;
            }
        };
    }

    @Test
    public void testDelegation() {
        Page mock = mock(Page.class);
        Page subject = testDelegationCreateSubject(mock);
        assertNotNull(subject);

        when(mock.getLanguage()).thenReturn("en");
        assertEquals("en", subject.getLanguage());

        Calendar now = Calendar.getInstance();
        when(mock.getLastModifiedDate()).thenReturn(now);
        assertEquals(now, subject.getLastModifiedDate());

        when(mock.getKeywords()).thenReturn(new String[] { "foo", "bar" });
        assertThat(subject.getKeywords()).containsExactly("foo", "bar");

        when(mock.getDesignPath()).thenReturn("/etc/designs/foobar");
        assertEquals(subject.getDesignPath(), "/etc/designs/foobar");

        when(mock.getStaticDesignPath()).thenReturn("/etc/designs/foobar");
        assertEquals(subject.getStaticDesignPath(), "/etc/designs/foobar");

        when(mock.getFavicons()).thenReturn(Collections.singletonMap("foo", "bar.png"));
        assertThat(subject.getFavicons()).containsEntry("foo", "bar.png");

        when(mock.getTitle()).thenReturn("title");
        assertEquals("title", subject.getTitle());

        when(mock.getBrandSlug()).thenReturn("brand slug");
        assertEquals("brand slug", subject.getBrandSlug());

        when(mock.getClientLibCategories()).thenReturn(new String[] { "my.page.v1" });
        assertThat(subject.getClientLibCategories()).containsExactly("my.page.v1");

        when(mock.getClientLibCategoriesJsBody()).thenReturn(new String[] { "my.page.v1" });
        assertThat(subject.getClientLibCategoriesJsBody()).containsExactly("my.page.v1");

        when(mock.getClientLibCategoriesJsHead()).thenReturn(new String[] { "my.page.v1" });
        assertThat(subject.getClientLibCategoriesJsHead()).containsExactly("my.page.v1");

        when(mock.getTemplateName()).thenReturn("template");
        assertEquals("template", subject.getTemplateName());

        when(mock.getAppResourcesPath()).thenReturn("/content/foo/bar");
        assertEquals("/content/foo/bar", subject.getAppResourcesPath());

        when(mock.getCssClassNames()).thenReturn("my-page__root");
        assertEquals("my-page__root", subject.getCssClassNames());

        NavigationItem redirectTarget = mock(NavigationItem.class);
        when(mock.getRedirectTarget()).thenReturn(redirectTarget);
        assertEquals(redirectTarget, subject.getRedirectTarget());

        when(mock.hasCloudconfigSupport()).thenReturn(true);
        assertTrue(subject.hasCloudconfigSupport());

        when(mock.getComponentsResourceTypes()).thenReturn(Collections.singleton("my/page"));
        assertThat(subject.getComponentsResourceTypes()).containsExactly("my/page");

        when(mock.getExportedItemsOrder()).thenReturn(new String[] { "exportedItem1", "exportedItem2" });
        assertThat(subject.getExportedItemsOrder()).containsExactly("exportedItem1", "exportedItem2");

        ComponentExporter exportedItem = mock(ComponentExporter.class);
        Map<String, ComponentExporter> exportedItems = Collections.singletonMap("exportedItem", exportedItem);
        doReturn(exportedItems).when(mock).getExportedItems();
        assertThat((Map<String, ComponentExporter>) subject.getExportedItems()).containsEntry("exportedItem", exportedItem);

        when(mock.getExportedType()).thenReturn("type");
        assertEquals("type", subject.getExportedType());

        when(mock.getMainContentSelector()).thenReturn("main");
        assertEquals("main", subject.getMainContentSelector());

        HtmlPageItem htmlPageItem = mock(HtmlPageItem.class);
        when(mock.getHtmlPageItems()).thenReturn(Collections.singletonList(htmlPageItem));
        assertThat(subject.getHtmlPageItems()).contains(htmlPageItem);

        when(mock.getId()).thenReturn("id");
        assertEquals("id", subject.getId());

        assertNull(subject.getData());
        ComponentData data = mock(ComponentData.class);
        when(data.getId()).thenReturn("myId");
        when(mock.getData()).thenReturn(data);
        assertEquals(data.getId(), subject.getData().getId());
        assertEquals("type", subject.getData().getType());

        when(mock.getAppliedCssClasses()).thenReturn("my-page__root");
        assertEquals("my-page__root", subject.getAppliedCssClasses());

        when(mock.getDescription()).thenReturn("description");
        assertEquals("description", subject.getDescription());

        when(mock.getCanonicalLink()).thenReturn("canonicalLink");
        assertEquals("canonicalLink", subject.getCanonicalLink());

        Map<Locale, String> alternateLanguageLinks = ImmutableMap.of(Locale.CANADA, "http://venia.ca/en.html");
        when(mock.getAlternateLanguageLinks()).thenReturn(alternateLanguageLinks);
        assertThat(subject.getAlternateLanguageLinks()).containsExactly(alternateLanguageLinks.entrySet().toArray(new Map.Entry[0]));

        when(mock.getRobotsTags()).thenReturn(Arrays.asList("noindex", "nofollow"));
        assertThat(subject.getRobotsTags()).containsExactly("noindex", "nofollow");
    }
}
