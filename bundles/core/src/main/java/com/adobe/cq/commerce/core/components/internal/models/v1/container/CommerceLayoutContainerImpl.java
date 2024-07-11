/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.container;

import java.lang.reflect.Field;
import java.util.*;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.via.ForcedResourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.internal.services.CommerceComponentModelFinder;
import com.adobe.cq.commerce.core.components.models.RetrievingModel;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractRetriever;
import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.LayoutContainer;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = LayoutContainer.class,
    resourceType = CommerceLayoutContainerImpl.RESOURCE_TYPE)
public class CommerceLayoutContainerImpl implements LayoutContainer {
    static final String RESOURCE_TYPE = "core/cif/components/commerce/container/v1/container";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @Self
    @Via(type = ForcedResourceType.class, value = "core/wcm/components/container/v1/container")
    private LayoutContainer wcmLayoutContainer = null;

    @OSGiService
    private CommerceComponentModelFinder commerceModelFinder;

    @PostConstruct
    void initModel() {
        if (true)
            return;

        // iterate over collection of models to find them in the current page
        List<String> queries = new ArrayList<>();
        Collection<RetrievingModel> models = commerceModelFinder.findModels(request, resource);
        for (RetrievingModel model : models) {
            queries.add(model.getRetriever().generateQuery());
        }

        if (!queries.isEmpty()) {
            AbstractRetriever retriever = models.iterator().next().getRetriever();
            try {
                Field field = AbstractRetriever.class.getDeclaredField("client");
                field.setAccessible(true);
                MagentoGraphqlClient graphqlClient = (MagentoGraphqlClient) field.get(retriever);
                if (graphqlClient != null) {
                    graphqlClient.executeAll(queries);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ComponentData getData() {
        return wcmLayoutContainer.getData();
    }

    @Override
    public String getAccessibilityLabel() {
        return wcmLayoutContainer.getAccessibilityLabel();
    }

    @Override
    public String getExportedType() {
        return resource.getResourceType();
    }

    @Override
    public @NotNull List<ListItem> getItems() {
        return wcmLayoutContainer.getItems();
    }

    @Override
    public String getId() {
        return wcmLayoutContainer.getId();
    }

    @Override
    public @Nullable String getAppliedCssClasses() {
        return wcmLayoutContainer.getAppliedCssClasses();
    }

    @NotNull
    @Override
    public LayoutType getLayout() {
        return wcmLayoutContainer.getLayout();
    }

    @Override
    public String getRoleAttribute() {
        return wcmLayoutContainer.getRoleAttribute();
    }

    @Override
    public @Nullable String getBackgroundStyle() {
        return wcmLayoutContainer.getBackgroundStyle();
    }

    @Override
    public @NotNull Map<String, ? extends ComponentExporter> getExportedItems() {
        return wcmLayoutContainer.getExportedItems();
    }

    @Override
    public @NotNull String[] getExportedItemsOrder() {
        return wcmLayoutContainer.getExportedItemsOrder();
    }
}
