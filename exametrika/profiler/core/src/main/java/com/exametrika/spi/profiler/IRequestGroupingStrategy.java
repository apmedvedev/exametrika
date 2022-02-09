/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;


/**
 * The {@link IRequestGroupingStrategy} is a request grouping strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IRequestGroupingStrategy {
    /**
     * Returns request group name.
     *
     * @param scope   scope
     * @param request request
     * @param name    name of request or request group of current level
     * @param level   current level starting from 0
     * @return request group name or null if current request is root
     */
    String getRequestGroupName(IScope scope, Object request, String name, int level);
}
