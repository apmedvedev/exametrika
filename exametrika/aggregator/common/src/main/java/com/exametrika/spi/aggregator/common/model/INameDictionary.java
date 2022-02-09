/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.model;

import com.exametrika.api.aggregator.common.model.IName;


/**
 * The {@link INameDictionary} is a persistent dictionary of names (typically name index) used for serialization optimization.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public interface INameDictionary {
    /**
     * Returns name by persistent identifier.
     *
     * @param persistentNameId persistent name identifier.
     * @return name or null if id is 0 (name is root)
     */
    IName getName(long persistentNameId);

    /**
     * Returns persistent identifier of name.
     *
     * @param name name
     * @return persistent identifier of name starting from 1 or 0 if name if empty
     */
    long getName(IName name);

    /**
     * Returns persistent identifier of name. 0 in parent callpath identifier and 0 in metric identifier means root call path.
     *
     * @param parentCallPathId persistent identifier of parent callpath
     * @param metricId         persistent identifier of metric segment
     * @return persistent identifier of callpath starting from 1 or 0 if callpath is root
     */
    long getCallPath(long parentCallPathId, long metricId);
}
