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

package com.adobe.cq.commerce.core.components.internal.models.v1.experiencefragment;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RangeIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.services.UrlProvider;
import com.adobe.cq.commerce.core.components.services.UrlProvider.ProductIdentifierType;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = CommerceExperienceFragment.class,
    resourceType = CommerceExperienceFragmentImpl.RESOURCE_TYPE,
    cache = true)
public class CommerceExperienceFragmentImpl implements CommerceExperienceFragment {

    protected static final String RESOURCE_TYPE = "core/cif/components/commerce/experiencefragment/v1/experiencefragment";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceExperienceFragmentImpl.class);

    // This query is backed up by an index
    private static final String QUERY_TEMPLATE = "SELECT * FROM [cq:PageContent] as node WHERE ISDESCENDANTNODE('%s') "
        + "AND node.[" + PN_CQ_PRODUCTS + "] = '%s' AND node.[" + PN_FRAGMENT_LOCATION + "] ";

    @Self
    private SlingHttpServletRequest request;

    @ValueMapValue(name = PN_FRAGMENT_LOCATION, injectionStrategy = InjectionStrategy.OPTIONAL)
    private String fragmentLocation;

    @ScriptVariable
    private Page currentPage;

    @Inject
    protected Resource resource;

    @SlingObject
    private ResourceResolver resolver;

    @Inject
    private UrlProvider urlProvider;

    @Inject
    private LanguageManager languageManager;

    @Inject
    private LiveRelationshipManager relationshipManager;

    private Resource xfResource;
    private String name;

    @PostConstruct
    private void initModel() {

        // Parse identifier in URL
        Pair<ProductIdentifierType, String> identifier = urlProvider.getProductIdentifier(request);
        String sku = null;

        if (ProductIdentifierType.SKU.equals(identifier.getLeft())) {
            sku = identifier.getRight();
        } else if (SiteNavigation.isProductPage(currentPage)) {
            Product product = request.adaptTo(Product.class);
            if (product.getFound()) {
                sku = product.getSku();
            }
        }

        if (sku == null) {
            LOGGER.warn("Cannot find sku or product for current request");
            return;
        }

        String localizationRoot = getLocalizationRoot(currentPage.getPath());
        String xfRoot = localizationRoot.replace("/content/", "/content/experience-fragments/");

        String query = String.format(QUERY_TEMPLATE, xfRoot, sku);
        if (fragmentLocation != null) {
            query += "= '" + fragmentLocation + "'";
        } else {
            query += "IS NULL";
        }

        List<Resource> xfs = findExperienceFragments(query);
        if (!xfs.isEmpty()) {
            xfResource = xfs.get(0);
            resolveName();
        }
    }

    private List<Resource> findExperienceFragments(String query) {
        LOGGER.debug("Looking for experience fragments with query: {}", query);

        List<Resource> experienceFragments = new ArrayList<>();
        try {
            Session session = resolver.adaptTo(Session.class);
            Workspace workspace = session.getWorkspace();
            QueryManager qm = workspace.getQueryManager();
            Query jcrQuery = qm.createQuery(query, "JCR-SQL2");
            QueryResult result = jcrQuery.execute();
            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                Resource resource = resolver.getResource(node.getPath());
                if (resource != null) {
                    experienceFragments.add(resource);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error looking for experience fragments", e);
        }

        if (experienceFragments.size() > 1) {
            LOGGER.warn("Found multiple experience fragments matching {} with location {}", request.getRequestURI(), fragmentLocation);
        }

        return experienceFragments;
    }

    private void resolveName() {
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page xfVariationPage = pageManager.getPage(xfResource.getParent().getPath());
        if (xfVariationPage != null) {
            Page xfPage = xfVariationPage.getParent();
            if (xfPage != null) {
                name = xfPage.getName();
            }
        }
    }

    @Override
    public Resource getExperienceFragmentResource() {
        return xfResource;
    }

    @Override
    public String getName() {
        return name;
    }

    // All the methods below are copied from the WCM ExperienceFragmentImpl class
    // and will be OSGi-exported in a new public class in a next release

    /**
     * Returns the localization root of the resource defined at the given path.
     *
     * @param path the resource path
     * @return the localization root of the resource at the given path if it exists, {@code null} otherwise
     */
    private String getLocalizationRoot(String path) {
        String root = null;
        if (StringUtils.isNotEmpty(path)) {
            Resource resource = resolver.getResource(path);
            root = getLanguageRoot(resource);
            if (StringUtils.isEmpty(root)) {
                root = getBlueprintPath(resource);
            }
            if (StringUtils.isEmpty(root)) {
                root = getLiveCopyPath(resource);
            }
        }
        return root;
    }

    /**
     * Returns the language root of the resource.
     *
     * @param resource the resource
     * @return the language root of the resource if it exists, {@code null} otherwise
     */
    private String getLanguageRoot(Resource resource) {
        Page rootPage = languageManager.getLanguageRoot(resource);
        if (rootPage != null) {
            return rootPage.getPath();
        }
        return null;
    }

    /**
     * Returns the path of the blueprint of the resource.
     *
     * @param resource the resource
     * @return the path of the blueprint of the resource if it exists, {@code null} otherwise
     */
    private String getBlueprintPath(Resource resource) {
        try {
            if (relationshipManager.isSource(resource)) {
                // the resource is a blueprint
                RangeIterator liveCopiesIterator = relationshipManager.getLiveRelationships(resource, null, null);
                if (liveCopiesIterator != null) {
                    LiveRelationship relationship = (LiveRelationship) liveCopiesIterator.next();
                    LiveCopy liveCopy = relationship.getLiveCopy();
                    if (liveCopy != null) {
                        return liveCopy.getBlueprintPath();
                    }
                }
            }
        } catch (WCMException e) {
            LOGGER.error("Unable to get the blueprint: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Returns the path of the live copy of the resource.
     *
     * @param resource the resource
     * @return the path of the live copy of the resource if it exists, {@code null} otherwise
     */
    private String getLiveCopyPath(Resource resource) {
        try {
            if (relationshipManager.hasLiveRelationship(resource)) {
                // the resource is a live copy
                LiveRelationship liveRelationship = relationshipManager.getLiveRelationship(resource, false);
                if (liveRelationship != null) {
                    LiveCopy liveCopy = liveRelationship.getLiveCopy();
                    if (liveCopy != null) {
                        return liveCopy.getPath();
                    }
                }
            }
        } catch (WCMException e) {
            LOGGER.error("Unable to get the live copy: {}", e.getMessage());
        }
        return null;
    }
}
