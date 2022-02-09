/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.List;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link ScopeHierarchy} is a hierarchy of scopes from parents to children.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ScopeHierarchy {
    private final List<IScopeName> scopes;

    public ScopeHierarchy(List<? extends IScopeName> scopes) {
        Assert.notNull(scopes);

        this.scopes = Immutables.wrap(scopes);
    }

    public List<IScopeName> getScopes() {
        return scopes;
    }

    @Override
    public String toString() {
        return scopes.toString();
    }
}
