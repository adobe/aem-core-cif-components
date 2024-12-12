/*******************************************************************************
 *
 *    Copyright 2024 Adobe. All rights reserved.
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
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
    private SlingRepository repository;

    @Activate
    protected void activate() {
        try {
            LOGGER.info("Activating AuthorInvalidateCacheEventListener...");
            Session session = repository.loginService(InvalidateCacheSupport.SERVICE_USER, null);
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

    @Override
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event event = events.nextEvent();
            try {
                String path = event.getPath();
                String actualPath = InvalidateCacheSupport.INVALIDATE_WORKING_AREA + "/" + InvalidateCacheSupport.NODE_NAME_BASE;
                if (path.startsWith(actualPath)) {
                    LOGGER.debug("Cache invalidation event detected: {} and {}", path, event.getType());
                    invalidateCacheImpl.invalidateCache(path);
                    invalidateDispatcherCacheImpl.invalidateCache(path);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error processing JCR event: {}", e.getMessage(), e);
            }
        }
    }
}
