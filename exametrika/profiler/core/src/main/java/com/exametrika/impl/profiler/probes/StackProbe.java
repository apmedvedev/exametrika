/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.TIntSet;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.boot.IStackInterceptor;
import com.exametrika.impl.profiler.boot.StackProbeInterceptor;
import com.exametrika.impl.profiler.probes.UltraFastMethodManager.UltraFastMethodInfo;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ScopeContext;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;


/**
 * The {@link StackProbe} is a stack probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackProbe extends BaseProbe implements IStackProbeCollectorFactory, IStackInterceptor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(StackProbe.class);
    private final StackProbeConfiguration configuration;
    private final UltraFastMethodManager ultraFastMethodManager;
    private final AtomicIntegerArray concurrencyCounters;
    private final UUID typeId = UUID.randomUUID();
    private final IMeasurementStrategy stackMeasurementStrategy;
    private volatile StackProbeCalibrateInfo calibrateInfo = new StackProbeCalibrateInfo();
    private Object[] methods;
    private TIntSet methodIndexes;
    private long unblockTime;
    private int invalidationCount = 1;
    private volatile long endEstimationTime;

    public StackProbe(StackProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;

        ProfilerConfiguration profilerConfiguration = context.getConfiguration();
        ultraFastMethodManager = new UltraFastMethodManager(profilerConfiguration.getWorkPath(), profilerConfiguration.getMaxInstrumentedMethodsCount());
        ultraFastMethodManager.load(configuration, context.getConfiguration());

        if (configuration.getConcurrencyLevel().isEnabled())
            concurrencyCounters = new AtomicIntegerArray(context.getConfiguration().getMaxInstrumentedMethodsCount());
        else
            concurrencyCounters = null;

        if (configuration.getStackMeasurementStrategy() != null) {
            stackMeasurementStrategy = context.findMeasurementStrategy(configuration.getStackMeasurementStrategy());
            Assert.checkState(stackMeasurementStrategy != null);
        } else
            stackMeasurementStrategy = null;
    }

    public StackProbeConfiguration getConfiguration() {
        return configuration;
    }

    public StackProbeCalibrateInfo getCalibrateInfo() {
        return calibrateInfo;
    }

    public int getInvalidationCount() {
        return invalidationCount;
    }

    public IJoinPointFilter getJoinPointFilter() {
        return new StackProbeJoinPointFilter();
    }

    public boolean isEstimating() {
        return methods == null;
    }

    public boolean isUltraFastMethod(String method) {
        return ultraFastMethodManager.get().contains(method);
    }

    public void addUltraFastMethods(Set<UltraFastMethod> methods) {
        ultraFastMethodManager.add(methods, this.methods);
    }

    public int incrementConcurrency(int index) {
        return concurrencyCounters.incrementAndGet(index);
    }

    public int decrementConcurrency(int index) {
        return concurrencyCounters.decrementAndGet(index);
    }

    public UUID getTypeId() {
        return typeId;
    }

    public IMeasurementStrategy getStackMeasurementStrategy() {
        return stackMeasurementStrategy;
    }

    @Override
    public synchronized boolean isCalibrated() {
        StackProbeCalibrator calibrator = new StackProbeCalibrator(context, configuration, this);
        StackProbeCalibrateInfo calibrateInfo = calibrator.getCalibrateInfo();
        if (calibrateInfo != null) {
            this.calibrateInfo = calibrateInfo;
            return true;
        } else
            return false;
    }

    @Override
    public synchronized void calibrate(boolean force) {
        StackProbeCalibrator calibrator = new StackProbeCalibrator(context, configuration, this);
        calibrateInfo = calibrator.calibrate(force);
    }

    @Override
    public synchronized void start() {
        StackProbeInterceptor.interceptor = this;

        long estimatedCalibrateTime = isCalibrated() ? 0 : (ScopeContext.CALIBRATE_DELAY + 40000);
        endEstimationTime = configuration.getWarmupDelay() + context.getTimeService().getCurrentTime() +
                configuration.getMaxEstimationPeriod() * 2 + estimatedCalibrateTime;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.estimationPeriodStarted());
    }

    @Override
    public synchronized void stop() {
        StackProbeInterceptor.interceptor = null;
        StackProbeInterceptor.estimationInterceptor = null;
        StackProbeInterceptor.methods = null;
        invalidationCount++;

        ultraFastMethodManager.save(configuration, context.getConfiguration());
    }

    @Override
    public synchronized void setEnabled(boolean value) {
        if (enabled == value)
            return;

        if (enabled)
            StackProbeInterceptor.estimationInterceptor = this;

        enabled = value;
        invalidationCount++;
        ultraFastMethodManager.save(configuration, context.getConfiguration());
    }

    @Override
    public boolean isStack() {
        return true;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        return new StackProbeRootCollector(this, configuration, context, (Scope) scope,
                threadLocalAccessor.get());
    }

    @Override
    public void onTimer() {
        ultraFastMethodManager.save(configuration, context.getConfiguration());
        long currentTime = context.getTimeService().getCurrentTime();
        if (methods == null && endEstimationTime != 0 && currentTime >= endEstimationTime) {
            UltraFastMethodInfo info = ultraFastMethodManager.getInfo();
            if (!info.classes.isEmpty() && context.getClassTransformer() != null)
                context.getClassTransformer().retransformClasses(info.classes);

            methodIndexes = info.methodIndexes;
            unblockTime = context.getTimeService().getCurrentTime() + 5000;
            methods = info.methodBlocks;
            StackProbeInterceptor.methods = methods;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.estimationPeriodFinished());
        } else if (methodIndexes != null && currentTime > unblockTime) {
            methodIndexes.forEach(new TIntProcedure() {
                @Override
                public boolean execute(int index) {
                    if (index < methods.length)
                        methods[index] = null;
                    return true;
                }
            });

            methodIndexes = null;
        }
    }

    @Override
    public Object onEnter(int index, int version) {
        Container container = threadLocalAccessor.get();
        if (container == null)
            return null;

        StackProbeCollector top = (StackProbeCollector) container.top;
        if (!enabled || top == null || container.inCall || top.isBlocked())
            return null;

        if (stackMeasurementStrategy != null && !stackMeasurementStrategy.allow())
            return null;

        container.inCall = true;

        StackProbeCollector child = top.beginMeasure(index, version, this, null, null);
        if (child != null)
            container.setTop(child);

        container.inCall = false;

        return child;
    }

    @Override
    public void onReturn(Object param) {
        StackProbeCollector collector = (StackProbeCollector) param;
        Container container = collector.getRoot().getContainer();

        if (container.inCall)
            return;

        container.inCall = true;
        collector.endMeasure();
        container.inCall = false;
        container.setTop(collector.getParent());
    }

    @Override
    public StackProbeCollector createCollector(int index, int version, StackProbeCollector parent, Object param) {
        StackProbeRootCollector root = parent.getRoot();
        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, version);
        if (joinPoint == null)
            return null;

        Json json = Json.object();
        String threadType;
        if (!root.getScope().isPermanent() && !root.getContext().getThreadLocalAccessor().isTemporary()) {
            EntryPointProbeCollector entryPoint = null;
            StackProbeCollector collector = parent;
            while (collector != null) {
                if (collector instanceof EntryPointProbeCollector) {
                    entryPoint = (EntryPointProbeCollector) collector;
                    break;
                }

                collector = collector.getParent();
            }

            if (entryPoint == null)
                return null;

            threadType = ",transaction";
            json.put("parent", parent.getComponentType())
                    .put("entry", entryPoint.getComponentType());
        } else {
            threadType = ",background";
            json.put("parent", parent.getComponentType());
        }

        JsonObject metadata = json
                .put("node", context.getConfiguration().getNodeName())
                .put("type", "stack,jvm" + threadType)
                .put("class", joinPoint.getClassName())
                .put("line", joinPoint.getSourceLineNumber())
                .put("file", joinPoint.getSourceFileName())
                .put("method", joinPoint.getMethodSignature())
                .toObject();

        IMetricName metric = MetricName.get(joinPoint.getClassName() + "." + joinPoint.getMethodSignature());
        ICallPath callPath = parent.getCallPath().getChild(metric);

        return new StackProbeCollector(index, callPath, root, parent, metadata, null);
    }

    @Override
    public JsonObject dump(int flags) {
        if ((flags & IProfilerMXBean.STATE_FLAG) == 0)
            return null;

        return Json.object()
                .putIf("calibrateInfo", calibrateInfo.toJson(), !calibrateInfo.configHash.isEmpty())
                .put("ultraFastMethods", JsonUtils.toJson(ultraFastMethodManager.get()))
                .toObject();
    }

    private class StackProbeJoinPointFilter implements IJoinPointFilter {
        @Override
        public boolean match(IJoinPoint joinPoint) {
            return !isUltraFastMethod(joinPoint.getClassName() + "." + joinPoint.getMethodSignature());
        }
    }

    private interface IMessages {
        @DefaultMessage("Ultra-fast methods estimation period is started.")
        ILocalizedMessage estimationPeriodStarted();

        @DefaultMessage("Ultra-fast methods estimation period is finished.")
        ILocalizedMessage estimationPeriodFinished();
    }
}
