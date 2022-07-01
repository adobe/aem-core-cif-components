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
package com.adobe.cq.commerce.core.components.internal.services.experiencefragments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.jcr.MockQuery;
import org.apache.sling.testing.mock.jcr.MockQueryResult;
import org.apache.sling.testing.mock.jcr.MockQueryResultHandler;

import com.adobe.cq.commerce.core.components.services.experiencefragments.CommerceExperienceFragmentsRetriever;

/**
 * Mock query result handler to simulate the search of experience fragments.
 */
public class XFMockQueryResultHandler implements MockQueryResultHandler {

    private final Resource root;
    private final String productSku;
    private final String categoryId;
    private final String fragmentLocation;
    private final List<Node> nodes;
    private MockQuery query;

    /**
     * Instantiates a result handler that will start looking at the
     * <code>root</code> resource,
     * and will look for resources matching the given <code>sku</code> and
     * <code>fragmentLocation</code> parameters.
     *
     * @param root The resource where the search should start.
     * @param productSku The value of the <code>cq:products</code> property,
     *            can be null.
     * @param categoryId The value of the <code>cq:categories</code> property,
     *            can be null.
     * @param fragmentLocation The value of the <code>fragmentLocation</code>
     *            property, can be null.
     */
    public XFMockQueryResultHandler(Resource root, String productSku, String categoryId, String fragmentLocation) {
        this.root = root;
        this.productSku = productSku;
        this.categoryId = categoryId;
        this.fragmentLocation = fragmentLocation;
        nodes = new ArrayList<>();
    }

    @Override
    public MockQueryResult executeQuery(MockQuery query) {
        this.query = query;
        checkResource(root);
        return new MockQueryResult(nodes);
    }

    private void checkResource(Resource res) {
        Resource jcrContent = res.getChild("jcr:content");
        if (jcrContent != null) {
            ValueMap vm = jcrContent.getValueMap();
            String sku = vm.get(CommerceExperienceFragmentsRetriever.PN_CQ_PRODUCTS, String.class);
            String categoryId = vm.get(CommerceExperienceFragmentsRetriever.PN_CQ_CATEGORIES, String.class);
            String fragmentLocation = vm.get(CommerceExperienceFragmentsRetriever.PN_FRAGMENT_LOCATION, String.class);
            if (StringUtils.equals(this.fragmentLocation, fragmentLocation)
                && ((this.productSku == null && this.categoryId == null)
                    || (this.productSku != null && StringUtils.equals(this.productSku, sku))
                    || (this.categoryId != null && StringUtils.equals(this.categoryId, categoryId)))) {
                nodes.add(jcrContent.adaptTo(Node.class));
            }
        }

        Iterator<Resource> it = res.listChildren();
        while (it.hasNext()) {
            checkResource(it.next());
        }
    }

    public MockQuery getQuery() {
        return query;
    }
}
