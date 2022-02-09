/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.Probes;


/**
 * The {@link ApplicationLogEvent} is a application log event implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ApplicationLogEvent extends LogEvent {
    public ApplicationLogEvent(IMeasurementId id, String type, long time, String message, Throwable exception,
                               JsonObjectBuilder parameters) {
        super(id, type, time, message, exception, parameters, isError(parameters));
    }

    @Override
    public String getErrorLocation(IMeasurementContext measurementContext) {
        if (getException() != null)
            return Probes.getErrorLocation(getException(), ((IProbeContext) measurementContext).getJoinPointProvider());
        else
            return null;
    }

    private static boolean isError(JsonObjectBuilder parameters) {
        if (parameters != null && parameters.get("level", "trace").equals("error"))
            return true;
        else
            return false;
    }
}
