/*
 *  Copyright 2021 Adobe. All rights reserved.
 *
 *   This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.adobe.cq.commerce.core.components.internal.models.v1.common;

import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;

public class CommerceIdentifierImpl implements CommerceIdentifier {

    private final EntityType entityType;
    private final String value;
    private final IdentifierType type;

    public CommerceIdentifierImpl(String value, CommerceIdentifier.IdentifierType type, CommerceIdentifier.EntityType entityType) {
        this.type = type;
        this.value = value;
        this.entityType = entityType;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public IdentifierType getType() {
        return type;
    }

    @Override
    public EntityType getEntityType() {
        return entityType;
    }
}
