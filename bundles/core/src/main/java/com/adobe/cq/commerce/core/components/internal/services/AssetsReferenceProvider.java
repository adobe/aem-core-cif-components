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

import javax.jcr.Node;

import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.internal.models.v1.AssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.categorylist.CategoryListAssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.productcarousel.ProductCarouselAssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.productteaser.ProductTeaserAssetsProvider;
import com.adobe.cq.commerce.core.components.internal.models.v1.relatedproducts.RelatedProductsAssetsProvider;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LASTMODIFIED;

@Component(service = { ReferenceProvider.class })
public class AssetsReferenceProvider implements ReferenceProvider {

    List<AssetsProvider> assetsProviders = new ArrayList<AssetsProvider>() {
        {
            add(new ProductTeaserAssetsProvider());
            add(new ProductCarouselAssetsProvider());
            add(new CategoryListAssetsProvider());
            add(new RelatedProductsAssetsProvider());
        }
    };

    public List<Reference> findReferences(Resource resource) {
        List<Reference> references = new ArrayList<>();
        ResourceResolver resourceResolver = resource.getResourceResolver();
        for (String assetPath : collectAssets(resource, new HashSet<String>())) {
            addReference(resourceResolver, assetPath, references);
        }
        return references;
    }

    private Set<String> collectAssets(Resource resource, Set<String> assets) {
        Node node = resource.adaptTo(Node.class);
        if (node == null)
            return assets;

        for (AssetsProvider assetsProvider : assetsProviders) {
            if (assetsProvider.canHandle(resource)) {
                assetsProvider.addAssetPaths(resource, assets);
                return assets;
            }
        }

        Iterator<Resource> it = resource.listChildren();
        while (it.hasNext()) {
            collectAssets(it.next(), assets);
        }

        return assets;
    }

    private Asset getAemAsset(Resource imageResource) {
        Asset asset = imageResource.adaptTo(Asset.class);
        if (asset == null) {
            Rendition rendition = imageResource.adaptTo(Rendition.class);
            asset = rendition != null ? rendition.getAsset() : null;
        }
        return asset;
    }

    private void addReference(ResourceResolver resourceResolver, String imageUri, List<Reference> references) {
        Resource imageResource = resourceResolver.resolve(imageUri);
        if (!(imageResource instanceof NonExistingResource)) {
            Asset asset = getAemAsset(imageResource);
            addReference(resourceResolver.resolve(asset != null ? asset.getPath() : imageUri), references);
        }
    }

    private void addReference(Resource imageResource, List<Reference> references) {
        final Calendar mod = ResourceUtil.getValueMap(imageResource).get(JCR_LASTMODIFIED, Calendar.class);
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        references.add(new Reference("asset", imageResource.getName(), imageResource, lastModified));
    }
}
