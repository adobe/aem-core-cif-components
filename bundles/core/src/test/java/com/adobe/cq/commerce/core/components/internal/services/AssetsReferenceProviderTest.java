/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
package com.adobe.cq.commerce.core.components.internal.services;

import java.util.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.core.components.internal.models.v1.AssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.categorylist.CategoryListAssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel.ProductCarouselAssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserAssetsProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.reference.Reference;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LASTMODIFIED;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

public class AssetsReferenceProviderTest {

    private static final String PAGE = "/content/pageA";

    private final List<String> PRODUCT_TEASER_ASSETS = new ArrayList<String>() {
        {
            add("/content/dam/product-teaser-image.jpg");
        }
    };

    private final List<String> PRODUCT_CAROUSEL_ASSETS = new ArrayList<String>() {
        {
            add("/content/dam/product-carousel-image-1.jpg");
            add("/content/dam/product-carousel-image-2.jpg");
        }
    };

    private final List<String> CATEGORY_LIST_ASSETS = new ArrayList<String>() {
        {
            add("/content/dam/category-image-1.jpg");
            add("/content/dam/category-image-2.jpg");
            add("/content/dam/category-image-3.jpg");
        }
    };

    @Rule
    public final AemContext context = createContext("/context/jcr-content.json");

    private static AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            // Load page structure
            context.load().json(contentPath, "/content");

            UrlProviderImpl urlProvider = new UrlProviderImpl();
            urlProvider.activate(new MockUrlProviderConfiguration());
            context.registerService(UrlProvider.class, urlProvider);
        }, ResourceResolverType.JCR_MOCK);
    }

    private final AssetsReferenceProvider assetsReferenceProvider = new AssetsReferenceProvider();

    private Resource pageResource;

    private ResourceResolver resourceResolver;

    @Before
    public void setUp() {
        resourceResolver = Mockito.spy(context.resourceResolver());
        pageResource = Mockito.spy(resourceResolver.getResource(PAGE));
        context.currentResource(pageResource);
        Mockito.when(pageResource.getResourceResolver()).thenReturn(resourceResolver);

        List<AssetsProvider> assetsProviders = new ArrayList<AssetsProvider>() {
            {
                add(mockAssetsProvider(new ProductTeaserAssetsProvider(), PRODUCT_TEASER_ASSETS));
                add(mockAssetsProvider(new ProductCarouselAssetsProvider(), PRODUCT_CAROUSEL_ASSETS));
                add(mockAssetsProvider(new CategoryListAssetsProvider(), CATEGORY_LIST_ASSETS));
            }
        };
        Whitebox.setInternalState(assetsReferenceProvider, "assetsProviders", assetsProviders);
    }

    @Test
    public void testFindReferences() {
        List<Reference> referenceList = assetsReferenceProvider.findReferences(pageResource);
        Assert.assertNotNull(referenceList);
        Assert.assertEquals(6, referenceList.size());
    }

    private AssetsProvider mockAssetsProvider(AssetsProvider assetsProvider, List<String> assets) {
        // Mock assets
        for (String assetPath : assets) {
            mockAsset(assetPath);
        }

        // Mock asset provider
        AssetsProvider mockAssetsProvider = Mockito.spy(assetsProvider);
        doCallRealMethod().when(mockAssetsProvider).canHandle(any(Resource.class));
        doAnswer(invocationOnMock -> {
            Set<String> assetPaths = (Set<String>) invocationOnMock.getArguments()[1];
            assetPaths.addAll(assets);
            return null;
        }).when(mockAssetsProvider).addAssetPaths(any(Resource.class), any(Set.class));

        return mockAssetsProvider;
    }

    private void mockAsset(String assetPath) {
        // Mock asset
        Asset asset = Mockito.mock(Asset.class);
        Mockito.when(asset.getPath()).thenReturn(assetPath);

        // Mock rendition
        Rendition rendition = Mockito.mock(Rendition.class);
        Mockito.when(rendition.getAsset()).thenReturn(asset);

        // Mock image resource
        Calendar calendar = Mockito.mock(Calendar.class);
        Mockito.when(calendar.getTimeInMillis()).thenReturn(new Date().getTime());
        ValueMap valueMap = Mockito.mock(ValueMap.class);
        Mockito.when(valueMap.get(JCR_LASTMODIFIED, Calendar.class)).thenReturn(calendar);
        Resource imageResource = Mockito.mock(Resource.class);
        Mockito.when(imageResource.adaptTo(Asset.class)).thenReturn(asset);
        Mockito.when(imageResource.adaptTo(Rendition.class)).thenReturn(rendition);
        Mockito.when(imageResource.getValueMap()).thenReturn(valueMap);
        Mockito.when(imageResource.getPath()).thenReturn(assetPath);

        Mockito.when(resourceResolver.resolve(assetPath)).thenReturn(imageResource);
    }
}
