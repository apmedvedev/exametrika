/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.scopes;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IScope;


/**
 * The {@link Scope} is a scope.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class Scope implements IScope {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(Scope.class);
    private final IScopeName name;
    private final ScopeContainer container;
    private final int slotIndex;
    private final List<IProbeCollector> collectors;
    private final boolean permanent;
    private final boolean local;
    private final Element<Scope> element = new Element<Scope>(this);
    private final IMarker marker;
    private final String entryPointComponentType;
    private Scope previousScope;
    private boolean active;
    private long beginTime;
    private long totalTime;
    private volatile long lastEndTime;

    public Scope(IScopeName name, ScopeContainer container, int slotIndex, List<IProbe> probes, boolean permanent,
                 boolean local, String entryPointComponentType, boolean system) {
        Assert.notNull(name);
        Assert.notNull(container);
        Assert.notNull(probes);

        this.name = name;
        this.container = container;
        this.slotIndex = slotIndex;
        this.permanent = permanent;
        this.local = local;
        this.entryPointComponentType = entryPointComponentType;

        if (!local) {
            lastEndTime = getCurrentTime();
            marker = Loggers.getMarker(name.toString(), Loggers.getMarker(container.getParent().thread.getName()));

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.scopeCreated(permanent));
        } else
            marker = null;

        List<IProbeCollector> collectors = new ArrayList<IProbeCollector>();
        for (IProbe probe : probes) {
            if (system && !probe.isSystem())
                continue;

            if (!permanent) {
                if ((local && probe.isStack()) || (!local && !probe.isStack()))
                    continue;
            }

            IProbeCollector collector = probe.createCollector(this);
            if (collector != null)
                collectors.add(collector);
        }
        this.collectors = collectors;
    }

    public Element<Scope> getElement() {
        return element;
    }

    public ScopeContainer getContainer() {
        return container;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public Scope getPreviousScope() {
        return previousScope;
    }

    public void setPreviousScope(Scope scope) {
        previousScope = scope;
    }

    public long getTotalTime(long currentCpuTime) {
        return totalTime + (active ? (currentCpuTime - beginTime) : 0);
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String getEntryPointComponentType() {
        return entryPointComponentType;
    }

    public boolean isIdle() {
        return !permanent && getCurrentTime() > lastEndTime + container.getContext().getProbeContext().getConfiguration().getMaxScopeIdlePeriod();
    }

    public boolean isExtractionRequired() {
        for (IProbeCollector collector : collectors) {
            if (collector.isExtractionRequired())
                return true;
        }

        return false;
    }

    public void extract() {
        for (IProbeCollector collector : collectors) {
            if (collector.isExtractionRequired())
                collector.extract();
        }
    }

    public void activate() {
        if (active)
            return;

        active = true;

        if (!local) {
            beginTime = container.getContext().getProbeContext().getTimeSource().getCurrentTime();

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.scopeActivated(beginTime));
        }

        for (int i = 0; i < collectors.size(); i++)
            collectors.get(i).begin();
    }

    public void deactivate() {
        if (!active)
            return;

        active = false;

        if (!local) {
            long endTime = container.getContext().getProbeContext().getTimeSource().getCurrentTime();
            long duration = endTime - beginTime;
            totalTime += duration;
            lastEndTime = getCurrentTime();

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, marker, messages.scopeDeactivated(beginTime, endTime, duration, totalTime));
        }

        for (int i = 0; i < collectors.size(); i++)
            collectors.get(i).end();
    }

    @Override
    public IScopeName getName() {
        return name;
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public void begin() {
        if (active)
            return;

        container.activate(this);
        activate();
    }

    @Override
    public void end() {
        if (!active)
            return;

        deactivate();
        container.deactivate(this);
    }

    public JsonObject dump(int flags) {
        JsonObjectBuilder builder = new JsonObjectBuilder();

        if ((flags & IProfilerMXBean.STATE_FLAG) != 0) {
            builder.put("totalTime", totalTime);
            builder.put("permanent", permanent);
        }

        for (IProbeCollector collector : collectors) {
            if (!(collector instanceof IDumpProvider))
                continue;

            IDumpProvider dumpProvider = (IDumpProvider) collector;
            JsonObject object = dumpProvider.dump(flags);
            if (object != null)
                builder.put(dumpProvider.getName(), object);
        }

        return builder.toJson();
    }

    @Override
    public String toString() {
        return name.toString();
    }

    private long getCurrentTime() {
        return container.getContext().getProbeContext().getTimeService().getCurrentTime();
    }

    private interface IMessages {
        @DefaultMessage("Scope is created. Permanent: {0}")
        ILocalizedMessage scopeCreated(boolean permanent);

        @DefaultMessage("Scope is activated. Begin time: {0}")
        ILocalizedMessage scopeActivated(long beginTime);

        @DefaultMessage("Scope is deactivated. Begin time: {0}, end time: {1}, duration: {2}, total time: {3}")
        ILocalizedMessage scopeDeactivated(long beginTime, long endTime, long duration, long totalTime);
    }
}
