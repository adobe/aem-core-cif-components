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

package com.adobe.cq.commerce.core.components.internal.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Test;

import com.day.cq.commons.DownloadResource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ImageResourceWrapperTest {

    private final String TEST_DAM_PATH = "/content/dam/folder/image.jpg";

    @Test
    public void testBasicWrapping() {
        Map<String, Object> properties = new HashMap<String, Object>() {
            {
                put("a", 1);
                put("b", 2);
                put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "a/b/c");
            }
        };
        Resource wrappedResource = new ImageResourceWrapper(prepareResourceToBeWrapped(properties), "d/e/f", TEST_DAM_PATH);
        ArrayList<Map.Entry> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new DefaultMapEntry("a", 1));
        keyValuePairs.add(new DefaultMapEntry("b", 2));
        keyValuePairs.add(new DefaultMapEntry(ResourceResolver.PROPERTY_RESOURCE_TYPE, "d/e/f"));
        testValueMap(keyValuePairs, wrappedResource.adaptTo(ValueMap.class));
        testValueMap(keyValuePairs, wrappedResource.getValueMap());
        assertEquals("d/e/f", wrappedResource.getResourceType());
        assertEquals(TEST_DAM_PATH, wrappedResource.getValueMap().get(DownloadResource.PN_REFERENCE));
    }

    @Test
    public void testEmptyFileReference() {
        Map<String, Object> properties = new HashMap<String, Object>() {
            {
                put("a", 1);
                put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "a/b/c");
            }
        };
        Resource wrappedResource = new ImageResourceWrapper(prepareResourceToBeWrapped(properties), "d/e/f", "");
        ArrayList<Map.Entry> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new DefaultMapEntry("a", 1));
        keyValuePairs.add(new DefaultMapEntry(ResourceResolver.PROPERTY_RESOURCE_TYPE, "d/e/f"));
        testValueMap(keyValuePairs, wrappedResource.adaptTo(ValueMap.class));
        testValueMap(keyValuePairs, wrappedResource.getValueMap());
        assertEquals("d/e/f", wrappedResource.getResourceType());
        assertFalse(wrappedResource.getValueMap().containsKey(DownloadResource.PN_REFERENCE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNulls() {
        Resource wrappedResource = new ImageResourceWrapper(null, null, null);
    }

    private Resource prepareResourceToBeWrapped(Map<String, Object> properties) {
        Resource resource = mock(Resource.class);
        ValueMap valueMap = new ValueMapDecorator(properties);
        when(resource.getValueMap()).thenReturn(valueMap);
        when(resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
        return resource;
    }

    private void testValueMap(List<Map.Entry> keyValuePairs, ValueMap valueMap) {
        for (Map.Entry entry : keyValuePairs) {
            assertEquals(entry.getValue(), valueMap.get(entry.getKey().toString()));
        }
    }

}
