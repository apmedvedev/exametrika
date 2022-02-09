/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.util.List;


/**
 * The {@link ICallPath} represents a structured metric callpath, consisting of a set of segments.
 * <p>
 * String representation of a callpath consists of a list of metric name segments separated by {@link #SEPARATOR}.
 * <p>
 * Each segment can not be empty and can not be {@link #SEPARATOR}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface ICallPath extends IMetricLocation {
    /**
     * Segment separator.
     */
    char SEPARATOR = '⟶';

    /**
     * Is callpath empty?
     *
     * @return true if callpath is empty
     */
    @Override
    boolean isEmpty();

    /**
     * Returns callpath segments.
     *
     * @return callpath segments
     */
    List<IMetricName> getSegments();

    /**
     * Returns last segment.
     *
     * @return last segment
     */
    IMetricName getLastSegment();

    /**
     * Returns parent callpath.
     *
     * @return parent callpath or null if call path is root
     */
    ICallPath getParent();

    /**
     * Returns child callpath.
     *
     * @param segment last segment of child call path
     * @return child callpath
     */
    ICallPath getChild(IMetricName segment);
}
