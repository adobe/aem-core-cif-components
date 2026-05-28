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

import java.lang.reflect.Method;

import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.core.testing.TestContext;
import io.wcm.testing.mock.aem.junit.AemContext;

public class VersionHistoryUtilsTest {

    private static final String VERSION_HISTORY_ROOT = "/tmp/versionhistory";

    @Rule
    public final AemContext context = TestContext.newAemContext();

    @Test
    public void testResolveNullResource() {
        Assert.assertNull(VersionHistoryUtils.resolveSourceResource(null));
    }

    @Test
    public void testResolveNonVersionHistoryResourceReturnsSameResource() {
        Resource resource = context.create().resource("/content/site/page");
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveUsingRelativePathCandidate() {
        context.create().page("/content/site/page");
        Resource resource = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version/site/page");
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals("/content/site/page", resolved.getPath());
    }

    @Test
    public void testResolveUsingRelativeChildPathReturnsSameResource() {
        context.create().page("/content/site/page");
        String childPath = VERSION_HISTORY_ROOT + "/hash/version/site/page/child";
        Resource resource = context.create().resource(childPath);
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals(childPath, resolved.getPath());
    }

    @Test
    public void testResolveInvalidVersionHistoryPathReturnsSameResource() {
        Resource resource = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version");
        Resource resourceWithTrailingSlash = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version/");

        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Resource resolvedWithTrailingSlash = VersionHistoryUtils.resolveSourceResource(resourceWithTrailingSlash);

        Assert.assertEquals(VERSION_HISTORY_ROOT + "/hash/version", resolved.getPath());
        Assert.assertEquals(VERSION_HISTORY_ROOT + "/hash/version", resolvedWithTrailingSlash.getPath());
    }

    @Test
    public void testResolveWithoutAnyCandidateReturnsSameResource() {
        Resource resource = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version/site/page");
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals(VERSION_HISTORY_ROOT + "/hash/version/site/page", resolved.getPath());
    }

    @Test
    public void testResolveInvalidVersionHistoryPathWithoutVersionIdReturnsSameResource() {
        Resource resource = context.create().resource(VERSION_HISTORY_ROOT + "/hashonly");
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals(VERSION_HISTORY_ROOT + "/hashonly", resolved.getPath());
    }

    @Test
    public void testResolveUnknownPathWithoutCandidateReturnsSameResource() {
        Resource resource = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version/unknown");
        Resource resolved = VersionHistoryUtils.resolveSourceResource(resource);
        Assert.assertEquals(VERSION_HISTORY_ROOT + "/hash/version/unknown", resolved.getPath());
    }

    @Test
    public void testIsVersionHistoryResource() {
        Resource versionResource = context.create().resource(VERSION_HISTORY_ROOT + "/hash/version/site/page");
        Resource contentResource = context.create().resource("/content/site/page");

        Assert.assertTrue(VersionHistoryUtils.isVersionPreviewResource(versionResource));
        Assert.assertFalse(VersionHistoryUtils.isVersionPreviewResource(contentResource));
        Assert.assertFalse(VersionHistoryUtils.isVersionPreviewResource(null));
    }

    @Test
    public void testGetSourcePagePathReturnsNullForVersionHistoryRoot() throws Exception {
        Method method = VersionHistoryUtils.class.getDeclaredMethod("getSourcePagePath", String.class);
        method.setAccessible(true);
        Assert.assertNull(method.invoke(null, VERSION_HISTORY_ROOT + "/"));
    }
}
