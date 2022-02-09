/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;

/**
 * The {@link DomainServiceConfiguration} represents a configuration of domain service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DomainServiceConfiguration extends Configuration {
    private final String name;

    public DomainServiceConfiguration(String name) {
        Assert.notNull(name);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isCompatible(DomainServiceConfiguration configuration) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DomainServiceConfiguration))
            return false;

        DomainServiceConfiguration configuration = (DomainServiceConfiguration) o;
        return name.equals(configuration.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
