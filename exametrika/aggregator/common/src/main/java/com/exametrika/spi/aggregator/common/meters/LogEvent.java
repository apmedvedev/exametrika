/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;


/**
 * The {@link LogEvent} is a log event implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogEvent implements ILogEvent {
    private IMeasurementId id;
    private String type;
    private long time;
    private String message;
    private Throwable exception;
    private final JsonObjectBuilder parameters;
    private final boolean error;

    public LogEvent(IMeasurementId id, String type, long time, String message, Throwable exception, JsonObjectBuilder parameters,
                    boolean error) {
        Assert.notNull(id);
        Assert.notNull(type);
        if (message == null && exception != null)
            message = exception.getMessage();
        if (message == null)
            message = "";
        if (parameters == null)
            parameters = new JsonObjectBuilder();

        this.id = id;
        this.type = type;
        this.time = time;
        this.message = message;
        this.exception = exception;
        this.parameters = parameters;
        this.error = error;
    }

    public LogEvent getEvent() {
        return this;
    }

    @Override
    public IMeasurementId getId() {
        return id;
    }

    @Override
    public boolean isError() {
        return error;
    }

    @Override
    public void setId(IMeasurementId id) {
        Assert.notNull(id);

        this.id = id;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        Assert.notNull(type);

        this.type = type;
    }

    @Override
    public final long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public final String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        Assert.notNull(message);

        this.message = message;
    }

    @Override
    public final Throwable getException() {
        return exception;
    }

    @Override
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public String getErrorLocation(IMeasurementContext measurementContext) {
        return null;
    }

    @Override
    public final JsonObjectBuilder getParameters() {
        return parameters;
    }

    @Override
    public boolean hasStackTrace() {
        return exception != null;
    }

    @Override
    public void addParameters(boolean allowStackTrace, int maxStackTraceDepth, Json json, IMeasurementContext measurementContext) {
    }
}
