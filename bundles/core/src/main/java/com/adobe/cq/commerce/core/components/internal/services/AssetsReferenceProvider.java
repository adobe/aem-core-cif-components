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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.core.components.models.categorylist.FeaturedCategoryList;
import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productcarousel.ProductCarousel;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.wcm.api.reference.Reference;
import com.day.cq.wcm.api.reference.ReferenceProvider;

import static com.day.cq.commons.jcr.JcrConstants.JCR_LASTMODIFIED;

/**
 *
 */
@Component(service = { ReferenceProvider.class })
public class AssetsReferenceProvider implements ReferenceProvider {

    private static final String PRODUCTTEASER_RT = "core/cif/components/commerce/productteaser/v1/productteaser";
    private static final String PRODUCTCAROUSEL_RT = "core/cif/components/commerce/productcarousel/v1/productcarousel";
    private static final String FEATUREDCATEGORY_RT = "core/cif/components/commerce/featuredcategorylist/v1/featuredcategorylist";

    public List<Reference> findReferences(Resource resource) {
        return recurse(resource, new ArrayList<Reference>());
    }

    private List<Reference> recurse(Resource resource, ArrayList<Reference> references) {
        Node node = resource.adaptTo(Node.class);
        if (node == null)
            return references;

        if (resource.isResourceType(PRODUCTTEASER_RT)) {
            ProductTeaser productTeaser = resource.adaptTo(ProductTeaser.class);
            String image = productTeaser != null ? productTeaser.getImage() : null;
            addReference(resource.getResourceResolver(), image, references);
            return references;
        } else if (resource.isResourceType(PRODUCTCAROUSEL_RT)) {
            ProductCarousel productCarousel = resource.adaptTo(ProductCarousel.class);
            if (productCarousel != null) {
                for (ProductListItem item : productCarousel.getProducts()) {
                    addReference(resource.getResourceResolver(), item.getImageURL(), references);
                }
            }
            return references;
        } else if (resource.isResourceType(FEATUREDCATEGORY_RT)) {
            FeaturedCategoryList featuredCategoryList = resource.adaptTo(FeaturedCategoryList.class);
            if (featuredCategoryList != null) {
                for (CategoryTree item : featuredCategoryList.getCategories()) {
                    addReference(resource.getResourceResolver(), item.getImage(), references);
                }
            }
            return references;
        }

        Iterator<Resource> it = resource.listChildren();
        while (it.hasNext()) {
            recurse(it.next(), references);
        }

        return references;
    }

    private Asset getAemAsset(Resource imageResource) {
        Asset asset = imageResource.adaptTo(Asset.class);
        if (asset == null) {
            Rendition rendition = imageResource.adaptTo(Rendition.class);
            asset = rendition != null ? rendition.getAsset() : null;
        }
        return asset;
    }

    private boolean isAemAsset(String imageUri) {
        return StringUtils.isNotBlank(imageUri) && imageUri.startsWith("/content");
    }

    private void addReference(ResourceResolver resourceResolver, String imageUri, ArrayList<Reference> references) {
        if (!isAemAsset(imageUri))
            return;

        Resource imageResource = resourceResolver.resolve(imageUri);
        if (!(imageResource instanceof NonExistingResource)) {
            Asset asset = getAemAsset(imageResource);
            addReference(resourceResolver.resolve(asset != null ? asset.getPath() : imageUri), references);
        }
    }

    private void addReference(Resource imageResource, ArrayList<Reference> references) {
        final Calendar mod = ResourceUtil.getValueMap(imageResource).get(JCR_LASTMODIFIED, Calendar.class);
        long lastModified = mod != null ? mod.getTimeInMillis() : -1;
        references.add(new Reference("asset", imageResource.getName(), imageResource, lastModified));
    }
}
