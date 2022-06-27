/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.components.models.breadcrumb;

import java.util.Comparator;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;

public interface Breadcrumb extends com.adobe.cq.wcm.core.components.models.Breadcrumb {

    /**
     * When a product is in multiple categories, the comparator returned by this method is used
     * to sort and select the "primary" catagory used for the breadcrumb. The default comparator
     * uses the following rules to sort the categories:<br>
     * <ul>
     * <li>if the <code>structureDepth</code> property is set on the breadcrumb component resource,
     * categories with a depth smaller or equal than the property value are chosen first
     * <li>the categories with deepest depth are then chosen first
     * </ul>
     *
     * @return The comparator.
     * @deprecated this {@link Comparator} is not used by the implementation anymore. Instead, the retrieved categories are filtered by
     *             the context url path. After that filtering, the categories are sorted by url path in reversed natural order.
     */
    @Deprecated
    Comparator<CategoryInterface> getCategoryInterfaceComparator();
}
