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

package com.adobe.cq.commerce.core.components.internal.models.v1.breadcrumb;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;

import static org.assertj.core.api.Assertions.assertThat;

public class BreadcrumbImplTest {

    @Test
    public void testCategoryInterfaceComparator() {
        CategoryTree c1 = new CategoryTree();
        c1.setUrlPath("men");
        c1.setId(1);

        CategoryTree c2 = new CategoryTree();
        c2.setUrlPath("men/tops");
        c2.setId(2);

        CategoryTree c3 = new CategoryTree();
        c3.setUrlPath("men/tops/tanks");
        c3.setId(3);

        CategoryTree c4 = new CategoryTree();
        c4.setUrlPath("women/tops");
        c4.setId(4);

        BreadcrumbImpl breadcrumb = new BreadcrumbImpl();
        List<CategoryInterface> categories = Arrays.asList(c4, c3, c2, c1);

        Whitebox.setInternalState(breadcrumb, "structureDepth", 1);
        categories.sort(breadcrumb.getCategoryInterfaceComparator());
        // [men, men/tops/tanks, men/tops, women/tops]
        assertThat(categories).containsExactly(c1, c3, c2, c4);

        Whitebox.setInternalState(breadcrumb, "structureDepth", 2);
        categories.sort(breadcrumb.getCategoryInterfaceComparator());
        // [men/tops, women/tops, men, men/tops/tanks]
        assertThat(categories).containsExactly(c2, c4, c1, c3);

        Whitebox.setInternalState(breadcrumb, "structureDepth", 3);
        categories.sort(breadcrumb.getCategoryInterfaceComparator());
        // [men/tops/tanks, men/tops, women/tops, men]
        assertThat(categories).containsExactly(c3, c2, c4, c1);
    }
}
