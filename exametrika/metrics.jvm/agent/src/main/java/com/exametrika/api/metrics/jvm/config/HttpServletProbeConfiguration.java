/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import com.exametrika.impl.metrics.jvm.probes.HttpServletProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link HttpServletProbeConfiguration} is a configuration of HTTP servlet probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpServletProbeConfiguration extends EntryPointProbeConfiguration {
    public HttpServletProbeConfiguration(String name, String scopeType,
                                         String measurementStrategy, long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy, long maxDuration,
                                         CounterConfiguration transactionTimeCounter, LogConfiguration stalledRequestsLog, String primaryEntryPointExpression,
                                         String stackMeasurementStrategy, PrimaryType allowPrimary, boolean allowSecondary,
                                         CounterConfiguration timeCounter, CounterConfiguration receiveBytesCounter,
                                         CounterConfiguration sendBytesCounter, LogConfiguration errorsLog) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy, maxDuration,
                transactionTimeCounter, stalledRequestsLog, primaryEntryPointExpression, stackMeasurementStrategy,
                allowPrimary, allowSecondary, timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
    }

    @Override
    public String getType() {
        return super.getType() + ",http";
    }

    @Override
    public String getEntryPointType() {
        return "httpServlet";
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new HttpServletProbe(this, context, index);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpServletProbeConfiguration))
            return false;

        HttpServletProbeConfiguration configuration = (HttpServletProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
