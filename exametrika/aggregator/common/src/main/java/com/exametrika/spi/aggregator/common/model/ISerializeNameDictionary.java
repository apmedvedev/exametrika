/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.model;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;


/**
 * The {@link ISerializeNameDictionary} is a dictionary of names used for serialization optimization.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface ISerializeNameDictionary {
    /**
     * Are internal persistent measurement identifiers converted to names?
     *
     * @return true if identifiers are converted to names
     */
    boolean convertIdsToNames();

    /**
     * Returns name by persistent identifier.
     *
     * @param persistentNameId persistent name identifier
     * @return name or null if id is 0 (name is root)
     */
    IName getName(long persistentNameId);

    /**
     * Returns session identifier of scope.
     *
     * @param name scope name
     * @return session identifier of scope starting from 0 or -1 if scope is not found in session
     */
    long getScopeId(IScopeName name);

    /**
     * Adds scope to session.
     *
     * @param name scope name
     * @return session identifier of scope starting from 0
     */
    long putScope(IScopeName name);

    /**
     * Returns session identifier of metric.
     *
     * @param name metric name
     * @return session identifier of metric starting from 0 or -1 if metric is not found in session
     */
    long getMetricId(IMetricName name);

    /**
     * Adds metric to session.
     *
     * @param name metric name
     * @return session identifier of metric starting from 0
     */
    long putMetric(IMetricName name);

    /**
     * Returns session identifier of callpath.
     *
     * @param name callpath name
     * @return session identifier of callpath starting from 0 or -1 if callpath is not found in session
     */
    long getCallPathId(ICallPath name);

    /**
     * Adds callpath to session.
     *
     * @param name callpath name
     * @return session identifier of callpath starting from 0
     */
    long putCallPath(ICallPath name);
}
