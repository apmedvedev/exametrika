/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.HttpConnectionProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link HttpConnectionProbeConfiguration} is a configuration of HTTP connection probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HttpConnectionProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration timeCounter;
    private final CounterConfiguration receiveBytesCounter;
    private final CounterConfiguration sendBytesCounter;
    private final LogConfiguration errorsLog;

    public HttpConnectionProbeConfiguration(String name, String scopeType,
                                            String measurementStrategy, long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy,
                                            CounterConfiguration timeCounter, CounterConfiguration receiveBytesCounter,
                                            CounterConfiguration sendBytesCounter, LogConfiguration errorsLog) {
        super(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy);

        Assert.notNull(timeCounter);
        Assert.notNull(receiveBytesCounter);
        Assert.notNull(sendBytesCounter);
        Assert.notNull(errorsLog);

        this.timeCounter = timeCounter;
        this.receiveBytesCounter = receiveBytesCounter;
        this.sendBytesCounter = sendBytesCounter;
        this.errorsLog = errorsLog;
    }

    @Override
    public String getType() {
        return super.getType() + ",remote,http";
    }

    @Override
    public String getExitPointType() {
        return "httpRequests";
    }

    public CounterConfiguration getTimeCounter() {
        return timeCounter;
    }

    public CounterConfiguration getReceiveBytesCounter() {
        return receiveBytesCounter;
    }

    public CounterConfiguration getSendBytesCounter() {
        return sendBytesCounter;
    }

    public LogConfiguration getErrorsLog() {
        return errorsLog;
    }

    @Override
    public String getComponentType() {
        return "app.httpConnection";
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isIntermediate() {
        return true;
    }

    @Override
    public boolean isPermanentHotspot() {
        return true;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new HttpConnectionProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (timeCounter.isEnabled())
            builder.metric(timeCounter.getSchema("app.http.time"));
        if (receiveBytesCounter.isEnabled())
            builder.metric(receiveBytesCounter.getSchema("app.http.receive.bytes"));
        if (sendBytesCounter.isEnabled())
            builder.metric(sendBytesCounter.getSchema("app.http.send.bytes"));

        if (errorsLog.isEnabled()) {
            builder.metrics(errorsLog.getMetricSchemas());
            errorsLog.buildComponentSchemas(builder.name() + ".errors", components);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpConnectionProbeConfiguration))
            return false;

        HttpConnectionProbeConfiguration configuration = (HttpConnectionProbeConfiguration) o;
        return super.equals(configuration) && timeCounter.equals(configuration.timeCounter) &&
                receiveBytesCounter.equals(configuration.receiveBytesCounter) &&
                sendBytesCounter.equals(configuration.sendBytesCounter) && errorsLog.equals(configuration.errorsLog);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(timeCounter, receiveBytesCounter,
                sendBytesCounter, errorsLog);
    }
}
