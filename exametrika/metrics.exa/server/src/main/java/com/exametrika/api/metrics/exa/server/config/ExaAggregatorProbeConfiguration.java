/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.common.meters.config.ErrorCountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ValueSchemas;
import com.exametrika.impl.metrics.exa.server.probes.ExaAggregatorProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ExaAggregatorProbeConfiguration} is a configuration of exa aggregator probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaAggregatorProbeConfiguration extends ProbeConfiguration {
    private final CounterConfiguration counter;
    private final CounterConfiguration timeCounter;
    private final GaugeConfiguration gauge;
    private final LogConfiguration transactionErrors;

    public ExaAggregatorProbeConfiguration(String name, String scopeType, long extractionPeriod, String measurementStrategy,
                                           long warmupDelay) {
        super(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay);

        counter = new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(0, 30)), false, 0);
        timeCounter = new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(1000000, 15)), false, 0);
        gauge = new GaugeConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                new LogarithmicHistogramFieldConfiguration(0, 30)));
        transactionErrors = new LogConfiguration(true, null, Arrays.asList(
                new LogMeterConfiguration("exa.rawdb.transaction.errors.count", new CounterConfiguration(true, true, 0), null,
                        new ErrorCountLogProviderConfiguration())), null, null, 100, 512, 1000, 10, 100);
    }

    public CounterConfiguration getCounter() {
        return counter;
    }

    public CounterConfiguration getTimeCounter() {
        return timeCounter;
    }

    public GaugeConfiguration getGauge() {
        return gauge;
    }

    public LogConfiguration getTransactionErrors() {
        return transactionErrors;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new ExaAggregatorProbe(this, context);
    }

    @Override
    public String getComponentType() {
        return null;
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
        ComponentValueSchemaBuilder builder = ValueSchemas.component("exa.rawdb");
        builder.object("exa.rawdb.memoryManager");
        builder.metric(gauge.getSchema("exa.rawdb.pagePool"));
        builder.metric(timeCounter.getSchema("exa.rawdb.file.read.time"));
        builder.metric(counter.getSchema("exa.rawdb.file.read.bytes"));
        builder.metric(timeCounter.getSchema("exa.rawdb.file.write.time"));
        builder.metric(counter.getSchema("exa.rawdb.file.write.bytes"));
        builder.metric(gauge.getSchema("exa.rawdb.file.currentLoaded"));
        builder.metric(counter.getSchema("exa.rawdb.file.loaded"));
        builder.metric(counter.getSchema("exa.rawdb.file.unloaded"));
        builder.metric(timeCounter.getSchema("exa.rawdb.transactionLog.flush.time"));
        builder.metric(counter.getSchema("exa.rawdb.transactionLog.flush.bytes"));
        builder.metric(gauge.getSchema("exa.rawdb.transaction.queue"));
        builder.metric(timeCounter.getSchema("exa.rawdb.transaction.time"));

        List<MetricValueSchemaConfiguration> metrics = transactionErrors.getMetricSchemas();
        builder.metrics(metrics);
        components.add(builder.toConfiguration());

        transactionErrors.buildComponentSchemas("exa.rawdb.transaction.errors.log", components);

        builder = ValueSchemas.component("exa.rawdb.pageCache");
        builder.metric(gauge.getSchema("exa.rawdb.pageCache.size"));
        builder.metric(gauge.getSchema("exa.rawdb.pageCache.maxSize"));
        builder.metric(gauge.getSchema("exa.rawdb.pageCache.quota"));
        builder.metric(counter.getSchema("exa.rawdb.pageCache.totalLoaded"));
        builder.metric(counter.getSchema("exa.rawdb.pageCache.totalUnloaded"));
        builder.metric(counter.getSchema("exa.rawdb.pageCache.unloadedByOverflow"));
        builder.metric(counter.getSchema("exa.rawdb.pageCache.unloadedByTimer"));
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("exa.rawdb.pageType");
        builder.metric(gauge.getSchema("exa.rawdb.pageType.regionsCount"));
        builder.metric(gauge.getSchema("exa.rawdb.pageType.regionsSize"));
        builder.metric(counter.getSchema("exa.rawdb.pageType.allocated"));
        builder.metric(counter.getSchema("exa.rawdb.pageType.freed"));
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("exa.exadb.fullText");
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.addTime"));
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.updateTime"));
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.deleteTime"));
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.searchTime"));
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.searcherUpdateTime"));
        builder.metric(timeCounter.getSchema("exa.exadb.fullText.commitTime"));
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("exa.exadb.nodeCache");
        builder.metric(gauge.getSchema("exa.exadb.nodeCache.size"));
        builder.metric(gauge.getSchema("exa.exadb.nodeCache.maxSize"));
        builder.metric(gauge.getSchema("exa.exadb.nodeCache.quota"));
        builder.metric(counter.getSchema("exa.exadb.nodeCache.totalLoaded"));
        builder.metric(counter.getSchema("exa.exadb.nodeCache.totalUnloaded"));
        builder.metric(counter.getSchema("exa.exadb.nodeCache.unloadedByOverflow"));
        builder.metric(counter.getSchema("exa.exadb.nodeCache.unloadedByTimer"));
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("exa.aggregator.nameCache");
        builder.metric(gauge.getSchema("exa.aggregator.nameCache.size"));
        builder.metric(gauge.getSchema("exa.aggregator.nameCache.maxSize"));
        builder.metric(gauge.getSchema("exa.aggregator.nameCache.quota"));
        builder.metric(counter.getSchema("exa.aggregator.nameCache.totalLoaded"));
        builder.metric(counter.getSchema("exa.aggregator.nameCache.totalUnloaded"));
        builder.metric(counter.getSchema("exa.aggregator.nameCache.unloadedByOverflow"));
        builder.metric(counter.getSchema("exa.aggregator.nameCache.unloadedByTimer"));
        components.add(builder.toConfiguration());

        builder = ValueSchemas.component("exa.aggregator");
        builder.metric(timeCounter.getSchema("exa.aggregator.aggregateTime"));
        builder.metric(counter.getSchema("exa.aggregator.aggregateCount"));
        builder.metric(timeCounter.getSchema("exa.aggregator.closePeriodTime"));
        builder.metric(timeCounter.getSchema("exa.aggregator.selectTime"));
        builder.metric(counter.getSchema("exa.aggregator.selectSize"));
        components.add(builder.toConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaAggregatorProbeConfiguration))
            return false;

        ExaAggregatorProbeConfiguration configuration = (ExaAggregatorProbeConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
