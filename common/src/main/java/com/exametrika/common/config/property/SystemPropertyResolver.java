/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config.property;

import com.exametrika.common.utils.Assert;


/**
 * The {@link SystemPropertyResolver} is an implementation of {@link IPropertyResolver} that uses system properties and environment
 * variables.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SystemPropertyResolver implements IPropertyResolver {
    @Override
    public String resolveProperty(String propertyName) {
        Assert.notNull(propertyName);

        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null)
            return propertyValue;

        return System.getenv(propertyName);
    }
}
