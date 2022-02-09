/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.util.List;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.common.utils.Assert;


/**
 * The {@link CompositeJoinPointFilter} represents an composite join point filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CompositeJoinPointFilter implements IJoinPointFilter {
    protected final List<IJoinPointFilter> filters;

    public CompositeJoinPointFilter(List<IJoinPointFilter> filters) {
        Assert.notNull(filters);

        this.filters = filters;
    }

    @Override
    public boolean match(IJoinPoint joinPoint) {
        for (IJoinPointFilter filter : filters) {
            if (!filter.match(joinPoint))
                return false;
        }

        return true;
    }
}
