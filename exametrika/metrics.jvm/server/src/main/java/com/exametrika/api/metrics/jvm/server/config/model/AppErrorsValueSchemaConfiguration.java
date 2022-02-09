/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AppErrorsValueSchemaConfiguration} is a application errors value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AppErrorsValueSchemaConfiguration extends ObjectValueSchemaConfiguration {
    private final String baseRepresentation;

    public AppErrorsValueSchemaConfiguration(String name, String baseRepresentation) {
        super(name);

        Assert.notNull(baseRepresentation);

        this.baseRepresentation = baseRepresentation;
    }

    public String getBaseRepresentation() {
        return baseRepresentation;
    }

    @Override
    public void buildBaseRepresentations(Set<String> baseRepresentations) {
        baseRepresentations.add(baseRepresentation);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AppErrorsValueSchemaConfiguration))
            return false;

        AppErrorsValueSchemaConfiguration configuration = (AppErrorsValueSchemaConfiguration) o;
        return super.equals(o) && baseRepresentation.equals(configuration.baseRepresentation);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(baseRepresentation);
    }
}
