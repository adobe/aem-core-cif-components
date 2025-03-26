/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = EventListener.class, immediate = true)
public class InvalidateCacheEventListener implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidateCacheEventListener.class);

    @Reference
    private InvalidateCacheImpl invalidateCacheImpl;

    @Reference
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Reference
    private InvalidateCacheSupport invalidateCacheSupport;

    @Reference
    private SlingRepository repository;

    private Session session;

    @Reference(target = "(|(" + ServiceUserMapped.SUBSERVICENAME + "=" + InvalidateCacheSupport.SERVICE_USER + ")(!(subServiceName=*)))")
    private ServiceUserMapped serviceUserMapped;

    String pathDelimiter = "/";

    @Activate
    protected void activate() {
        try {
            LOGGER.info("Activating AuthorInvalidateCacheEventListener...");
            session = repository.loginService(InvalidateCacheSupport.SERVICE_USER, null);
            ObservationManager observationManager = session.getWorkspace().getObservationManager();
            observationManager.addEventListener(
                this,
                Event.NODE_ADDED,
                InvalidateCacheSupport.INVALIDATE_WORKING_AREA,
                true,
                null,
                null,
                false);
            LOGGER.info("Event listener registered for path: {}", InvalidateCacheSupport.INVALIDATE_WORKING_AREA);
        } catch (RepositoryException e) {
            LOGGER.error("Error registering JCR event listener: {}", e.getMessage(), e);
        }
    }

    @Deactivate
    protected void deactivate() {
        try {
            if (session != null) {
                ObservationManager observationManager = session.getWorkspace().getObservationManager();
                observationManager.removeEventListener(this);
                LOGGER.info("Event listener unregistered.");
            }
        } catch (RepositoryException e) {
            LOGGER.error("Error unregistering JCR event listener: {}", e.getMessage(), e);
        } finally {
            if (session != null) {
                session.logout();
                LOGGER.info("Session logged out.");
            }
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            try {
                String path = event.getPath();
                String actualPath = InvalidateCacheSupport.INVALIDATE_WORKING_AREA + pathDelimiter + InvalidateCacheSupport.NODE_NAME_BASE;
                if (path.startsWith(actualPath)) {
                    LOGGER.debug("Cache invalidation event detected: {} and {}", path, event.getType());
                    invalidateCacheImpl.invalidateCache(path);
                    if (Boolean.TRUE.equals(invalidateCacheSupport.getEnableDispatcherCacheInvalidation())) {
                        invalidateDispatcherCacheImpl.invalidateCache(path);
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error processing JCR event: {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Unexpected error processing JCR event: {}", e.getMessage(), e);
            }
        }
    }
}
