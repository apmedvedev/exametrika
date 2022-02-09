/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.scopes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.instrument.IJoinPointFilter;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.boot.Bootstrap;
import com.exametrika.impl.profiler.boot.MainInterceptor;
import com.exametrika.impl.profiler.probes.ProbeContext;
import com.exametrika.impl.profiler.probes.StackProbe;
import com.exametrika.impl.profiler.strategies.MeasurementStrategyManager;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistrar;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;
import com.exametrika.spi.profiler.Probes;
import com.exametrika.spi.profiler.config.ProbeConfiguration;


/**
 * The {@link ScopeContext} is a scope context.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ScopeContext {
    public static final long CALIBRATE_DELAY = 30000;
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ScopeContext.class);
    private final ProbeContext probeContext;
    private final Map<String, PermanentScopeInfo> filteredPermanentScopes;
    private final Map<String, ScopeTypeInfo> scopeTypesMap;
    private final List<IThreadLocalProvider> threadLocalProviders;
    private final Map<String, Object> runtimeContext;
    private final IJoinPointFilter joinPointFilter;
    private final long calibrateTime;
    private volatile boolean calibrating;

    public ScopeContext(MeasurementStrategyManager measurementStrategyManager, ProbeContext probeContext) {
        Assert.notNull(measurementStrategyManager);
        Assert.notNull(probeContext);

        this.probeContext = probeContext;

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = Probes.createRuntimeContext(probeContext);

        Map<String, PermanentScopeInfo> filteredPermanentScopes = new HashMap<String, PermanentScopeInfo>();
        for (ScopeConfiguration scopeConfiguration : probeContext.getConfiguration().getPermanentScopes()) {
            if (scopeConfiguration.getThreadFilter() == null)
                continue;

            PermanentScopeInfo info = new PermanentScopeInfo();
            info.filter = Expressions.compile(scopeConfiguration.getThreadFilter(), compileContext);
            filteredPermanentScopes.put(scopeConfiguration.getName(), info);
        }

        this.filteredPermanentScopes = filteredPermanentScopes;

        long currentTime = probeContext.getTimeService().getCurrentTime();

        ThreadLocalProviderRegistry registry = new ThreadLocalProviderRegistry();
        Map<String, ScopeTypeInfo> scopeTypesMap = new HashMap<String, ScopeTypeInfo>();
        int i = 0, k = 0;
        for (ProbeConfiguration probeConfiguration : probeContext.getConfiguration().getProbes()) {
            ScopeTypeInfo info = scopeTypesMap.get(probeConfiguration.getScopeType());
            if (info == null) {
                info = new ScopeTypeInfo(i++);
                scopeTypesMap.put(probeConfiguration.getScopeType(), info);
            }

            IProbe probe = probeConfiguration.createProbe(k++, probeContext);
            IMeasurementStrategy measurementStrategy = null;
            if (probeConfiguration.getMeasurementStrategy() != null) {
                measurementStrategy = measurementStrategyManager.findMeasurementStrategy(
                        probeConfiguration.getMeasurementStrategy());
                Assert.checkState(measurementStrategy != null);
            }
            info.probeInfos.add(new ProbeInfo(probeConfiguration, probe, measurementStrategy, currentTime));
            info.probes.add(probe);
            if (probe instanceof IThreadLocalProviderRegistrar)
                ((IThreadLocalProviderRegistrar) probe).register(registry);
            else if (probe instanceof IThreadLocalProvider)
                registry.providers.add((IThreadLocalProvider) probe);
        }

        this.threadLocalProviders = Immutables.wrap(registry.providers);
        this.scopeTypesMap = scopeTypesMap;
        this.joinPointFilter = findJoinPointFilter();
        this.calibrateTime = Times.getCurrentTime() + CALIBRATE_DELAY;
    }

    public ProbeContext getProbeContext() {
        return probeContext;
    }

    public List<IThreadLocalProvider> getThreadLocalProviders() {
        return threadLocalProviders;
    }

    public IJoinPointFilter getJoinPointFilter() {
        return joinPointFilter;
    }

    public Pair<StackProbeConfiguration, StackProbe> findStackProbe() {
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (ProbeInfo info : type.probeInfos) {
                if (info.probe instanceof StackProbe)
                    return new Pair(info.configuration, info.probe);
            }
        }

        return null;
    }

    public void createPermanentScopes(ScopeContainer container, SimpleList<Scope> permanentScopes, Scope[] scopes, boolean system) {
        for (ScopeConfiguration scopeConfiguration : probeContext.getConfiguration().getPermanentScopes()) {
            ScopeTypeInfo type = scopeTypesMap.get(scopeConfiguration.getType());
            if (type == null)
                continue;

            PermanentScopeInfo info = filteredPermanentScopes.get(scopeConfiguration.getName());
            if (info != null && !info.filter.<Boolean>execute(Thread.currentThread(), runtimeContext))
                continue;

            Scope scope = new Scope(ScopeName.get(getScopeName(scopeConfiguration.getId())),
                    container, type.slotIndex, type.probes, true, false, null, system);
            scopes[type.slotIndex] = scope;
            permanentScopes.addLast(scope.getElement());
            scope.activate();
        }
    }

    public Scope createScope(String name, String type, ScopeContainer container, boolean local, String entryPointComponentType) {
        ScopeTypeInfo info = scopeTypesMap.get(type);
        Assert.isTrue(info != null);

        return new Scope(ScopeName.get(getScopeName(name)), container, info.slotIndex, info.probes, false, local, entryPointComponentType, false);
    }

    public String getScopeName(String name) {
        return probeContext.getConfiguration().getNodeName() + (name != null ? ("." + name) : "");
    }

    public boolean isProbe(String className) {
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (IProbe probe : type.probes) {
                if (probe.isProbeInterceptor(className))
                    return true;
            }
        }

        return false;
    }

    public void onTimer() {
        long currentTime = probeContext.getTimeService().getCurrentTime();

        if (calibrating) {
            if (currentTime >= calibrateTime) {
                boolean force = probeContext.getAgentArgs().containsKey("calibrate");

                for (ScopeTypeInfo type : scopeTypesMap.values()) {
                    for (ProbeInfo info : type.probeInfos)
                        calibrateProbe(info, force);
                }

                endCalibration(true);
            }
        } else {
            for (ScopeTypeInfo type : scopeTypesMap.values()) {
                for (ProbeInfo info : type.probeInfos) {
                    if (enableProbe(info, info.canMeasure(currentTime)))
                        info.probe.onTimer();
                }
            }
        }

        probeContext.onTimer();
    }

    public void open() {
        beginCalibration();
        boolean attached = probeContext.getAgentArgs().containsKey("attached");
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (ProbeInfo info : type.probeInfos) {
                if (attached)
                    calibrateProbe(info, false);

                startProbe(info);
            }
        }
    }

    public void close() {
        endCalibration(false);

        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (ProbeInfo info : type.probeInfos)
                stopProbe(info);
        }
    }

    public JsonObject dump(int flags) {
        Json json = Json.object();
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (ProbeInfo info : type.probeInfos) {
                if ((flags & IProfilerMXBean.STATE_FLAG) != 0)
                    json.putObject(info.configuration.getName()).put("enabled", info.enabled);

                if (!(info.probe instanceof IDumpProvider))
                    continue;

                IDumpProvider dumpProvider = (IDumpProvider) info.probe;
                JsonObject object = dumpProvider.dump(flags);
                if (object != null)
                    json.put(dumpProvider.getName(), object);
            }
        }

        return json.toObject();
    }

    private void startProbe(ProbeInfo info) {
        info.probe.start();
        info.probe.setEnabled(false);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.probeStarted(info.configuration.getName()));
    }

    private void stopProbe(ProbeInfo info) {
        info.probe.stop();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.probeStopped(info.configuration.getName()));
    }

    private void calibrateProbe(ProbeInfo info, boolean force) {
        if (!info.calibrated || force) {
            info.calibrated = true;
            info.probe.calibrate(force);
        }
    }

    private boolean enableProbe(ProbeInfo info, boolean value) {
        if (info.enabled != value) {
            info.enabled = value;
            info.probe.setEnabled(value);

            if (logger.isLogEnabled(LogLevel.DEBUG)) {
                if (value)
                    logger.log(LogLevel.DEBUG, messages.probeEnabled(info.configuration.getName()));
                else
                    logger.log(LogLevel.DEBUG, messages.probeDisabled(info.configuration.getName()));
            }
        }

        return value;
    }

    private void beginCalibration() {
        if (probeContext.getThreadLocalAccessor().isTemporary())
            return;

        boolean force = probeContext.getAgentArgs().containsKey("calibrate");
        boolean calibrationRequired = false;
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (ProbeInfo info : type.probeInfos) {
                info.calibrated = info.probe.isCalibrated();
                if (!info.calibrated) {
                    calibrationRequired = true;
                    break;
                }
            }
        }

        if (calibrationRequired || force) {
            calibrating = true;
            System.out.println(messages.calibrationStarted());
        } else
            endCalibration(true);
    }

    private void endCalibration(boolean full) {
        if (probeContext.getThreadLocalAccessor().isTemporary())
            return;

        if (calibrating) {
            calibrating = false;
            System.out.println(messages.calibrationCompleted());
        }

        if (full) {
            Bootstrap.calibrated = true;
            MainInterceptor.calibrated = true;
        }
    }

    private IJoinPointFilter findJoinPointFilter() {
        for (ScopeTypeInfo type : scopeTypesMap.values()) {
            for (IProbe probe : type.probes) {
                if (probe instanceof StackProbe)
                    return ((StackProbe) probe).getJoinPointFilter();
            }
        }

        return null;
    }

    private static class ScopeTypeInfo {
        private final int slotIndex;
        private final List<ProbeInfo> probeInfos = new ArrayList<ProbeInfo>();
        private final List<IProbe> probes = new ArrayList<IProbe>();

        public ScopeTypeInfo(int slotIndex) {
            this.slotIndex = slotIndex;
        }
    }

    private static class PermanentScopeInfo {
        private IExpression filter;
    }

    private static class ProbeInfo {
        private final ProbeConfiguration configuration;
        private final IProbe probe;
        private final IMeasurementStrategy measurementStrategy;
        private final long startTime;
        private boolean enabled;
        private boolean calibrated;

        public ProbeInfo(ProbeConfiguration configuration, IProbe probe, IMeasurementStrategy measurementStrategy, long currentTime) {
            this.configuration = configuration;
            this.probe = probe;
            this.measurementStrategy = measurementStrategy;
            this.startTime = currentTime;
        }

        public boolean canMeasure(long currentTime) {
            if (!calibrated)
                return false;

            if (currentTime < (startTime + configuration.getWarmupDelay()) ||
                    (measurementStrategy != null && !measurementStrategy.allow()))
                return false;

            return true;
        }
    }

    private static class ThreadLocalProviderRegistry implements IThreadLocalProviderRegistry {
        private final List<IThreadLocalProvider> providers = new ArrayList<IThreadLocalProvider>();

        @Override
        public void addProvider(IThreadLocalProvider provider) {
            Assert.notNull(provider);

            providers.add(provider);
        }
    }

    private interface IMessages {
        @DefaultMessage("Probe ''{0}'' is started.")
        ILocalizedMessage probeStarted(String name);

        @DefaultMessage("Probe ''{0}'' is stopped.")
        ILocalizedMessage probeStopped(String name);

        @DefaultMessage("Probe ''{0}'' is enabled.")
        ILocalizedMessage probeEnabled(String name);

        @DefaultMessage("Probe ''{0}'' is disabled.")
        ILocalizedMessage probeDisabled(String name);

        @DefaultMessage("Calibration has been started...")
        ILocalizedMessage calibrationStarted();

        @DefaultMessage("Calibration has been completed.")
        ILocalizedMessage calibrationCompleted();
    }
}
