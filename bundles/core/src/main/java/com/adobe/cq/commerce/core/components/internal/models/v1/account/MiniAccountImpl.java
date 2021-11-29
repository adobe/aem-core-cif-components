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
package com.adobe.cq.commerce.core.components.internal.models.v1.account;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;

import com.adobe.cq.commerce.core.components.internal.datalayer.DataLayerComponent;
import com.adobe.cq.commerce.core.components.models.account.MiniAccount;
import com.day.cq.wcm.api.designer.Style;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = MiniAccount.class,
    resourceType = MiniAccountImpl.RT_MINIACCOUNT_V2)
public class MiniAccountImpl extends DataLayerComponent implements MiniAccount {

    protected static final String RT_MINIACCOUNT_V2 = "core/cif/components/content/miniaccount/v2/miniaccount";
    protected static final String PN_STYLE_ENABLE_WISH_LIST = "enableWishList";

    @ScriptVariable
    private Style currentStyle;

    private boolean wishListEnabled;

    @PostConstruct
    protected void initModel() {
        wishListEnabled = currentStyle.get(PN_STYLE_ENABLE_WISH_LIST, MiniAccount.super.getWishListEnabled());
    }

    @Override
    public boolean getWishListEnabled() {
        return wishListEnabled;
    }
}
