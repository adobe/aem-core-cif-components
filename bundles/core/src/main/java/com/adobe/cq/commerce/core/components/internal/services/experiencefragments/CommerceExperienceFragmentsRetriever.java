/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2022 Adobe
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
package com.adobe.cq.commerce.core.components.internal.services.experiencefragments;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

import com.day.cq.wcm.api.Page;

/**
 * This service searches for experience fragments associated
 * with a product or category
 */
@ProviderType
public interface CommerceExperienceFragmentsRetriever {

    /**
     * This method returns a list of experience fragments that match the location
     * and product identifier.
     * 
     * @return The a list of experience fragments that match this container.
     */
    List<Resource> getExperienceFragmentsForProduct(String sku, String fragmentLocation, Page currentPage);

    /**
     * This method returns a list of experience fragments that match the location
     * and category identifier.
     * 
     * @return The a list of experience fragments that match this container.
     */
    List<Resource> getExperienceFragmentsForCategory(String categoryUid, String fragmentLocation, Page currentPage);

}
