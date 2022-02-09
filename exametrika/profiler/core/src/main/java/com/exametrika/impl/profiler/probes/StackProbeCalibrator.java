/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Serializers;
import com.exametrika.common.utils.Strings;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.spi.profiler.IProbe;


/**
 * The {@link StackProbeCalibrator} is a stack probe calibrator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackProbeCalibrator {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(StackProbeCalibrator.class);
    private static final MessageDigest digest = getMessageDigest();
    private final ProbeContext context;
    private final StackProbeConfiguration configuration;
    private final StackProbe probe;

    public StackProbeCalibrator(ProbeContext context, StackProbeConfiguration configuration, StackProbe probe) {
        Assert.notNull(context);
        Assert.notNull(configuration);
        Assert.notNull(probe);

        this.context = context;
        this.configuration = configuration;
        this.probe = probe;
    }

    public StackProbeCalibrateInfo getCalibrateInfo() {
        String configHash = getConfigHash();
        StackProbeCalibrateInfo calibrateInfo = loadCalibrateInfo();
        if (calibrateInfo == null || !calibrateInfo.configHash.equals(configHash))
            return null;
        else
            return calibrateInfo;
    }

    public StackProbeCalibrateInfo calibrate(boolean force) {
        String configHash = getConfigHash();
        StackProbeCalibrateInfo calibrateInfo = loadCalibrateInfo();
        if (force || calibrateInfo == null || !calibrateInfo.configHash.equals(configHash)) {
            if (logger.isLogEnabled(LogLevel.INFO))
                logger.log(LogLevel.INFO, messages.calibrateStarted());

            calibrateInfo = calibrate(configHash);

            if (logger.isLogEnabled(LogLevel.INFO))
                logger.log(LogLevel.INFO, messages.calibrateFinished(calibrateInfo.toJson().toString()));

            saveCalibrateInfo(calibrateInfo);
        } else {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.calibrateLoaded(calibrateInfo.toJson().toString()));
        }

        return calibrateInfo;
    }

    private StackProbeCalibrateInfo calibrate(String configHash) {
        ThreadLocalAccessor.setThreadLocal(Thread.currentThread(), null);
        ThreadLocalAccessor calibrateAccessor = new ThreadLocalAccessor(context);
        StackProbeRootCollector root = new StackProbeRootCollector(probe, configuration, new ProbeContext(
                calibrateAccessor, context.getInstrumentationService(), context.getJoinPointProvider(), context.getClassTransformer(),
                context.getTimeService(), context.getMeasurementHandler(), context.getConfiguration(),
                new HashMap(), context.getMeasurementStrategyManager()),
                new Scope(ScopeName.get("calibrate"), calibrateAccessor.get().scopes, 0,
                        java.util.Collections.<IProbe>emptyList(), false, false, null, false), calibrateAccessor.get());

        IStackProbeCollectorFactory collectorFactory = new CalibrateStackProbeCollectorFactory();
        for (int i = 0; i < 100000; i++)
            root.beginMeasure(0, 0, collectorFactory, null, null).endMeasure();

        int normalCount = 1000000;

        long t = 0;
        for (int i = 0; i < normalCount; i++) {
            long l1 = context.getTimeSource().getCurrentTime();
            long l2 = context.getTimeSource().getCurrentTime();
            t += l2 - l1;
        }

        long normalCollectorInnerOverhead = t / normalCount;

        for (int i = 0; i < 100000; i++)
            root.beginMeasure(1, 0, collectorFactory, null, null).endMeasure();

        StackProbeCollector collector = root.beginMeasure(1, 0, collectorFactory, null, null);
        collector.setCalibrate(false, false);

        t = System.nanoTime();
        for (int i = 0; i < normalCount; i++)
            root.beginMeasure(1, 0, collectorFactory, null, null).endMeasure();

        long normalCollectorEstimatingOuterOverhead = (System.nanoTime() - t) / normalCount;

        for (int i = 0; i < 100000; i++)
            root.beginMeasure(2, 0, collectorFactory, null, null).endMeasure();

        collector = root.beginMeasure(2, 0, collectorFactory, null, null);
        collector.setCalibrate(true, false);

        t = System.nanoTime();
        for (int i = 0; i < normalCount; i++)
            root.beginMeasure(2, 0, collectorFactory, null, null).endMeasure();

        long normalCollectorFullOuterOverhead = (System.nanoTime() - t) / normalCount;

        long fastCollectorInnerOverhead = 0;
        long fastCollectorEstimatingOuterOverhead = 0;
        long fastCollectorFullOuterOverhead = 0;
        if (Times.isTickCountAvaliable()) {
            int fastCount = 5000000;

            t = System.nanoTime();
            for (int i = 0; i < fastCount; i++) {
                Times.getTickCount();
                Times.getTickCount();
            }

            fastCollectorInnerOverhead = (System.nanoTime() - t) / fastCount;

            for (int i = 0; i < 100000; i++)
                root.beginMeasure(3, 0, collectorFactory, null, null).endMeasure();

            collector = root.beginMeasure(3, 0, collectorFactory, null, null);
            collector.setCalibrate(false, true);

            t = System.nanoTime();
            for (int i = 0; i < fastCount; i++)
                root.beginMeasure(3, 0, collectorFactory, null, null).endMeasure();

            fastCollectorEstimatingOuterOverhead = (System.nanoTime() - t) / fastCount;

            collector = root.beginMeasure(4, 0, collectorFactory, null, null);
            collector.setCalibrate(true, true);
            for (int i = 0; i < 100000; i++)
                root.beginMeasure(4, 0, collectorFactory, null, null).endMeasure();

            t = System.nanoTime();
            for (int i = 0; i < fastCount; i++)
                root.beginMeasure(4, 0, collectorFactory, null, null).endMeasure();

            fastCollectorFullOuterOverhead = (System.nanoTime() - t) / fastCount;

            normalCollectorInnerOverhead += fastCollectorInnerOverhead;
        }

        calibrateAccessor.close();

        if (fastCollectorInnerOverhead > normalCollectorInnerOverhead)
            normalCollectorInnerOverhead = fastCollectorInnerOverhead;
        if (normalCollectorInnerOverhead > normalCollectorEstimatingOuterOverhead)
            normalCollectorEstimatingOuterOverhead = normalCollectorInnerOverhead;
        if (normalCollectorEstimatingOuterOverhead > normalCollectorFullOuterOverhead)
            normalCollectorFullOuterOverhead = normalCollectorEstimatingOuterOverhead;
        if (fastCollectorInnerOverhead > fastCollectorEstimatingOuterOverhead)
            fastCollectorEstimatingOuterOverhead = fastCollectorInnerOverhead;
        if (fastCollectorEstimatingOuterOverhead > fastCollectorFullOuterOverhead)
            fastCollectorFullOuterOverhead = fastCollectorEstimatingOuterOverhead;

        return new StackProbeCalibrateInfo(normalCollectorInnerOverhead, normalCollectorEstimatingOuterOverhead, normalCollectorFullOuterOverhead,
                fastCollectorInnerOverhead, fastCollectorEstimatingOuterOverhead, fastCollectorFullOuterOverhead,
                configHash);
    }

    private StackProbeCalibrateInfo loadCalibrateInfo() {
        File file = new File(context.getConfiguration().getWorkPath(), "calibrate.json");
        if (file.exists()) {
            Reader reader = null;
            try {
                reader = new FileReader(file);
                JsonObject object = JsonSerializers.read(reader, false);
                return StackProbeCalibrateInfo.fromJson(object);
            } catch (IOException e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            } finally {
                IOs.close(reader);
            }
        }

        return null;
    }

    private void saveCalibrateInfo(StackProbeCalibrateInfo info) {
        File workPath = context.getConfiguration().getWorkPath();
        workPath.mkdirs();
        File file = new File(workPath, "calibrate.json");
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            JsonSerializers.write(writer, info.toJson(), true);
        } catch (IOException e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        } finally {
            IOs.close(writer);
        }
    }

    private String getConfigHash() {
        ByteOutputStream stream = new ByteOutputStream();
        Serializers.serialize(stream, configuration.getCalibratorConfiguration());
        Serializers.serialize(stream, context.getConfiguration().getTimeSource());

        return Strings.digestToString(digest.digest(stream.toByteArray()));
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return Exceptions.wrapAndThrow(e);
        }
    }

    private class CalibrateStackProbeCollectorFactory implements IStackProbeCollectorFactory {
        @Override
        public StackProbeCollector createCollector(int index, int version, StackProbeCollector parent, Object param) {
            ICallPath callPath = CallPath.get(CallPath.root(), MetricName.get("calibrate"));
            StackProbeRootCollector root = parent.getRoot();
            return new StackProbeCollector(index, callPath, root, parent, null, null);
        }
    }

    private interface IMessages {
        @DefaultMessage("Calibrate info is loaded:\n{0}")
        ILocalizedMessage calibrateLoaded(String string);

        @DefaultMessage("Calibration is started...")
        ILocalizedMessage calibrateStarted();

        @DefaultMessage("Calibration is finished:\n{0}")
        ILocalizedMessage calibrateFinished(String string);
    }
}
