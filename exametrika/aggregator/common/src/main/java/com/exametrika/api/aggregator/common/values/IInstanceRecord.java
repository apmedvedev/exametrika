/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link IInstanceRecord} represents a instance field record value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IInstanceRecord extends IFieldValue {
    /**
     * Returns measurement identifier of record.
     *
     * @return measurement identifier of record
     */
    IMeasurementId getId();

    /**
     * Returns context of record.
     *
     * @return context of record
     */
    JsonObject getContext();

    /**
     * Returns time of record.
     *
     * @return time of record
     */
    long getTime();

    /**
     * Returns value.
     *
     * @return value
     */
    long getValue();
}
