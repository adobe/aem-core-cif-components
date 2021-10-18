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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.apache.sling.sitemap.builder.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

class SitemapGeneratorBase {

    protected static final DateTimeFormatter GQL_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final Logger logger;

    protected SitemapGeneratorBase() {
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    protected void addLastModified(Url url, ProductInterface productInterface) {
        addLastModified(url, productInterface.getSku(), productInterface.getCreatedAt(), productInterface.getUpdatedAt());
    }

    protected void addLastModified(Url url, CategoryInterface categoryInterface) {
        addLastModified(url, categoryInterface.getUrlPath(), categoryInterface.getCreatedAt(), categoryInterface.getUpdatedAt());
    }

    private void addLastModified(Url url, String identifier, String createdAtStr, String updatedAtStr) {
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;
        try {
            TemporalAccessor ta = GQL_TIMESTAMP_FORMAT.parse(createdAtStr);
            createdAt = LocalDateTime.from(ta);
        } catch (DateTimeParseException | NullPointerException ex) {
            logger.warn("Could not parse created_at of '{}': {}", identifier, createdAtStr, ex);
        }

        try {
            TemporalAccessor ta = GQL_TIMESTAMP_FORMAT.parse(updatedAtStr);
            updatedAt = LocalDateTime.from(ta);
        } catch (DateTimeParseException | NullPointerException ex) {
            logger.warn("Could not parse updated_at of '{}': {}", identifier, updatedAtStr, ex);
        }

        if ((createdAt != null && updatedAt != null && updatedAt.isAfter(createdAt))) {
            url.setLastModified(updatedAt.toInstant(ZoneOffset.UTC));
        } else if (createdAt != null) {
            url.setLastModified(createdAt.toInstant(ZoneOffset.UTC));
        }
    }
}
