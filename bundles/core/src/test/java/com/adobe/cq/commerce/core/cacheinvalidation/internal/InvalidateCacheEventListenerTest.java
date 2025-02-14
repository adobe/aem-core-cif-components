/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class InvalidateCacheEventListenerTest {

    @Mock
    private InvalidateCacheImpl invalidateCacheImpl;

    @Mock
    private InvalidateDispatcherCacheImpl invalidateDispatcherCacheImpl;

    @Mock
    private InvalidateCacheSupport invalidateCacheSupport;

    @Mock
    private SlingRepository repository;

    @Mock
    private Session session;

    @Mock
    private ObservationManager observationManager;

    @Mock
    private EventIterator eventIterator;

    @Mock
    private Event event;

    @InjectMocks
    private InvalidateCacheEventListener eventListener;

    @Before
    public void setUp() throws RepositoryException {
        MockitoAnnotations.initMocks(this);
        when(repository.loginService(anyString(), isNull(String.class))).thenReturn(session);
        when(session.getWorkspace()).thenReturn(mock(javax.jcr.Workspace.class));
        when(session.getWorkspace().getObservationManager()).thenReturn(observationManager);
    }

    @Test
    public void testActivate() throws RepositoryException {
        eventListener.activate();
        verify(observationManager).addEventListener(
            eq(eventListener),
            eq(Event.NODE_ADDED),
            eq(InvalidateCacheSupport.INVALIDATE_WORKING_AREA),
            eq(true),
            isNull(String[].class),
            isNull(String[].class),
            eq(false));
    }

    @Test
    public void testDeactivate() throws RepositoryException {
        eventListener.activate();
        eventListener.deactivate();
        verify(observationManager).removeEventListener(eventListener);
        verify(session).logout();
    }

    @Test
    public void testOnEvent() throws RepositoryException {
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(event);
        when(event.getPath()).thenReturn(
            InvalidateCacheSupport.INVALIDATE_WORKING_AREA + "/" + InvalidateCacheSupport.NODE_NAME_BASE);

        eventListener.onEvent(eventIterator);

        verify(invalidateCacheImpl).invalidateCache(anyString());
    }
}
