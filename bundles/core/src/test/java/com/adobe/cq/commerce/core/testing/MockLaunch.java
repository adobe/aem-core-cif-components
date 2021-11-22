/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
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
package com.adobe.cq.commerce.core.testing;

import java.util.Calendar;
import java.util.List;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.launches.api.LaunchPromotionScope;
import com.adobe.cq.launches.api.LaunchSource;
import com.day.cq.commons.jcr.JcrConstants;

public class MockLaunch implements Launch {

    private Resource resource;
    private Resource jcrConstant;

    public MockLaunch(Resource resource) {
        this.resource = resource;
        this.jcrConstant = resource.getChild(JcrConstants.JCR_CONTENT);
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public Resource getRootResource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<LaunchSource> getLaunchSources() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getSourceRootResource() {
        String sourceRootResPath = jcrConstant.getValueMap().get("sourceRootResource", String.class);
        return sourceRootResPath != null ? resource.getResourceResolver().getResource(sourceRootResPath) : null;
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getLiveDate() {
        return jcrConstant.getValueMap().get("liveDate", Calendar.class);
    }

    @Override
    public boolean isProductionReady() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLiveCopy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeep() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getCreated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCreatedBy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getModified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getModifiedBy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getLastPromoted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastPromotedBy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsResource(Resource productionResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Launch launch) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LaunchPromotionScope getLaunchAutoPromotionScope() {
        throw new UnsupportedOperationException();
    }
}
