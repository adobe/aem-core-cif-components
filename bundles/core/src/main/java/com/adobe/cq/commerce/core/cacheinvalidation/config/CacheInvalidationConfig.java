package com.adobe.cq.commerce.core.cacheinvalidation.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Cache Invalidation Configuration")
public @interface CacheInvalidationConfig {

    @AttributeDefinition(
        name = "Enable Cache Invalidation",
        description = "Check to enable cache invalidation",
        type = AttributeType.BOOLEAN
    )
    boolean enableCacheInvalidation() default false;
}