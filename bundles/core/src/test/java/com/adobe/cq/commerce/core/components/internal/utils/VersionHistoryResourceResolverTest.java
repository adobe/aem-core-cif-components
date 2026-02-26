/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2026 Adobe
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
package com.adobe.cq.commerce.core.components.internal.utils;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.testing.TestContext;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

public class VersionHistoryResourceResolverTest {

    @Rule
    public final AemContext context = TestContext.newAemContext();

    @Test
    public void testResolveNullResource() {
        Assert.assertNull(VersionHistoryResourceResolver.resolveSourceResource(null));
    }

    @Test
    public void testResolveNonVersionHistoryResourceReturnsSameResource() {
        Resource resource = context.create().resource("/content/site/page");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveFromCqSourcePathProperty() {
        context.create().resource("/content/site/source-page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/site/page",
            Collections.singletonMap("cq:sourcePath", "/content/site/source-page"));

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/source-page", resolved.getPath());
    }

    @Test
    public void testResolveFromSourcePathPropertyFallback() {
        context.create().resource("/content/site/source-page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/site/page",
            ImmutableMap.of(
                "cq:sourcePath", "",
                "sourcePath", "/content/site/source-page"));

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/source-page", resolved.getPath());
    }

    @Test
    public void testResolveFromJcrSourcePathPropertyFallback() {
        context.create().resource("/content/site/source-page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/site/page",
            ImmutableMap.of(
                "cq:sourcePath", " ",
                "sourcePath", "",
                "jcr:sourcePath", "/content/site/source-page"));

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/source-page", resolved.getPath());
    }

    @Test
    public void testResolveUsingRelativePathCandidate() {
        context.create().resource("/content/site/page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/content/site/page");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveUsingRelativePathParentFallback() {
        context.create().resource("/content/site/page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/content/site/page/child");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveUsingContentPrefixedFallback() {
        context.create().resource("/content/site/page");
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/site/page");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveInvalidVersionHistoryPathReturnsSameResource() {
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/tmp/versionhistory/hash/version", resolved.getPath());
    }

    @Test
    public void testResolveWithoutAnyCandidateReturnsSameResource() {
        Resource resource = context.create().resource("/tmp/versionhistory/hash/version/site/page");

        Resource resolved = VersionHistoryResourceResolver.resolveSourceResource(resource);

        Assert.assertEquals("/tmp/versionhistory/hash/version/site/page", resolved.getPath());
    }
}
