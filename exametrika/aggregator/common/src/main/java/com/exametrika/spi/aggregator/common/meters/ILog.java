/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link ILog} represents a log.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILog extends IMeter {
    /**
     * Returns measurement identifier.
     *
     * @return measurement identifier
     */
    IMeasurementId getId();

    /**
     * Adds log entry.
     *
     * @param value log event value
     */
    void measure(ILogEvent value);

    /**
     * Returns metadata.
     *
     * @return metadata or null if metadata are not set
     */
    JsonObject getMetadata();

    /**
     * Sets metadata.
     *
     * @param metadata metadata or null if metadata are not used
     */
    void setMetadata(JsonObject metadata);
}
