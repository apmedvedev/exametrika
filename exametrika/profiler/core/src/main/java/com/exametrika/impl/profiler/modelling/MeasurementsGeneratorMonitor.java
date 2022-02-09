/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.modelling;

import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.profiler.config.MeasurementsGeneratorMonitorConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.impl.profiler.modelling.MeasurementsGenerator.MeasurementProfile;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link MeasurementsGeneratorMonitor} is a monitor of measurements generator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementsGeneratorMonitor extends AbstractMonitor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(MeasurementsGeneratorMonitor.class);
    private final MeasurementsGeneratorMonitorConfiguration configuration;
    private List<MeasurementSet> generatedMeasurements;

    public MeasurementsGeneratorMonitor(MeasurementsGeneratorMonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, true);

        this.configuration = configuration;
    }

    @Override
    protected void createMeters() {
    }

    @Override
    public void measure(List<Measurement> measurements, final long time, final long period, final boolean force) {
        context.getTaskQueue().offer(new Runnable() {
            @Override
            public void run() {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.beginGenerating());

                MeasurementsGenerator generator = createGenerator();
                if (generatedMeasurements == null)
                    generatedMeasurements = generator.generate();

                int count = 0;
                for (MeasurementSet set : generatedMeasurements) {
                    context.getMeasurementHandler().handle(set);
                    count += set.getMeasurements().size();
                }

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.endGenerating(count));
            }
        });
    }

    private MeasurementsGenerator createGenerator() {
        boolean regenerate = false;
        if (regenerate)
            this.generatedMeasurements = null;

        String str = configuration.getMeasurementProfile().toUpperCase().replace('-', '_');
        MeasurementProfile measurementProfile = MeasurementProfile.PROD;
        for (MeasurementProfile profile : MeasurementProfile.values()) {
            if (profile.toString().equals(str)) {
                measurementProfile = profile;
                break;
            }
        }
        return new MeasurementsGenerator(configuration.getNodesCount(), configuration.getPrimaryEntryPointNodesCount(),
                configuration.getTransactionsPerNodeCount(),
                configuration.getTransactionSegmentsDepth(), configuration.getLogRecordsCount(), configuration.getStackDepth(),
                configuration.getLeafStackEntriesCount(), configuration.getMaxEndExitPointsCount(),
                configuration.getMaxIntermediateExitPointsCount(), configuration.getExitPointsPerEntryCount(),
                configuration.getCombineType(), context.getConfiguration().getSchemaVersion(),
                measurementProfile);
    }

    private interface IMessages {
        @DefaultMessage("Begin generating...")
        ILocalizedMessage beginGenerating();

        @DefaultMessage("End generating. Measurements: {0}")
        ILocalizedMessage endGenerating(int measurementsCount);
    }
}
