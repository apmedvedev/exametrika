/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.scopes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IClassTransformer;
import com.exametrika.api.instrument.IInstrumentationService;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.instrument.IJoinPointProvider;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.l10n.NonLocalizedMessage;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Fields.IField;
import com.exametrika.common.utils.IFunction;
import com.exametrika.common.utils.StackTraces;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.probes.ProbeInstanceContextProvider;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IThreadLocalAccessor;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.boot.Collector;
import com.exametrika.spi.profiler.boot.ThreadLocalContainer;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ThreadLocalAccessor} is an accessor to thread locals of current thread.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThreadLocalAccessor implements IThreadLocalAccessor {
    public static final boolean underAgent = isUnderAgent();
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ThreadLocalAccessor.class.getName() + ".log");
    private static final ILogger stateLogger = Loggers.get(ThreadLocalAccessor.class.getName() + ".state");
    private static final ILogger fullStateLogger = Loggers.get(ThreadLocalAccessor.class.getName() + ".fullState");
    private static final ILogger measurementsLogger = Loggers.get(ThreadLocalAccessor.class.getName() + ".measurements");
    private static final IField field = getField(false);
    private static final IField targetField = getField(true);
    private final ScopeContext scopeContext;
    private final List<ThreadLocalSlot> slots;
    private final IJoinPointFilter joinPointFilter;
    private final boolean temporary;
    private List<Container> containers = new LinkedList<Container>();
    private boolean closed;
    private final Object sync = new Object();
    private boolean inCall;
    private List<Throwable> errors = new ArrayList<Throwable>();
    private long errorsCount;

    public static class Container extends ThreadLocalContainer {
        public final Thread thread;
        public final ScopeContainer scopes;
        public final ProbeInstanceContextProvider contextProvider;
        public final long[] counters = new long[AppStackCounterType.values().length];
        private final Object[] slots;
        private long startSuspendTime;
        private long suspendTime;
        private long fullSuspendCount;
        private long totalSuspendCount;
        private long suspendFailureCount;
        private StackTraceElement[] traceSnapshot;

        public Container(Thread thread, ScopeContext scopeContext, int slotSize) {
            Assert.notNull(thread);
            Assert.notNull(scopeContext);

            this.thread = thread;
            this.scopes = new ScopeContainer(this, scopeContext);
            this.contextProvider = new ProbeInstanceContextProvider(scopeContext.getProbeContext().getTimeService());
            this.slots = new Object[slotSize];
        }

        @Override
        public long[] getCounters() {
            return counters;
        }

        @Override
        public void activateAll() {
            scopes.activateAll();
        }

        @Override
        public void deactivateAll() {
            scopes.deactivateAll();
        }

        public long getSuspendTime() {
            return suspendTime;
        }

        public long getFullSuspendCount() {
            return fullSuspendCount;
        }

        public long getTotalSuspendCount() {
            return totalSuspendCount;
        }

        public void setTop(Collector collector) {
            if (StackProbeCollector.CHECK_STACK)
                StackProbeCollector.traceTop((StackProbeCollector) top, (StackProbeCollector) collector);

            top = collector;
        }

        public void initTraceSnapshot() {
            traceSnapshot = thread.getStackTrace();
        }

        public StackTraceElement[] getStackTrace() {
            StackTraceElement[] trace = thread.getStackTrace();
            StackTraceElement[] traceSnapshot = this.traceSnapshot;
            if (traceSnapshot != null) {
                for (int i = 0; i < trace.length; i++) {
                    int n = trace.length - 1 - i;
                    int m = traceSnapshot.length - 1 - i;
                    if (m < 0)
                        break;

                    StackTraceElement element = trace[n];
                    StackTraceElement snapshotElement = traceSnapshot[m];

                    if (element.getMethodName() == null) {
                        if (element.getClassName().equals(snapshotElement.getClassName()))
                            trace[n] = snapshotElement;
                    } else
                        break;
                }
            }
            return trace;
        }

        @Override
        public void run() {
        }

        @Override
        public String toString() {
            return thread.getName();
        }

        private void init(List<ThreadLocalSlot> slots, boolean system) {
            Assert.notNull(slots);

            int i = 0;
            for (ThreadLocalSlot slot : slots)
                this.slots[i++] = slot.provider.allocate();

            scopes.init(system);
        }

        private void beginSuspend() {
            suspended = true;
            startSuspendTime = System.nanoTime();
        }

        private void endSuspend(boolean full) {
            suspendTime += System.nanoTime() - startSuspendTime;
            if (full) {
                fullSuspendCount++;
                suspended = false;
            }
            totalSuspendCount++;
        }
    }

    public ThreadLocalAccessor(ProbeContext context) {
        this(new ProfilerConfiguration(context.getConfiguration().getNodeName(), context.getConfiguration().getTimeSource(),
                        Collections.<MeasurementStrategyConfiguration>asSet(), Collections.<ScopeConfiguration>asSet(),
                        Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                        1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, context.getConfiguration().getNodeProperties(), null),
                context.getInstrumentationService(), context.getJoinPointProvider(), context.getClassTransformer(),
                context.getTimeService(), context.getMeasurementHandler(), new MeasurementStrategyManager(), new HashMap(),
                context.getThreadLocalAccessor(), true);
    }

    public ThreadLocalAccessor(ProfilerConfiguration configuration, IInstrumentationService instrumentationService,
                               IJoinPointProvider joinPointProvider, IClassTransformer classTransformer, ITimeService timeService,
                               IMeasurementHandler measurementHandler, MeasurementStrategyManager measurementStrategyManager, Map<String, String> agentArgs) {
        this(configuration, instrumentationService, joinPointProvider, classTransformer, timeService, measurementHandler,
                measurementStrategyManager, agentArgs, null, false);
    }

    public ThreadLocalAccessor(ProfilerConfiguration configuration, IInstrumentationService instrumentationService,
                               IJoinPointProvider joinPointProvider, IClassTransformer classTransformer, ITimeService timeService,
                               IMeasurementHandler measurementHandler, MeasurementStrategyManager measurementStrategyManager, Map<String,
            String> agentArgs, ThreadLocalAccessor parent, boolean temporary) {
        Assert.notNull(configuration);

        this.temporary = temporary;
        scopeContext = new ScopeContext(measurementStrategyManager, new ProbeContext(this, instrumentationService, joinPointProvider, classTransformer,
                timeService, measurementHandler, configuration, agentArgs, measurementStrategyManager));

        List<ThreadLocalSlot> slots;
        if (parent == null) {
            slots = new ArrayList<ThreadLocalSlot>();

            int i = 0;
            for (IThreadLocalProvider provider : scopeContext.getThreadLocalProviders()) {
                ThreadLocalSlot slot = new ThreadLocalSlot(i++, provider);
                provider.setSlot(slot);
                slots.add(slot);
            }
        } else
            slots = parent.slots;

        this.slots = slots;
        this.joinPointFilter = scopeContext.getJoinPointFilter();

        scopeContext.open();
    }

    public ScopeContext getScopeContext() {
        return scopeContext;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public boolean isClosed() {
        return closed;
    }

    public IJoinPointFilter getJoinPointFilter() {
        return joinPointFilter;
    }

    @Override
    public Container getContainer() {
        return get();
    }

    public Container get() {
        Runnable container = (Runnable) field.getObject(Thread.currentThread());
        if (container instanceof Container)
            return (Container) container;

        return create(container, true);
    }

    public Container get(boolean checkThread) {
        Runnable container = (Runnable) field.getObject(Thread.currentThread());
        if (container instanceof Container)
            return (Container) container;

        return create(container, checkThread);
    }

    public Container find() {
        Runnable container = (Runnable) field.getObject(Thread.currentThread());
        if (container instanceof Container)
            return (Container) container;
        else if (container instanceof ThreadLocalContainer)
            return (Container) ((ThreadLocalContainer) container).subContainer;
        else
            return null;
    }

    public void onTimer() {
        synchronized (this) {
            for (Iterator<Container> it = containers.iterator(); it.hasNext(); ) {
                Container container = it.next();
                if (!container.thread.isAlive()) {
                    it.remove();

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.containerRemoved(container.thread.getName()));
                }
            }
        }

        scopeContext.onTimer();

        extract();

        if (stateLogger.isLogEnabled(LogLevel.TRACE)) {
            Json json = Json.object();
            dump(json, IProfilerMXBean.STATE_FLAG);
            stateLogger.log(LogLevel.TRACE, messages.stateDump(json.toObject()));
        }

        if (fullStateLogger.isLogEnabled(LogLevel.TRACE)) {
            Json json = Json.object();
            dump(json, IProfilerMXBean.FULL_STATE_FLAG);
            fullStateLogger.log(LogLevel.TRACE, messages.stateDump(json.toObject()));
        }

        if (measurementsLogger.isLogEnabled(LogLevel.TRACE)) {
            Json json = Json.object();
            dump(json, IProfilerMXBean.MEASUREMENTS_FLAG);
            measurementsLogger.log(LogLevel.TRACE, messages.measurementsDump(json.toObject()));
        }
    }

    public void close() {
        List<Container> containers;
        synchronized (this) {
            if (closed)
                return;

            containers = new ArrayList<Container>(this.containers);
            closed = true;
        }

        if (!temporary) {
            processContainers(containers, new IFunction<Container, Void>() {
                @Override
                public Void evaluate(Container container) {
                    field.setObject(container.thread, null);

                    if (logger.isLogEnabled(LogLevel.DEBUG))
                        logger.log(LogLevel.DEBUG, messages.containerRemoved(container.thread.getName()));
                    return null;
                }
            });
        } else {
            for (Container container : containers)
                field.setObject(container.thread, null);
        }

        synchronized (this) {
            this.containers.clear();
            scopeContext.close();
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.accessorClosed());
    }

    public void log(String message) {
        try {
            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, new NonLocalizedMessage(message));
        } catch (Throwable e) {
        }
    }

    public void logError(Throwable exception) {
        synchronized (errors) {
            if (errors.size() < 1000)
                errors.add(exception);

            errorsCount++;
        }

        try {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, exception);
        } catch (Throwable e) {
        }
    }

    public void dump(Json json, final int flags) {
        List<Container> containers;
        synchronized (this) {
            containers = new ArrayList<Container>(this.containers);
        }

        List<Throwable> errors = null;
        long errorsCount = 0;
        synchronized (this.errors) {
            if (!this.errors.isEmpty())
                errors = new ArrayList<Throwable>(this.errors);

            errorsCount = this.errorsCount;
        }

        if (errors != null || errorsCount > 0) {
            Json jsonErrors = json.putObject("errors");
            jsonErrors.put("errorsCount", errorsCount);
            if (errors != null) {
                Json jsonExceptions = jsonErrors.putArray("exceptions");
                for (Throwable error : errors)
                    Meters.buildExceptionStackTrace(error, Integer.MAX_VALUE, Integer.MAX_VALUE,
                            jsonExceptions.addObject(), true);
            }
        }

        final Json collectors = json.putObject("collectors");
        processContainers(containers, new IFunction<Container, Void>() {
            @Override
            public Void evaluate(Container container) {
                Json thread = collectors.putObject(container.thread.getName());

                if ((flags & IProfilerMXBean.STATE_FLAG) != 0) {
                    if (container.totalSuspendCount > 0) {
                        thread.put("totalSuspendTime", container.suspendTime);
                        thread.put("totalSuspendCount", container.totalSuspendCount);
                        thread.put("fullSuspendCount", container.fullSuspendCount);
                        thread.putIf("suspendFailureCount", container.suspendFailureCount, container.suspendFailureCount > 0);
                        if (container.fullSuspendCount > 0)
                            thread.put("suspendTime", container.suspendTime / container.fullSuspendCount);
                        thread.put("suspendSuccesses%", container.fullSuspendCount * 100 / container.totalSuspendCount);
                    }
                }

                thread.put("scopes", container.scopes.dump(flags));
                for (int k = 0; k < container.slots.length; k++) {
                    if (!(container.slots[k] instanceof IDumpProvider))
                        continue;

                    IDumpProvider dumpProvider = (IDumpProvider) container.slots[k];
                    thread.put(k + ":" + dumpProvider.getName(), dumpProvider.dump(flags));
                }

                if ((flags & IProfilerMXBean.FULL_STATE_FLAG) == IProfilerMXBean.FULL_STATE_FLAG) {
                    Json countersDump = thread.putObject("counters");
                    AppStackCounterType[] values = AppStackCounterType.values();
                    for (int j = 0; j < container.counters.length; j++)
                        countersDump.put(values[j].toString(), container.counters[j]);
                }

                return null;
            }
        });

        json.put("probes", scopeContext.dump(flags));
    }

    public static Runnable getThreadLocal(Thread thread) {
        Assert.notNull(thread);

        return (Runnable) field.getObject(thread);
    }

    public static void setThreadLocal(Thread thread, Runnable target) {
        Assert.notNull(thread);

        field.setObject(thread, target);
    }

    public static Runnable getThreadTarget(Thread thread) {
        Assert.notNull(thread);

        return (Runnable) targetField.getObject(thread);
    }

    public static void setThreadTarget(Thread thread, Runnable target) {
        Assert.notNull(thread);

        targetField.setObject(thread, target);
    }

    private static boolean isUnderAgent() {
        return Fields.get(Thread.class, "_exaTls") != null;
    }

    private static IField getField(boolean targetOnly) {
        if (!targetOnly) {
            IField field = Fields.get(Thread.class, "_exaTls");
            if (field != null)
                return field;
        }

        IField field = Fields.get(Thread.class, "target");
        Assert.checkState(field != null);

        return field;
    }

    synchronized Container create(Runnable runnable, boolean checkThread) {
        if (closed)
            return null;
        if (runnable instanceof ThreadLocalContainer) {
            if (checkThread)
                return null;

            Container subContainer = (Container) ((ThreadLocalContainer) runnable).subContainer;
            if (subContainer != null)
                return (Container) ((ThreadLocalContainer) runnable).subContainer;
        }

        if (inCall)
            return null;

        inCall = true;

        Thread thread = Thread.currentThread();
        if (!temporary && !checkThread(thread)) {
            runnable = new ThreadLocalContainer();
            field.setObject(thread, runnable);

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.threadSkipped(thread.getName()));

            if (checkThread) {
                inCall = false;
                return null;
            }
        }

        Container container = new Container(thread, scopeContext, slots.size());
        if (runnable instanceof ThreadLocalContainer)
            ((ThreadLocalContainer) runnable).subContainer = container;
        else
            field.setObject(thread, container);

        containers.add(container);

        container.init(slots, !checkThread);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.containerCreated(thread.getName()));

        inCall = false;

        return container;
    }

    private boolean checkThread(Thread thread) {
        if (!Debug.isProfile() && thread.getClass().getClassLoader() == getClass().getClassLoader())
            return false;
        else
            return true;
    }

    private void processContainers(List<Container> containers, IFunction<Container, Void> function) {
        synchronized (sync) {
            for (Container container : containers) {
                if (!checkThread(container.thread))
                    continue;

                boolean suspended = false;
                for (int i = 0; i < 10; i++) {
                    container.beginSuspend();
                    Threads.suspend(container.thread);

                    boolean inProbe = inProbe(container);
                    if (inProbe) {
                        Threads.resume(container.thread);
                        container.endSuspend(false);
                        Threads.sleep(1);
                        continue;
                    }

                    try {
                        function.evaluate(container);
                    } finally {
                        Threads.resume(container.thread);
                        container.endSuspend(true);
                        suspended = true;
                    }

                    break;
                }

                if (!suspended) {
                    container.suspendFailureCount++;
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.suspendFailure(container.thread.getName()));
                    if (logger.isLogEnabled(LogLevel.TRACE))
                        logger.log(LogLevel.TRACE, messages.allStackTraces(StackTraces.getAllStackTraces()));
                }
            }
        }
    }

    private void extract() {
        final List<Container> extractedContainers = new ArrayList<Container>();
        synchronized (this) {
            for (Container container : containers) {
                if (container.scopes.isExtractionRequired())
                    extractedContainers.add(container);
            }
        }

        if (extractedContainers.isEmpty())
            return;

        processContainers(extractedContainers, new IFunction<Container, Void>() {
            @Override
            public Void evaluate(Container container) {
                container.scopes.extract();
                return null;
            }
        });

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.measurementsExtracted(extractedContainers));
    }

    private boolean inProbe(Container container) {
        StackTraceElement[] trace = container.thread.getStackTrace();
        for (StackTraceElement element : trace) {
            if (scopeContext.isProbe(element.getClassName()))
                return true;
        }

        return false;
    }

    public class ThreadLocalSlot implements IThreadLocalSlot {
        private final int index;
        private final IThreadLocalProvider provider;

        public ThreadLocalSlot(int index, IThreadLocalProvider provider) {
            Assert.notNull(provider);

            this.index = index;
            this.provider = provider;
        }

        @Override
        public <T> T get() {
            return (T) ThreadLocalAccessor.this.get().slots[index];
        }

        public <T> T get(boolean checkThread) {
            return (T) ThreadLocalAccessor.this.get(checkThread).slots[index];
        }

        @Override
        public String toString() {
            return Integer.toString(index);
        }
    }

    private interface IMessages {
        @DefaultMessage("State dump:\n{0}")
        ILocalizedMessage stateDump(JsonObject object);

        @DefaultMessage("Measurements dump:\n{0}")
        ILocalizedMessage measurementsDump(JsonObject object);

        @DefaultMessage("Thread local accessor is closed.")
        ILocalizedMessage accessorClosed();

        @DefaultMessage("Thread local container is created for thread ''{0}''.")
        ILocalizedMessage containerCreated(String threadName);

        @DefaultMessage("Thread local container is removed for thread ''{0}''.")
        ILocalizedMessage containerRemoved(String threadName);

        @DefaultMessage("Thread ''{0}'' profiling is skipped.")
        ILocalizedMessage threadSkipped(String threadName);

        @DefaultMessage("Thread ''{0}'' can not be suspended.")
        ILocalizedMessage suspendFailure(String threadName);

        @DefaultMessage("All stack traces:\n{0}")
        ILocalizedMessage allStackTraces(String stackTraces);

        @DefaultMessage("Dump before measurements extraction:\n{0}")
        ILocalizedMessage measurementsExtractedDump(JsonObject object);

        @DefaultMessage("Measurements are extracted for threads:{0}")
        ILocalizedMessage measurementsExtracted(List<Container> extractedContainers);
    }
}
