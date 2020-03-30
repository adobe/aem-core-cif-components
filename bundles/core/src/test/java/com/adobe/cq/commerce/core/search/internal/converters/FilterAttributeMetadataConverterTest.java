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

package com.adobe.cq.commerce.core.search.internal.converters;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.cq.commerce.core.search.models.FilterAttributeMetadata;
import com.adobe.cq.commerce.magento.graphql.Attribute;
import com.adobe.cq.commerce.magento.graphql.__InputValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilterAttributeMetadataConverterTest {

    @Mock
    Attribute attribute;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    __InputValue inputField;

    FilterAttributeMetadataConverter converterUnderTest;

    @Test
    public void testConvertsForInputAlone() {

        final String CODE = "cool_attribute";
        final String TYPE_NAME = "OK";

        when(inputField.getName()).thenReturn(CODE);
        when(inputField.getType().getName()).thenReturn(TYPE_NAME);

        converterUnderTest = new FilterAttributeMetadataConverter(new ArrayList<>());

        final FilterAttributeMetadata result = converterUnderTest.apply(inputField);

        assertThat(result).isNotNull();
        assertThat(result.getAttributeCode()).isEqualTo(CODE);
        assertThat(result.getFilterInputType()).isEqualTo(TYPE_NAME);

    }

    @Test
    public void testConvertsForInputWithMatchingAttribute() {

        final String CODE = "cool_attribute";
        final String TYPE_NAME = "AWildInputAppears";
        final String ATTRIBUTE_TYPE = "Int";
        final String INPUT_TYPE = "boolean";

        when(inputField.getName()).thenReturn(CODE);
        when(inputField.getType().getName()).thenReturn(TYPE_NAME);

        when(attribute.getAttributeCode()).thenReturn(CODE);
        when(attribute.getAttributeType()).thenReturn(ATTRIBUTE_TYPE);
        when(attribute.getInputType()).thenReturn(INPUT_TYPE);

        converterUnderTest = new FilterAttributeMetadataConverter(Arrays.asList(attribute));

        final FilterAttributeMetadata result = converterUnderTest.apply(inputField);

        assertThat(result.getAttributeInputType()).isEqualTo(INPUT_TYPE);
        assertThat(result.getAttributeType()).isEqualTo(ATTRIBUTE_TYPE);

    }

    @Test
    public void testConverteOnlyMatchesAttributeCodes() {

        final String CODE = "cool_attribute";
        final String DIFFERENT_CODE = "uncool_attribute";
        final String TYPE_NAME = "AWildInputAppears";
        final String ATTRIBUTE_TYPE = "Int";
        final String INPUT_TYPE = "boolean";

        when(inputField.getName()).thenReturn(CODE);
        when(inputField.getType().getName()).thenReturn(TYPE_NAME);

        when(attribute.getAttributeCode()).thenReturn(DIFFERENT_CODE);
        when(attribute.getAttributeType()).thenReturn(ATTRIBUTE_TYPE);
        when(attribute.getInputType()).thenReturn(INPUT_TYPE);

        converterUnderTest = new FilterAttributeMetadataConverter(Arrays.asList(attribute));

        final FilterAttributeMetadata result = converterUnderTest.apply(inputField);

        assertThat(result.getAttributeInputType()).isNullOrEmpty();
        assertThat(result.getAttributeType()).isNullOrEmpty();

    }

}
