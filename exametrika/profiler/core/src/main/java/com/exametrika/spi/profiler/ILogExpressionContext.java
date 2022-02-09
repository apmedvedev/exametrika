/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link ILogExpressionContext} represents a expression log context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILogExpressionContext extends IExpressionContext {
    /**
     * Normalizes log level.
     *
     * @param level logger specific level
     * @return normalized level
     */
    String normalizeLevel(String level);
}
