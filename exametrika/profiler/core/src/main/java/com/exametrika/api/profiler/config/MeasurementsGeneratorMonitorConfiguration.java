/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.profiler.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.profiler.modelling.MeasurementsGeneratorMonitor;
import com.exametrika.spi.profiler.IMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link MeasurementsGeneratorMonitorConfiguration} is a configuration of measurements generator monitor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementsGeneratorMonitorConfiguration extends MonitorConfiguration {
    private final int nodesCount;
    private final int primaryEntryPointNodesCount;
    private final int transactionsPerNodeCount;
    private final int transactionSegmentsDepth;
    private final int logRecordsCount;
    private final int stackDepth;
    private final int leafStackEntriesCount;
    private final int maxEndExitPointsCount;
    private final int maxIntermediateExitPointsCount;
    private final int exitPointsPerEntryCount;
    private final CombineType combineType;
    private final String measurementProfile;

    public MeasurementsGeneratorMonitorConfiguration(String name, String scope, String measurementStrategy, long period,
                                                     int nodesCount, int primaryEntryPointNodesCount,
                                                     int transactionsPerNodeCount, int transactionSegmentsDepth,
                                                     int logRecordsCount, int stackDepth, int leafStackEntriesCount, int maxEndExitPointsCount,
                                                     int maxIntermediateExitPointsCount, int exitPointsPerEntryCount, CombineType combineType, String measurementProfile) {
        super(name, scope, period, measurementStrategy);

        Assert.notNull(combineType);
        Assert.notNull(measurementProfile);

        this.nodesCount = nodesCount;
        this.primaryEntryPointNodesCount = primaryEntryPointNodesCount;
        this.transactionsPerNodeCount = transactionsPerNodeCount;
        this.transactionSegmentsDepth = transactionSegmentsDepth;
        this.logRecordsCount = logRecordsCount;
        this.stackDepth = stackDepth;
        this.leafStackEntriesCount = leafStackEntriesCount;
        this.maxEndExitPointsCount = maxEndExitPointsCount;
        this.maxIntermediateExitPointsCount = maxIntermediateExitPointsCount;
        this.exitPointsPerEntryCount = exitPointsPerEntryCount;
        this.combineType = combineType;
        this.measurementProfile = measurementProfile;
    }

    public int getNodesCount() {
        return nodesCount;
    }

    public int getPrimaryEntryPointNodesCount() {
        return primaryEntryPointNodesCount;
    }

    public int getTransactionsPerNodeCount() {
        return transactionsPerNodeCount;
    }

    public int getTransactionSegmentsDepth() {
        return transactionSegmentsDepth;
    }

    public int getLogRecordsCount() {
        return logRecordsCount;
    }

    public int getStackDepth() {
        return stackDepth;
    }

    public int getLeafStackEntriesCount() {
        return leafStackEntriesCount;
    }

    public int getMaxEndExitPointsCount() {
        return maxEndExitPointsCount;
    }

    public int getMaxIntermediateExitPointsCount() {
        return maxIntermediateExitPointsCount;
    }

    public int getExitPointsPerEntryCount() {
        return exitPointsPerEntryCount;
    }

    public CombineType getCombineType() {
        return combineType;
    }

    public String getMeasurementProfile() {
        return measurementProfile;
    }

    @Override
    public IMonitor createMonitor(IMonitorContext context) {
        return new MeasurementsGeneratorMonitor(this, context);
    }

    @Override
    public void buildComponentSchemas(Set<ComponentValueSchemaConfiguration> components) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeasurementsGeneratorMonitorConfiguration))
            return false;

        MeasurementsGeneratorMonitorConfiguration configuration = (MeasurementsGeneratorMonitorConfiguration) o;
        return super.equals(configuration) && nodesCount == configuration.nodesCount &&
                primaryEntryPointNodesCount == configuration.primaryEntryPointNodesCount &&
                transactionsPerNodeCount == configuration.transactionsPerNodeCount &&
                transactionSegmentsDepth == configuration.transactionSegmentsDepth &&
                logRecordsCount == configuration.logRecordsCount && stackDepth == configuration.stackDepth &&
                leafStackEntriesCount == configuration.leafStackEntriesCount && maxEndExitPointsCount == configuration.maxEndExitPointsCount &&
                maxIntermediateExitPointsCount == configuration.maxIntermediateExitPointsCount &&
                exitPointsPerEntryCount == configuration.exitPointsPerEntryCount && combineType.equals(configuration.combineType) &&
                measurementProfile.equals(configuration.measurementProfile);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(nodesCount, primaryEntryPointNodesCount, transactionsPerNodeCount, transactionSegmentsDepth,
                logRecordsCount, stackDepth, leafStackEntriesCount, maxEndExitPointsCount, maxIntermediateExitPointsCount, exitPointsPerEntryCount,
                combineType, measurementProfile);
    }
}
