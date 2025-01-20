/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2025 Adobe
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
package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import java.lang.reflect.Field;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

public class InvalidateCacheEventListenerTest {

    @Mock
    private InvalidateCacheImpl invalidateCacheImpl;

    @Mock
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Mock
    private SlingRepository repository;

    @Mock
    private Logger logger;

    @InjectMocks
    private InvalidateCacheEventListener listener;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Use reflection to set the private static final LOGGER field to the mock logger
        Field loggerField = InvalidateCacheEventListener.class.getDeclaredField("LOGGER");
        loggerField.setAccessible(true);
        loggerField.set(null, logger);

    }

    @Test
    public void testActivate() throws RepositoryException {
        Session session = mock(Session.class);
        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);

        when(repository.loginService(anyString(), any())).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getObservationManager()).thenReturn(observationManager);

        listener.activate();

        verify(observationManager).addEventListener((EventListener) eq(listener), eq(Event.NODE_ADDED), anyString(), eq(true),
            (String[]) isNull(), (String[]) isNull(), eq(false));
    }

    @Test
    public void testOnEvent() throws RepositoryException {
        EventIterator events = mock(EventIterator.class);
        Event event = mock(Event.class);
        when(events.hasNext()).thenReturn(true, false);
        when(events.nextEvent()).thenReturn(event);
        when(event.getPath()).thenReturn("/var/cif/cacheInvalidation/invalidate_entry");

        listener.onEvent(events);

        verify(invalidateCacheImpl).invalidateCache("/var/cif/cacheInvalidation/invalidate_entry");
        verify(invalidateDispatcherCacheImpl).invalidateCache("/var/cif/cacheInvalidation/invalidate_entry");
    }

    @Test
    public void testActivateWithRepositoryException() throws RepositoryException {
        when(repository.loginService(anyString(), any())).thenThrow(new RepositoryException("Test RepositoryException"));

        listener.activate();

        verify(logger).error(eq("Error registering JCR event listener: {}"), eq("Test RepositoryException"), any(
            RepositoryException.class));
    }

    @Test
    public void testOnEventWithRepositoryException() throws RepositoryException {
        EventIterator events = mock(EventIterator.class);
        Event event = mock(Event.class);
        when(events.hasNext()).thenReturn(true, false);
        when(events.nextEvent()).thenReturn(event);
        when(event.getPath()).thenThrow(new RepositoryException("Test RepositoryException"));

        listener.onEvent(events);

        verify(logger).error(eq("Error processing JCR event: {}"), eq("Test RepositoryException"), any(RepositoryException.class));
    }

}
