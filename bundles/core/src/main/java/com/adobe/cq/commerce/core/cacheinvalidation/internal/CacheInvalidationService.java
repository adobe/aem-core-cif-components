package com.adobe.cq.commerce.core.cacheinvalidation.internal;

import com.adobe.cq.commerce.core.cacheinvalidation.config.CacheInvalidationConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Component(service = CacheInvalidationService.class, immediate = true)
@Designate(ocd = CacheInvalidationConfig.class)
public class CacheInvalidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationService.class);

    private boolean enableCacheInvalidation;

    @Activate
    @Modified
    protected void activate(CacheInvalidationConfig config) {
        this.enableCacheInvalidation = config.enableCacheInvalidation();
        LOGGER.info("Cache Invalidation enabled: {}", enableCacheInvalidation);
    }

    public boolean isCacheInvalidationEnabled() {
        return enableCacheInvalidation;
    }
}