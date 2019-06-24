package com.adobe.cq.commerce.core.components.models.categorylist;

import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

import com.adobe.cq.commerce.magento.graphql.CategoryInterface;

@ProviderType
public interface FeaturedCategoryList {

    List<CategoryInterface> getCategories();

}
