/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.model;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;


/**
 * The {@link IDeserializeNameDictionary} is a dictionary of names used for deserialization optimization.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface IDeserializeNameDictionary {
    /**
     * Adds scope to session.
     *
     * @param sessionScopeId session identifier of scope
     * @param name           scope name
     * @return persistent identifier of scope
     */
    long putScope(long sessionScopeId, IScopeName name);

    /**
     * Returns persistent identifier of scope.
     *
     * @param sessionScopeId session identifier of scope
     * @return persistent identifier of scope
     */
    long getScopeId(long sessionScopeId);

    /**
     * Removes scope from session.
     *
     * @param sessionScopeId session identifier of scope
     */
    void removeScope(long sessionScopeId);

    /**
     * Adds metric to session.
     *
     * @param sessionMetricId session identifier of metric
     * @param name            metric name
     * @return persistent identifier of metric
     */
    long putMetric(long sessionMetricId, IMetricName name);

    /**
     * Returns persistent identifier of metric.
     *
     * @param sessionMetricId session identifier of metric
     * @return persistent identifier of metric
     */
    long getMetricId(long sessionMetricId);

    /**
     * Removes metric from session.
     *
     * @param sessionMetricId session identifier of metric
     */
    void removeMetric(long sessionMetricId);

    /**
     * Adds callpath to session.
     *
     * @param sessionCallPathId session identifier of callpath
     * @param parentCallPathId  persistent identifier of parent callpath
     * @param metricId          persistent identifier of metric segment of callpath
     * @return persistent identifier of callpath
     */
    long putCallPath(long sessionCallPathId, long parentCallPathId, long metricId);

    /**
     * Returns persistent identifier of callpath.
     *
     * @param sessionCallPathId session identifier of callpath
     * @return persistent identifier of callpath
     */
    long getCallPathId(long sessionCallPathId);

    /**
     * Removes callpath from session.
     *
     * @param sessionCallPathId session identifier of callpath
     */
    void removeCallPath(long sessionCallPathId);
}
