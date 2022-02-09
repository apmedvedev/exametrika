/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ScopeConfiguration} is a configuration for scopes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ScopeConfiguration extends Configuration {
    private final String name;
    private final String id;
    private final String type;
    private final String threadFilter;

    public ScopeConfiguration(String name, String id, String type, String threadFilter) {
        Assert.notNull(name);
        Assert.isTrue(!name.isEmpty());
        Assert.notNull(type);

        this.name = name;
        this.id = id;
        this.type = type;
        this.threadFilter = threadFilter;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getThreadFilter() {
        return threadFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ScopeConfiguration))
            return false;

        ScopeConfiguration configuration = (ScopeConfiguration) o;
        return name.equals(configuration.name) && Objects.equals(id, configuration.id) &&
                type.equals(configuration.type) && Objects.equals(threadFilter, configuration.threadFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, type, threadFilter);
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
