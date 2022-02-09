/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackIdsValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;


/**
 * The {@link EntryPointProbeConfiguration} is a configuration of entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class EntryPointProbeConfiguration extends ProbeConfiguration {
    public static final int POINTCUT_PRIORITY = 1000;
    private final String primaryEntryPointExpression;
    private final RequestMappingStrategyConfiguration requestMappingStrategy;
    private final long maxDuration;
    private final CounterConfiguration transactionTimeCounter;
    private final LogConfiguration stalledRequestsLog;
    private final CounterConfiguration timeCounter;
    private final CounterConfiguration receiveBytesCounter;
    private final CounterConfiguration sendBytesCounter;
    private final LogConfiguration errorsLog;
    private final String stackMeasurementStrategy;
    private final PrimaryType allowPrimary;
    private final boolean allowSecondary;

    public enum PrimaryType {
        YES,
        NO,
        ALWAYS
    }

    public EntryPointProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                        long warmupDelay, RequestMappingStrategyConfiguration requestMappingStrategy, long maxDuration,
                                        CounterConfiguration transactionTimeCounter, LogConfiguration stalledRequestsLog, String primaryEntryPointExpression,
                                        String stackMeasurementStrategy, PrimaryType allowPrimary, boolean allowSecondary,
                                        CounterConfiguration timeCounter, CounterConfiguration receiveBytesCounter,
                                        CounterConfiguration sendBytesCounter, LogConfiguration errorsLog) {
        super(name, scopeType, 0, measurementStrategy, warmupDelay);

        Assert.notNull(transactionTimeCounter);
        Assert.notNull(stalledRequestsLog);
        Assert.notNull(timeCounter);
        Assert.notNull(receiveBytesCounter);
        Assert.notNull(sendBytesCounter);
        Assert.notNull(errorsLog);

        this.requestMappingStrategy = requestMappingStrategy;
        this.maxDuration = maxDuration;
        this.transactionTimeCounter = transactionTimeCounter;
        this.stalledRequestsLog = stalledRequestsLog;
        this.timeCounter = timeCounter;
        this.receiveBytesCounter = receiveBytesCounter;
        this.sendBytesCounter = sendBytesCounter;
        this.errorsLog = errorsLog;
        this.primaryEntryPointExpression = primaryEntryPointExpression;
        this.stackMeasurementStrategy = stackMeasurementStrategy;
        this.allowPrimary = allowPrimary;
        this.allowSecondary = allowSecondary;
    }

    public final RequestMappingStrategyConfiguration getRequestMappingStrategy() {
        return requestMappingStrategy;
    }

    public final long getMaxDuration() {
        return maxDuration;
    }

    public final CounterConfiguration getTransactionTimeCounter() {
        return transactionTimeCounter;
    }

    public final LogConfiguration getStalledRequestsLog() {
        return stalledRequestsLog;
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

    public String getPrimaryEntryPointExpression() {
        return primaryEntryPointExpression;
    }

    public String getStackMeasurementStrategy() {
        return stackMeasurementStrategy;
    }

    public PrimaryType getAllowPrimary() {
        return allowPrimary;
    }

    public boolean isSecondaryAllowed() {
        return allowSecondary;
    }

    public String getType() {
        return "entry,jvm";
    }

    @Override
    public String getComponentType() {
        return "app.entryPoint";
    }

    public abstract String getEntryPointType();

    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        builder.metric(new StackIdsValueSchemaConfiguration("stackIds"));

        if (stalledRequestsLog.isEnabled()) {
            builder.metrics(stalledRequestsLog.getMetricSchemas());
            stalledRequestsLog.buildComponentSchemas(getComponentType() + ".stalls", components);
        }

        if (transactionTimeCounter.isEnabled())
            builder.metric(transactionTimeCounter.getSchema("app.transaction.time"));

        if (timeCounter.isEnabled())
            builder.metric(timeCounter.getSchema("app.request.time"));
        if (receiveBytesCounter.isEnabled())
            builder.metric(receiveBytesCounter.getSchema("app.receive.bytes"));
        if (sendBytesCounter.isEnabled())
            builder.metric(sendBytesCounter.getSchema("app.send.bytes"));
        if (errorsLog.isEnabled()) {
            builder.metrics(errorsLog.getMetricSchemas());
            errorsLog.buildComponentSchemas(getComponentType() + ".errors", components);
        }
    }

    @Override
    public final void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        Assert.supports(false);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof EntryPointProbeConfiguration))
            return false;

        EntryPointProbeConfiguration configuration = (EntryPointProbeConfiguration) o;
        return super.equals(configuration) &&
                Objects.equals(requestMappingStrategy, configuration.requestMappingStrategy) &&
                maxDuration == configuration.maxDuration && transactionTimeCounter.equals(configuration.transactionTimeCounter) &&
                stalledRequestsLog.equals(configuration.stalledRequestsLog) &&
                Objects.equals(primaryEntryPointExpression, configuration.primaryEntryPointExpression) &&
                Objects.equals(stackMeasurementStrategy, configuration.stackMeasurementStrategy) &&
                allowPrimary == configuration.allowPrimary && allowSecondary == configuration.allowSecondary &&
                timeCounter.equals(configuration.timeCounter) && receiveBytesCounter.equals(configuration.receiveBytesCounter) &&
                sendBytesCounter.equals(configuration.sendBytesCounter) && errorsLog.equals(configuration.errorsLog);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(requestMappingStrategy, maxDuration, transactionTimeCounter,
                stalledRequestsLog, primaryEntryPointExpression, stackMeasurementStrategy, allowPrimary, allowSecondary,
                timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
    }
}
