/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;


/**
 * The {@link IStandardExpressionContext} represents an expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IStandardExpressionContext extends IExpressionContext {
    /**
     * Replaces value's string representation by digest.
     *
     * @param value value
     * @return digested value's string representation
     */
    String hide(Object value);

    /**
     * Truncates object's string representation.
     *
     * @param value    value
     * @param length   maximum length
     * @param ellipsis if true ellipsis is added
     * @return truncated value's string representation
     */
    Object truncate(Object value, int length, boolean ellipsis);

    /**
     * Converts json value to builder.
     *
     * @param value value
     * @return json builder
     */
    JsonObjectBuilder json(JsonObject value);

    /**
     * Returns current thread name.
     *
     * @return current thread name
     */
    String getCurrentThread();

    /**
     * Returns current wall time.
     *
     * @return current wall time
     */
    long getWallTime();

    /**
     * Returns number of list elements.
     *
     * @param list list
     * @return number of list elements
     */
    int count(Iterable list);

    /**
     * Filters value by specified pattern.
     *
     * @param pattern glob or regexp filter pattern
     * @param value   value
     * @return true if value matched to pattern
     */
    boolean filter(String pattern, String value);
}
