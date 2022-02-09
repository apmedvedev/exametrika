/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

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
 * The {@link HttpConnectionErrorLogEvent} is a http log event implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HttpConnectionErrorLogEvent extends LogEvent {
    private int status;
    private final IRequest request;
    private final Scope scope;

    public HttpConnectionErrorLogEvent(IMeasurementId id, String type, long time, long transactionId, String thread, String message,
                                       Scope scope, int status, IRequest request) {
        super(id, type, time, message, null, Json.object()
                .putIf("transactionId", transactionId, transactionId != 0)
                .put("thread", thread)
                .put("status", status)
                .toObjectBuilder(), true);

        Assert.notNull(request);
        Assert.notNull(scope);

        this.status = status;
        this.request = request;
        this.scope = scope;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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
        json.putIf("request", requestParameters, requestParameters != null && !requestParameters.isEmpty());
    }
}
