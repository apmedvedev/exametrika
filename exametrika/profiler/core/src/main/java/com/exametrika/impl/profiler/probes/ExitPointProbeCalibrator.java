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
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link ExitPointProbeCalibrator} is a exit point probe calibrator.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExitPointProbeCalibrator {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ExitPointProbeCalibrator.class);
    private static final MessageDigest digest = getMessageDigest();
    private final ProbeContext context;
    private final ExitPointProbeConfiguration exitPointProbeConfiguration;
    private final ExitPointProbe exitPointProbe;
    private final String name;
    private final StackProbeConfiguration stackProbeConfiguration;
    private final StackProbe stackProbe;

    public ExitPointProbeCalibrator(ProbeContext context, ExitPointProbeConfiguration exitPointProbeConfiguration,
                                    ExitPointProbe exitPointProbe, StackProbeConfiguration stackProbeConfiguration, StackProbe stackProbe,
                                    String name) {
        Assert.notNull(context);
        Assert.notNull(exitPointProbeConfiguration);
        Assert.notNull(exitPointProbe);
        Assert.notNull(stackProbeConfiguration);
        Assert.notNull(stackProbe);
        Assert.notNull(name);

        this.context = context;
        this.exitPointProbeConfiguration = exitPointProbeConfiguration;
        this.exitPointProbe = exitPointProbe;
        this.stackProbeConfiguration = stackProbeConfiguration;
        this.stackProbe = stackProbe;
        this.name = name;
    }

    public ExitPointProbeCalibrateInfo getCalibrateInfo() {
        String configHash = getConfigHash();
        ExitPointProbeCalibrateInfo calibrateInfo = loadCalibrateInfo();
        if (calibrateInfo == null || !calibrateInfo.configHash.equals(configHash))
            return null;
        else
            return calibrateInfo;
    }

    public ExitPointProbeCalibrateInfo calibrate(boolean force) {
        String configHash = getConfigHash();
        ExitPointProbeCalibrateInfo calibrateInfo = loadCalibrateInfo();
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

    private ExitPointProbeCalibrateInfo calibrate(String configHash) {
        ThreadLocalAccessor.setThreadLocal(Thread.currentThread(), null);
        ThreadLocalAccessor calibrateAccessor = new ThreadLocalAccessor(context);
        Container container = calibrateAccessor.get();

        int index = ExitPointProbe.ROOT_INDEX_START;
        ProbeContext calibrateContext = new ProbeContext(
                calibrateAccessor, context.getInstrumentationService(), context.getJoinPointProvider(), context.getClassTransformer(),
                context.getTimeService(), context.getMeasurementHandler(), context.getConfiguration(),
                new HashMap(), context.getMeasurementStrategyManager());
        StackProbeRootCollector root = new StackProbeRootCollector(stackProbe, stackProbeConfiguration, calibrateContext,
                new Scope(ScopeName.get("calibrate"), calibrateAccessor.get().scopes, 0,
                        java.util.Collections.<IProbe>emptyList(), false, false, null, false), container);

        Object rawRequest = exitPointProbe.createCalibratingRequest();
        IRequest request = exitPointProbe.mapRequest(root.getScope(), rawRequest);

        IStackProbeCollectorFactory stackProbeCollectorFactory = new CalibrateStackProbeCollectorFactory();
        StackProbeCollector parent = root.beginMeasure(index + 0, 0, stackProbeCollectorFactory, null, null);
        parent.endMeasure();
        parent.setCalibrate(true, false);
        container.setTop(parent);

        ExitPointProbeCollector collector = (ExitPointProbeCollector) parent.beginMeasure(index + 0, 0, exitPointProbe, request, null);
        collector.endMeasure();
        collector.setCalibrate(true, false);

        for (int i = 0; i < 100000; i++) {
            exitPointProbe.beginRequest(container, request.getRawRequest());
            exitPointProbe.endRequest(container, null, 0, 0);
        }

        int normalCount = 1000000;

        long t = System.nanoTime();
        for (int i = 0; i < normalCount; i++) {
            exitPointProbe.beginRequest(container, request.getRawRequest());
            exitPointProbe.endRequest(container, null, 0, 0);
        }

        long normalCollectorFullOuterOverhead = (System.nanoTime() - t) / normalCount;

        long fastCollectorFullOuterOverhead = 0;
        if (Times.isTickCountAvaliable()) {
            int fastCount = 1000000;

            collector = (ExitPointProbeCollector) parent.beginMeasure(index + 0, 0, exitPointProbe, request, null);
            collector.endMeasure();
            collector.setCalibrate(true, true);

            for (int i = 0; i < 100000; i++) {
                exitPointProbe.beginRequest(container, request.getRawRequest());
                exitPointProbe.endRequest(container, null, 0, 0);
            }

            t = System.nanoTime();
            for (int i = 0; i < fastCount; i++) {
                exitPointProbe.beginRequest(container, request.getRawRequest());
                exitPointProbe.endRequest(container, null, 0, 0);
            }

            fastCollectorFullOuterOverhead = (System.nanoTime() - t) / fastCount;
        }

        calibrateAccessor.close();

        return new ExitPointProbeCalibrateInfo(normalCollectorFullOuterOverhead, fastCollectorFullOuterOverhead, configHash);
    }

    private ExitPointProbeCalibrateInfo loadCalibrateInfo() {
        File file = new File(context.getConfiguration().getWorkPath(), name + ".json");
        if (file.exists()) {
            Reader reader = null;
            try {
                reader = new FileReader(file);
                JsonObject object = JsonSerializers.read(reader, false);
                return ExitPointProbeCalibrateInfo.fromJson(object);
            } catch (IOException e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            } finally {
                IOs.close(reader);
            }
        }

        return null;
    }

    private void saveCalibrateInfo(ExitPointProbeCalibrateInfo info) {
        File workPath = context.getConfiguration().getWorkPath();
        workPath.mkdirs();
        File file = new File(workPath, name + ".json");
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
        Serializers.serialize(stream, exitPointProbeConfiguration);
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
