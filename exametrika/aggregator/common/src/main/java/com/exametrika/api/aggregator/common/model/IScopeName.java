/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.util.List;


/**
 * The {@link IScopeName} represents a structured scope name, consisting of a hierarchical domain consisting of a list of segments.
 * <p>
 * String representation of a name consists of a list of segments separated by period.
 * Each domain segment can not be empty. Periods can be escaped by doubling.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IScopeName extends IName {
    /**
     * Is name empty?
     *
     * @return true if name is empty
     */
    @Override
    boolean isEmpty();

    /**
     * Returns name segments.
     *
     * @return name segments
     */
    List<String> getSegments();

    /**
     * Returns last segment.
     *
     * @return last segment
     */
    String getLastSegment();

    /**
     * Indicates that this name starts with specified prefix.
     *
     * @param prefix name prefix
     * @return true if this name starts with specified prefix
     */
    boolean startsWith(IScopeName prefix);
}
