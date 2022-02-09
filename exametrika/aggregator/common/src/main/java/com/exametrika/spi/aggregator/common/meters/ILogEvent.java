/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObjectBuilder;


/**
 * The {@link ILogEvent} represents a log event.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILogEvent {
    /**
     * Returns identifier of log event.
     *
     * @return identifier of log event
     */
    IMeasurementId getId();

    /**
     * Return true if log event is error.
     *
     * @return true if log event is error
     */
    boolean isError();

    /**
     * Sets identifier of log event.
     *
     * @param id identifier of log event
     */
    void setId(IMeasurementId id);

    /**
     * Returns type.
     *
     * @return type
     */
    String getType();

    /**
     * Sets type.
     *
     * @param type type
     */
    void setType(String type);

    /**
     * Returns time.
     *
     * @return time
     */
    long getTime();

    /**
     * Sets time.
     *
     * @param time time
     */
    void setTime(long time);

    /**
     * Returns message.
     *
     * @return message
     */
    String getMessage();

    /**
     * Sets message
     *
     * @param message message
     */
    void setMessage(String message);

    /**
     * Returns exception.
     *
     * @return exception or null if exception is not set
     */
    Throwable getException();

    /**
     * Sets exception.
     *
     * @param exception exception or null if exception is not used
     */
    void setException(Throwable exception);

    /**
     * Returns error location for error log event.
     *
     * @param measurementContext measurement context
     * @return error location for error log event or null if error location is not available
     */
    String getErrorLocation(IMeasurementContext measurementContext);

    /**
     * Returns additional modifiable log event parameters.
     *
     * @return additional modifiable log event parameters
     */
    JsonObjectBuilder getParameters();

    /**
     * Returns true if log event has stack trace.
     *
     * @return true if log event has stack trace
     */
    boolean hasStackTrace();

    /**
     * Adds additional parameters.
     *
     * @param allowStackTrace    if true stack trace is allowed
     * @param maxStackTraceDepth max depth of stack trace
     * @param json               json
     * @param measurementContext measurement context
     */
    void addParameters(boolean allowStackTrace, int maxStackTraceDepth, Json json, IMeasurementContext measurementContext);
}
