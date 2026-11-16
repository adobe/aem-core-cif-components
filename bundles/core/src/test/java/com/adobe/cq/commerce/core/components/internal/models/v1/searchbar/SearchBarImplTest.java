/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.commerce.core.components.internal.models.v1.searchbar;

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.adobe.cq.commerce.core.components.models.searchbar.Searchbar;
import com.adobe.cq.commerce.core.testing.Utils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.core.testing.TestContext.newAemContext;

public class SearchBarImplTest {

    @Rule
    public final AemContext context = newAemContext("/context/jcr-searchbar.json");

    private static final String PAGE = "/content/pageA";
    private static final String SEARCH_BAR = "/content/pageA/jcr:content/root/responsivegrid/searchbar";

    private Resource searchBarResource;
    private Searchbar searchBar;

    @Before
    public void setup() {
        ConfigurationBuilder mockConfigBuilder = Mockito.mock(ConfigurationBuilder.class);
        Utils.addDataLayerConfig(mockConfigBuilder, true);
        context.registerAdapter(Resource.class, ConfigurationBuilder.class, mockConfigBuilder);

        Page page = context.currentPage(PAGE);
        context.currentResource(SEARCH_BAR);
        searchBarResource = context.resourceResolver().getResource(SEARCH_BAR);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(searchBarResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);

        // Configure the component to create deep links to specific pages
        context.request().setAttribute(WCMMode.class.getName(), WCMMode.EDIT);
    }

    @Test
    public void testSearchResultPage() {
        searchBar = context.request().adaptTo(SearchbarImpl.class);
        Assert.assertEquals("/content/pageB.html", searchBar.getSearchResultsPageUrl());
        Assert.assertEquals("/content/pageB.html", searchBar.getSearchResultsPageUrl());
    }

    @Test
    public void testSearchResultPageWithMapping() {
        addMapping("localhost.80");
        searchBar = context.request().adaptTo(SearchbarImpl.class);
        Assert.assertEquals("/pageB.html", searchBar.getSearchResultsPageUrl());
    }

    @Test
    public void testSearchResultPageWithMappingInDifferentDomain() {
        addMapping("foobar.80");
        searchBar = context.request().adaptTo(SearchbarImpl.class);
        Assert.assertEquals("/content/pageB.html", searchBar.getSearchResultsPageUrl());
    }

    @Test
    public void testSearchResultPageWithNullMapping() {
        ResourceResolver resolver = Mockito.spy(context.resourceResolver());
        Mockito.doReturn(null).when(resolver).map(Mockito.any(SlingHttpServletRequest.class), Mockito.eq("/content/pageB"));
        SlingHttpServletRequest request = new SlingHttpServletRequestWrapper(context.request()) {
            @Override
            public ResourceResolver getResourceResolver() {
                return resolver;
            }
        };
        Searchbar model = context.getService(ModelFactory.class).createModel(request, SearchbarImpl.class);

        Assert.assertEquals("/content/pageB.html", model.getSearchResultsPageUrl());
    }

    @Test
    public void testSearchResultPageFallsBackToCurrentPage() {
        setupWithoutSearchResultsPage();
        Assert.assertEquals("/content/pageA.html", searchBar.getSearchResultsPageUrl());
        Assert.assertEquals("/content/pageA.html", searchBar.getSearchResultsPageUrl());
    }

    @Test
    public void testSearchResultPageFallsBackToMappedCurrentPage() {
        setupWithoutSearchResultsPage();
        addMapping("localhost.80");
        Assert.assertEquals("/pageA.html", searchBar.getSearchResultsPageUrl());
    }

    private void addMapping(String host) {
        String mappingPath = "/etc/map/http/" + host;
        context.create().resource(mappingPath, "sling:internalRedirect", new String[] { "/", "/content" });
        // Refresh MapEntries because the mock repository does not send JCR events.
        context.getService(EventAdmin.class).sendEvent(new Event(
            "org/apache/sling/api/resource/Resource/ADDED",
            Collections.singletonMap("path", mappingPath)));
    }

    private void setupWithoutSearchResultsPage() {
        context.resourceResolver().getResource("/content/jcr:content").adaptTo(ModifiableValueMap.class)
            .remove("cq:cifSearchResultsPage");
        searchBar = context.getService(ModelFactory.class).createModel(context.request(), SearchbarImpl.class);
    }

    @Test
    public void testJsonRender() throws IOException {
        searchBar = context.request().adaptTo(SearchbarImpl.class);
        ObjectMapper mapper = new ObjectMapper();
        String expected = Utils.getResource("results/result-datalayer-searchbar-component.json");
        String jsonResult = searchBar.getData().getJson();
        Assert.assertEquals(mapper.readTree(expected), mapper.readTree(jsonResult));
    }

}
