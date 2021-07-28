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
package com.adobe.cq.commerce.core;

import java.util.Calendar;
import java.util.List;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.launches.api.Launch;
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<LaunchSource> getLaunchSources() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getSourceRootResource() {
        String sourceRootResPath = jcrConstant.getValueMap().get("sourceRootResource", String.class);
        return sourceRootResPath != null ? resource.getResourceResolver().getResource(sourceRootResPath) : null;
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Calendar getLiveDate() {
        return jcrConstant.getValueMap().get("liveDate", Calendar.class);
    }

    @Override
    public boolean isProductionReady() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLiveCopy() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDeep() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Calendar getCreated() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCreatedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Calendar getModified() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getModifiedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Calendar getLastPromoted() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLastPromotedBy() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean containsResource(Resource productionResource) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int compareTo(Launch launch) {
        // TODO Auto-generated method stub
        return 0;
    }

}
