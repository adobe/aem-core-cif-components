/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.models.common;

/**
 * An identifier for any commerce entity (category or product). A commerce entity can be identified by more than one key, for example a
 * product can be identified by the URL key or by the SKU.
 */
public interface CommerceIdentifier {

    /**
     * The type of this identifier
     */
    enum IdentifierType {
        SKU, URL_KEY, UID, URL_PATH
    }

    /**
     * The type of the entity which is identified
     */
    enum EntityType {
        PRODUCT, CATEGORY
    }

    /**
     * The value of the identifier
     * 
     * @return a string value representing the value of the identifier
     */
    String getValue();

    /**
     * The type of the identifier
     * 
     * @return a {@link IdentifierType} value
     */
    IdentifierType getType();

    /**
     * The type of entity which identifier is for
     * 
     * @return a {@link EntityType} value
     */
    EntityType getEntityType();
}
