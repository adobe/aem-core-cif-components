package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.adobe.cq.commerce.core.cacheinvalidation.config.CacheInvalidationConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


public class CacheInvalidationServiceTest {

    @InjectMocks
    private CacheInvalidationService cacheInvalidationService;

    @Mock
    private CacheInvalidationConfig config;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testActivate() {
        when(config.enableCacheInvalidation()).thenReturn(true);

        cacheInvalidationService.activate(config);

        assertTrue(cacheInvalidationService.isCacheInvalidationEnabled());
    }

    @Test
    public void testActivate_DisableCacheInvalidation() {
        when(config.enableCacheInvalidation()).thenReturn(false);

        cacheInvalidationService.activate(config);

        assertFalse(cacheInvalidationService.isCacheInvalidationEnabled());
    }

}