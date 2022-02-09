/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IInstrumentationService;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.instrument.IReentrancyListener;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.IProfilingService;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ProfilerRecorderConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceProvider;
import com.exametrika.common.services.IServiceRegistrar;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.meters.AggregatingMeasurementHandler;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.profiler.monitors.MonitorManager;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProfilerMeasurementHandler;


/**
 * The {@link ProfilingService} represents a profiling service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProfilingService implements IProfilingService, IService, IServiceProvider, IProfilerMXBean {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ProfilingService.class);
    private final MeasurementStrategyManager measurementStrategyManager;
    private volatile MonitorManager monitorManager;
    private final ITimeService timeService;
    private final ProfilerMeasurementHandler measurementHandler = new ProfilerMeasurementHandler();
    private IInstrumentationService instrumentationService;
    private volatile ThreadLocalAccessor threadLocalAccessor;
    private volatile ProfilerConfiguration configuration;
    private long lastUpdateTime;
    private volatile long nextDumpTime;
    private IAggregationSchema aggregationSchema;
    private Map<String, String> agentArgs = new LinkedHashMap<String, String>();

    public ProfilingService() {
        measurementStrategyManager = new MeasurementStrategyManager();
        timeService = new SystemTimeService();

        if (!ThreadLocalAccessor.underAgent && System.getProperty("comp.exametrika.hostAgent") == null) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.nativeAgentNotFound());
        }

        if (!Times.isTickCountAvaliable()) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.nativeTimeNotFound());
        }
    }

    @Override
    public IAggregationSchema getAggregationSchema() {
        return aggregationSchema;
    }

    @Override
    public synchronized void setMeasurementHandler(IProfilerMeasurementHandler measurementHandler) {
        this.measurementHandler.measurementHandler = measurementHandler;
    }

    @Override
    public <T extends IMeasurementStrategy> T findMeasurementStrategy(String name) {
        return measurementStrategyManager.findMeasurementStrategy(name);
    }

    @Override
    public void register(IServiceRegistrar registrar) {
        registrar.register(IProfilingService.NAME, this);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        instrumentationService = registry.findService(IInstrumentationService.NAME);
        if (instrumentationService != null) {
            instrumentationService.addJoinPointFilter(new ProfilerJoinPointFilter());
            instrumentationService.setReentrancyListener(new ReentrancyListener());
        } else
            instrumentationService = new InstrumentationServiceStub();
    }

    @Override
    public synchronized void start(IServiceRegistry registry) {
        //Managements.register(MBEAN_NAME, this);

        agentArgs = registry.findParameter("agentArgs");

        monitorManager = new MonitorManager(measurementStrategyManager, measurementHandler, timeService, agentArgs, this);
        monitorManager.start();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());
    }

    @Override
    public synchronized void stop(boolean fromShutdownHook) {
        //Managements.unregister(MBEAN_NAME);

        if (monitorManager != null) {
            monitorManager.stop();
            monitorManager = null;
        }

        if (threadLocalAccessor != null) {
            threadLocalAccessor.close();
            threadLocalAccessor = null;
        }

        setMeasurementHandler(null);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.stopped());
    }

    @Override
    public synchronized void setConfiguration(ILoadContext context) {
        ProfilerConfiguration configuration = context.get(ProfilerConfiguration.SCHEMA);
        measurementStrategyManager.setConfiguration(configuration);

        if (configuration != null) {
            aggregationSchema = configuration.createAggregationSchema();
            measurementHandler.setSchema(aggregationSchema);
        } else
            aggregationSchema = null;

        if (this.configuration == null ||
                !configuration.getNodeName().equals(configuration.getNodeName()) ||
                configuration.getTimeSource() != this.configuration.getTimeSource() ||
                !configuration.getMeasurementStrategies().equals(this.configuration.getMeasurementStrategies()) ||
                !configuration.getPermanentScopes().equals(this.configuration.getPermanentScopes()) ||
                !configuration.getProbes().equals(this.configuration.getProbes()) ||
                configuration.getSchemaVersion() != this.configuration.getSchemaVersion() ||
                !configuration.getWorkPath().equals(this.configuration.getWorkPath()) ||
                configuration.getMaxInstrumentedMethodsCount() != this.configuration.getMaxInstrumentedMethodsCount()) {
            if (threadLocalAccessor != null)
                threadLocalAccessor.close();

            threadLocalAccessor = new ThreadLocalAccessor(configuration, instrumentationService, instrumentationService.getJoinPointProvider(),
                    instrumentationService.getClassTransformer(), timeService, measurementHandler, measurementStrategyManager, agentArgs);
        } else if (threadLocalAccessor != null)
            threadLocalAccessor.getScopeContext().getProbeContext().setConfiguration(configuration);

        if (monitorManager != null)
            monitorManager.setConfiguration(configuration);

        if (configuration != null && !configuration.getDump().isEmpty())
            nextDumpTime = timeService.getCurrentTime() + configuration.getDumpPeriod();
        else
            nextDumpTime = 0;

        this.configuration = configuration;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.configurationUpdated());
    }

    @Override
    public void requestMeasurements() {
        ThreadLocalAccessor threadLocalAccessor = this.threadLocalAccessor;
        if (threadLocalAccessor != null)
            ((AggregatingMeasurementHandler) threadLocalAccessor.getScopeContext().getProbeContext().getStackMeasurementHandler()).requestMeasurements();
    }

    @Override
    public void onTimer(long currentTime) {
        if (lastUpdateTime != 0 && currentTime < lastUpdateTime + 1000)
            return;

        lastUpdateTime = currentTime;

        dump(currentTime);

        measurementStrategyManager.onTimer();

        ThreadLocalAccessor threadLocalAccessor = this.threadLocalAccessor;
        if (threadLocalAccessor != null)
            threadLocalAccessor.onTimer();

        measurementHandler.onTimer(currentTime);
    }

    public synchronized JsonObject dump(int flags) {
        Json json = Json.object();

        measurementStrategyManager.dump(json, flags);

        if (monitorManager != null)
            monitorManager.dump(json, flags);

        ThreadLocalAccessor threadLocalAccessor = this.threadLocalAccessor;
        if (threadLocalAccessor != null)
            threadLocalAccessor.dump(json, flags);

        return json.toObject();
    }

    @Override
    public void dump(String path, int flags) {
        Assert.notNull(path);

        JsonObject dump = dump(flags);

        Writer writer = null;
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(file));
            JsonSerializers.write(writer, dump, true);
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        } finally {
            IOs.close(writer);
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.dumpWritten(path));
    }

    private synchronized void dump(long currentTime) {
        if (configuration != null && nextDumpTime != 0 && currentTime >= nextDumpTime) {
            File file = new File(configuration.getWorkPath(), "dump.json");
            int flags = 0;
            for (DumpType type : configuration.getDump()) {
                switch (type) {
                    case STATE:
                        flags |= IProfilerMXBean.STATE_FLAG;
                        break;
                    case FULL_STATE:
                        flags |= IProfilerMXBean.FULL_STATE_FLAG;
                        break;
                    case MEASUREMENTS:
                        flags |= IProfilerMXBean.MEASUREMENTS_FLAG;
                        break;
                    default:
                        Assert.error();
                }
            }
            dump(file.getPath(), flags);
            this.nextDumpTime = currentTime + configuration.getDumpPeriod();
        }
    }

    private class ProfilerJoinPointFilter implements IJoinPointFilter {
        @Override
        public boolean match(IJoinPoint joinPoint) {
            ThreadLocalAccessor threadLocalAccessor = ProfilingService.this.threadLocalAccessor;
            if (threadLocalAccessor == null)
                return true;

            IJoinPointFilter filter = threadLocalAccessor.getJoinPointFilter();
            if (filter == null)
                return true;

            return filter.match(joinPoint);
        }
    }

    private class ProfilerMeasurementHandler implements IProfilerMeasurementHandler {
        private IProfilerMeasurementHandler measurementHandler;
        private IAggregationSchema schema;
        private final List<MeasurementSet> measurements = new ArrayList<MeasurementSet>();
        private final SerializeNameDictionary dictionary = new SerializeNameDictionary();
        private long lastRecordTime;
        private long startTime = Times.getCurrentTime();
        private long startRecordTime;
        private volatile boolean recorded;

        @Override
        public boolean canHandle() {
            IProfilerMeasurementHandler measurementHandler = this.measurementHandler;
            if (measurementHandler != null)
                return measurementHandler.canHandle();
            else
                return true;
        }

        @Override
        public void handle(MeasurementSet measurements) {
            IProfilerMeasurementHandler measurementHandler = this.measurementHandler;
            if (measurementHandler != null)
                measurementHandler.handle(measurements);

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, messages.measurement(measurements));

            IAggregationSchema schema = this.schema;
            ProfilerConfiguration configuration = ProfilingService.this.configuration;
            if (!recorded && schema != null && configuration != null && configuration.getRecorder() != null)
                addMeasurements(measurements, configuration.getRecorder());
        }

        @Override
        public void setSchema(IAggregationSchema schema) {
            IProfilerMeasurementHandler measurementHandler = this.measurementHandler;
            if (measurementHandler != null)
                measurementHandler.setSchema(schema);

            this.schema = schema;
        }

        public void onTimer(long currentTime) {
            if (!recorded && (lastRecordTime == 0 || currentTime - lastRecordTime > 10000)) {
                lastRecordTime = currentTime;
                IAggregationSchema schema = this.schema;
                ProfilerConfiguration configuration = ProfilingService.this.configuration;
                if (schema != null && configuration != null && configuration.getRecorder() != null)
                    recordMeasurements(currentTime, configuration.getRecorder(), false);
            }
        }

        private synchronized void addMeasurements(MeasurementSet measurements, ProfilerRecorderConfiguration recorder) {
            long currentTime = Times.getCurrentTime();

            if (startRecordTime == 0 && recorder.getDelayPeriod() == 0) {
                startRecordTime = currentTime;
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.startRecording());
            }
            if (startRecordTime > 0) {
                if (currentTime - startRecordTime <= recorder.getRecordPeriod())
                    this.measurements.add(measurements);
                else {
                    recorded = true;
                    recordMeasurements(currentTime, recorder, true);
                    dictionary.reset();
                }
            }
        }

        private synchronized void recordMeasurements(long currentTime, ProfilerRecorderConfiguration recorder, boolean last) {
            if (startRecordTime == 0 && currentTime - startTime > recorder.getDelayPeriod()) {
                startRecordTime = currentTime;
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.startRecording());
                return;
            }

            if (!recorded && measurements.isEmpty())
                return;

            ByteOutputStream outputStream = new ByteOutputStream();
            DataSerialization serialization = new DataSerialization(outputStream);
            serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, dictionary);
            serialization.writeInt(measurements.size());
            for (MeasurementSet set : measurements)
                MeasurementSerializers.serializeMeasurementSet(serialization, set, schema, dictionary);

            measurements.clear();

            if (last)
                serialization.writeInt(0);

            OutputStream out = null;
            try {
                File file = new File(recorder.getFileName() + "-" + SigarHolder.instance.getPid() + ".data");
                file.getParentFile().mkdirs();
                out = new BufferedOutputStream(new FileOutputStream(file, true));
                IOs.copy(new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength()), out);
            } catch (Exception e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            } finally {
                IOs.close(out);
            }

            if (last) {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.endRecording());
            }
        }
    }

    private class ReentrancyListener implements IReentrancyListener {
        @Override
        public Object onTransformEntered() {
            ThreadLocalAccessor threadLocalAccessor = ProfilingService.this.threadLocalAccessor;
            if (threadLocalAccessor == null)
                return null;

            Container container = threadLocalAccessor.get();
            if (container == null || container.inCall)
                return null;

            container.inCall = true;
            return container;
        }

        @Override
        public void onTransformExited(Object param) {
            if (param == null)
                return;

            ((Container) param).inCall = false;
        }
    }

    private static class InstrumentationServiceStub implements IInstrumentationService {
        @Override
        public IJoinPointProvider getJoinPointProvider() {
            return new IJoinPointProvider() {
                @Override
                public int getJoinPointCount() {
                    return 0;
                }

                @Override
                public List<JoinPointEntry> findJoinPoints(String className, String methodName, Class interceptorClass) {
                    return Collections.emptyList();
                }

                @Override
                public IJoinPoint findJoinPoint(int index, int version) {
                    return null;
                }
            };
        }

        @Override
        public IClassTransformer getClassTransformer() {
            return new IClassTransformer() {
                @Override
                public void retransformClasses(Set<String> classNames) {
                }
            };
        }

        @Override
        public void addJoinPointFilter(IJoinPointFilter filter) {
        }

        @Override
        public void setReentrancyListener(IReentrancyListener listener) {
        }
    }

    private interface IMessages {
        @DefaultMessage("Profiling service is started.")
        ILocalizedMessage started();

        @DefaultMessage("Profiling service is stopped.")
        ILocalizedMessage stopped();

        @DefaultMessage("Configuration of profiling service is updated.")
        ILocalizedMessage configurationUpdated();

        @DefaultMessage("Measurements dump has been written: {0}.")
        ILocalizedMessage dumpWritten(String path);

        @DefaultMessage("Measurements - {0}")
        ILocalizedMessage measurement(MeasurementSet measurements);

        @DefaultMessage("Native agent is not found.")
        ILocalizedMessage nativeAgentNotFound();

        @DefaultMessage("Native time library is not found.")
        ILocalizedMessage nativeTimeNotFound();

        @DefaultMessage("Measurements recording has been started.")
        ILocalizedMessage startRecording();

        @DefaultMessage("Measurements recording has been ended.")
        ILocalizedMessage endRecording();
    }

}
