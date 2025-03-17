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
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
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
    private Workspace workspace;

    @Mock
    private ObservationManager observationManager;

    @Mock
    private Event event;

    @Mock
    private EventIterator eventIterator;

    @InjectMocks
    private InvalidateCacheEventListener listener;

    private static final String TEST_PATH = InvalidateCacheSupport.INVALIDATE_WORKING_AREA + "/" + InvalidateCacheSupport.NODE_NAME_BASE
        + "/test";

    @Before
    public void setUp() throws RepositoryException {
        when(repository.loginService(eq(InvalidateCacheSupport.SERVICE_USER), any())).thenReturn(session);
        when(session.getWorkspace()).thenReturn(workspace);
        when(workspace.getObservationManager()).thenReturn(observationManager);
    }

    @Test
    public void testActivate() throws RepositoryException {
        // Execute
        listener.activate();

        // Verify
        verify(repository).loginService(eq(InvalidateCacheSupport.SERVICE_USER), any());
        verify(observationManager).addEventListener(
            eq(listener),
            eq(Event.NODE_ADDED),
            eq(InvalidateCacheSupport.INVALIDATE_WORKING_AREA),
            eq(true),
            eq(null),
            eq(null),
            eq(false));
    }

    @Test
    public void testDeactivate() throws RepositoryException {
        // Setup
        listener.activate();

        // Execute
        listener.deactivate();

        // Verify
        verify(observationManager).removeEventListener(listener);
        verify(session).logout();
    }

    @Test
    public void testOnEventWithDispatcherEnabled() throws RepositoryException {
        // Setup
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(event);
        when(event.getPath()).thenReturn(TEST_PATH);
        when(invalidateCacheSupport.getEnableDispatcherCacheInvalidation()).thenReturn(true);

        // Execute
        listener.onEvent(eventIterator);

        // Verify
        verify(invalidateCacheImpl).invalidateCache(TEST_PATH);
        verify(invalidateDispatcherCacheImpl).invalidateCache(TEST_PATH);
    }

    @Test
    public void testOnEventWithDispatcherDisabled() throws RepositoryException {
        // Setup
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(event);
        when(event.getPath()).thenReturn(TEST_PATH);
        when(invalidateCacheSupport.getEnableDispatcherCacheInvalidation()).thenReturn(false);

        // Execute
        listener.onEvent(eventIterator);

        // Verify
        verify(invalidateCacheImpl).invalidateCache(TEST_PATH);
        verify(invalidateDispatcherCacheImpl, never()).invalidateCache(anyString());
    }

    @Test
    public void testOnEventWithRepositoryException() throws RepositoryException {
        // Setup
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(event);
        when(event.getPath()).thenThrow(new RepositoryException("Test exception"));

        // Execute
        listener.onEvent(eventIterator);

        // Verify no cache invalidation occurred
        verify(invalidateCacheImpl, never()).invalidateCache(anyString());
        verify(invalidateDispatcherCacheImpl, never()).invalidateCache(anyString());
    }

    @Test
    public void testOnEventWithInvalidPath() throws RepositoryException {
        // Setup
        when(eventIterator.hasNext()).thenReturn(true, false);
        when(eventIterator.nextEvent()).thenReturn(event);
        when(event.getPath()).thenReturn("/invalid/path");

        // Execute
        listener.onEvent(eventIterator);

        // Verify no cache invalidation occurred
        verify(invalidateCacheImpl, never()).invalidateCache(anyString());
        verify(invalidateDispatcherCacheImpl, never()).invalidateCache(anyString());
    }
}
