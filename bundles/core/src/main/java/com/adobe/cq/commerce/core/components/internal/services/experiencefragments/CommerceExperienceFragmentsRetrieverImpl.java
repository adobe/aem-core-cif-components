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
package com.adobe.cq.commerce.core.components.internal.services.experiencefragments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RangeIterator;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.core.components.models.experiencefragment.CommerceExperienceFragment;
import com.adobe.cq.commerce.core.components.services.experiencefragments.CommerceExperienceFragmentsRetriever;
import com.day.cq.wcm.api.LanguageManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveCopy;
import com.day.cq.wcm.msm.api.LiveRelationship;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

@Component(service = { CommerceExperienceFragmentsRetriever.class, CommerceExperienceFragmentsRetrieverImpl.class })
public class CommerceExperienceFragmentsRetrieverImpl implements CommerceExperienceFragmentsRetriever {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceExperienceFragmentsRetrieverImpl.class);
    private static final String XF_ROOT = "/content/experience-fragments/";

    @Reference
    private LanguageManager languageManager;

    @Reference
    private LiveRelationshipManager relationshipManager;

    @Override
    public List<Resource> getExperienceFragmentsForProduct(String sku, String fragmentLocation, Page currentPage) {
        if (StringUtils.isBlank(sku)) {
            LOGGER.warn("Cannot find product for current request");
            return Collections.emptyList();
        }

        String query = buildQueryForProduct(sku, fragmentLocation, currentPage);
        return findExperienceFragments(query, currentPage.getContentResource().getResourceResolver());
    }

    @Override
    public List<Resource> getExperienceFragmentsForCategory(String categoryUid, String fragmentLocation,
        Page currentPage) {
        if (StringUtils.isBlank(categoryUid)) {
            LOGGER.warn("Cannot find category for current request");
            return Collections.emptyList();
        }

        String query = buildQueryForCategory(categoryUid, fragmentLocation, currentPage);
        return findExperienceFragments(query, currentPage.getContentResource().getResourceResolver());
    }

    private String buildQueryForProduct(String sku, String fragmentLocation, Page currentPage) {
        // This query is backed up by an index
        final String PRODUCT_QUERY_TEMPLATE = "SELECT * FROM [cq:PageContent] as node WHERE ISDESCENDANTNODE('%s') "
            + "AND (node.[" + CommerceExperienceFragment.PN_CQ_PRODUCTS + "] = '%s' OR node.[" + CommerceExperienceFragment.PN_CQ_PRODUCTS
            + "] LIKE '%s#%%') "
            + "AND node.[" + CommerceExperienceFragment.PN_FRAGMENT_LOCATION + "] ";

        String query = String.format(PRODUCT_QUERY_TEMPLATE, getExperienceFragmentsRoot(currentPage), sku, sku);
        if (fragmentLocation != null) {
            query += "= '" + fragmentLocation + "'";
        } else {
            query += "IS NULL";
        }

        return query;
    }

    private String buildQueryForCategory(String categoryId, String fragmentLocation, Page currentPage) {
        final String CATEGORY_QUERY_TEMPLATE = "SELECT * FROM [cq:PageContent] as node WHERE ISDESCENDANTNODE('%s') "
            + "AND node.[" + CommerceExperienceFragment.PN_CQ_CATEGORIES + "] = '%s' "
            + "AND node.[" + CommerceExperienceFragment.PN_FRAGMENT_LOCATION + "] ";

        String query = String.format(CATEGORY_QUERY_TEMPLATE, getExperienceFragmentsRoot(currentPage), categoryId);
        if (fragmentLocation != null) {
            query += "= '" + fragmentLocation + "'";
        } else {
            query += "IS NULL";
        }

        return query;
    }

    private String getExperienceFragmentsRoot(Page currentPage) {
        String localizationRoot = getLocalizationRoot(currentPage.getPath(), currentPage.getContentResource().getResourceResolver());
        return localizationRoot != null ? localizationRoot.replace("/content/", XF_ROOT) : XF_ROOT;
    }

    private List<Resource> findExperienceFragments(String query, ResourceResolver resolver) {
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

        return experienceFragments;
    }

    /**
     * Returns the localization root of the resource defined at the given path.
     *
     * @param path the resource path
     * @return the localization root of the resource at the given path if it exists,
     *         {@code null} otherwise
     */
    private String getLocalizationRoot(String path, ResourceResolver resolver) {
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
     * @return the language root of the resource if it exists, {@code null}
     *         otherwise
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
     * @return the path of the blueprint of the resource if it exists, {@code null}
     *         otherwise
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
     * @return the path of the live copy of the resource if it exists, {@code null}
     *         otherwise
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
