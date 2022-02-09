/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.Probes;


/**
 * The {@link StalledLogEvent} is a stalled request log event implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StalledLogEvent extends LogEvent {
    private final Scope scope;
    private final long duration;
    private final IRequest request;

    public StalledLogEvent(IMeasurementId id, String type, String entryType, long time, long transactionId, String thread, String message, Scope scope,
                           long duration, IRequest request) {
        super(id, type, time, message, null, Json.object()
                .putIf("transactionId", transactionId, transactionId != 0)
                .put("thread", thread)
                .put("entryType", entryType)
                .put("duration", duration)
                .toObjectBuilder(), true);

        Assert.notNull(scope);
        Assert.notNull(request);

        this.scope = scope;
        this.duration = duration;
        this.request = request;
    }

    public long getDuration() {
        return duration;
    }

    public IRequest getRequest() {
        return request;
    }

    @Override
    public boolean hasStackTrace() {
        return true;
    }

    @Override
    public void addParameters(boolean allowStackTrace, int maxStackTraceDepth, Json json, IMeasurementContext measurementContext) {
        if (allowStackTrace)
            Probes.buildStackTrace(scope, maxStackTraceDepth, ((IProbeContext) measurementContext).getJoinPointProvider(), json);

        JsonObject requestParameters = request.getParameters();
        json.putIf("request", requestParameters, requestParameters != null);
    }
}
