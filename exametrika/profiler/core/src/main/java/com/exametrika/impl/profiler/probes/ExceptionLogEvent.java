/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.meters.IMeasurementContext;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.Probes;


/**
 * The {@link ExceptionLogEvent} is a exception log event implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExceptionLogEvent extends LogEvent {
    public ExceptionLogEvent(IMeasurementId id, String type, long time, String message, Throwable exception, JsonObjectBuilder parameters) {
        super(id, type, time, message, exception, parameters, true);

        Assert.notNull(exception);
    }

    @Override
    public String getErrorLocation(IMeasurementContext measurementContext) {
        return Probes.getErrorLocation(getException(), ((IProbeContext) measurementContext).getJoinPointProvider());
    }
}
