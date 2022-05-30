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
package com.adobe.cq.commerce.core.components.internal.models.v3.teaser;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerListItem;
import com.adobe.cq.commerce.core.components.internal.models.v1.Utils;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.teaser.CommerceTeaserActionItem;
import com.adobe.cq.wcm.core.components.commons.link.Link;

public class CommerceTeaserActionItemImpl extends DataLayerListItem implements CommerceTeaserActionItem {

    private final String title;
    private final String url;
    private CommerceIdentifier identifier;
    private CommerceLink link;

    public CommerceTeaserActionItemImpl(String title, String url, Resource action, String parentId) {
        super(parentId, action);
        this.title = title;
        this.url = url;
    }

    public CommerceTeaserActionItemImpl(String title, String url, CommerceIdentifier identifier, Resource action, String parentId) {
        this(title, url, action, parentId);
        this.identifier = identifier;
        this.link = new CommerceLink(resource, url);
    }

    @Override
    public Link getLink() {
        return link;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return title;
    }

    @Nonnull
    @Override
    public String getURL() {
        return url;
    }

    @Nullable
    public CommerceIdentifier getEntityIdentifier() {
        return identifier;
    }

    // DataLayer methods

    @Override
    public String getDataLayerLinkUrl() {
        return getURL();
    }

    @Override
    public String getDataLayerTitle() {
        return getTitle();
    }

    @Override
    public String getDataLayerType() {
        return identifier != null ? identifier.getEntityType().toString() : null;
    }

    private static class CommerceLink implements Link {
        private final Map<String, String> htmlAttributes = new HashMap<>();
        private final String url;

        public CommerceLink(Resource resource, String url) {
            this.url = url;
            htmlAttributes.put("href", url);
            String linkTarget = resource.getValueMap().get(PN_LINK_TARGET, String.class);
            linkTarget = Utils.normalizeLinkTarget(linkTarget);
            if (StringUtils.isNotBlank(linkTarget)) {
                htmlAttributes.put("target", linkTarget);
            }
        }

        @Override
        public String getURL() {
            return url;
        }

        @Override
        public boolean isValid() {
            return StringUtils.isNotBlank(url);
        }

        @Override
        public Map<String, String> getHtmlAttributes() {
            return htmlAttributes;
        }
    }
}
