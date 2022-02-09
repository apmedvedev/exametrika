/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.StackIdsValue;
import com.exametrika.impl.profiler.probes.EntryPointProbe.RequestInfo;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.ITransactionInfo;
import com.exametrika.spi.profiler.TraceTag;


/**
 * The {@link EntryPointProbeCollector} is an entry point probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class EntryPointProbeCollector extends StackProbeCollector {
    private final EntryPointProbe probe;
    private final String name;
    private final String combineId;
    private final boolean primary;
    private final boolean leaf;
    private final Scope localScope;
    private Map<String, EntryPointProbeCollector> collectors;
    private ICounter transactionTimeCounter;
    private ILog stalledRequestsLog;
    private long lastBeginTime;
    private long lastTransactionStartTime;
    private long transactionTimeDelta;
    private IRequest request;
    private boolean hasMeasurement;
    private Set<UUID> stackIds;
    protected ICounter timeCounter;
    protected ICounter receiveBytesCounter;
    protected ICounter sendBytesCounter;
    protected ILog errorsLog;

    public EntryPointProbeCollector(int index, EntryPointProbe probe, String name, String combineId, ICallPath callPath,
                                    StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf) {
        super(index, callPath, root, parent, metadata, createMeasurementId(root, parent, callPath, probe, leaf, primary));

        Assert.notNull(probe);
        Assert.notNull(name);

        this.probe = probe;
        this.name = name;
        this.combineId = combineId;
        this.primary = primary;
        this.leaf = leaf;

        if (leaf) {
            localScope = parent.getRoot().getContainer().scopes.createLocal(getScopePrefix(probe, primary) + callPath.getLastSegment().toString(),
                    probe.getConfiguration().getScopeType(), getComponentType());
        } else
            localScope = null;

        setParentNonSamplingRoot();
    }

    public final String getName() {
        return name;
    }

    public final EntryPointProbe getProbe() {
        return probe;
    }

    public final boolean isPrimary() {
        return primary;
    }

    public final IRequest getRequest() {
        return request;
    }

    @Override
    protected boolean isPermanentHotspotCollector() {
        return true;
    }

    @Override
    public final void extract(long time, long period, boolean force, double approximationMultiplier, List<Measurement> measurements) {
        if (leaf && request != null && time > lastBeginTime + probe.getConfiguration().getMaxDuration() && (hasMeasurement || force) &&
                stalledRequestsLog != null) {
            Container container = getRoot().getContainer();

            ITransactionInfo transaction = getRoot().getTransaction();
            long transactionId = 0;
            if (transaction != null)
                transactionId = transaction.getId();

            StalledLogEvent event = new StalledLogEvent(stalledRequestsLog.getId(), "stall", getProbe().getConfiguration().getEntryPointType(),
                    time, transactionId, container.thread.getName(), null, getRoot().getScope(),
                    time - lastBeginTime, request);
            stalledRequestsLog.measure(event);
            hasMeasurement = false;
        }

        if (localScope != null)
            localScope.extract();

        super.extract(time, period, force, approximationMultiplier, measurements);
    }

    @Override
    protected final void extractMeters(long period, boolean force, double approximationMultiplier, List<IMetricValue> metrics) {
        metrics.add(new StackIdsValue(stackIds));
        super.extractMeters(period, force, approximationMultiplier, metrics);

        stackIds = null;
    }

    @Override
    protected StackProbeCollector findChild(int index, Object param) {
        if (!leaf) {
            RequestInfo info = (RequestInfo) param;
            if (collectors != null)
                return collectors.get(info.request.getName());
            else
                return null;
        } else
            return super.findChild(index, param);
    }

    @Override
    protected StackProbeCollector createCollector(int index, int version, IStackProbeCollectorFactory collectorFactory,
                                                  Object param) {
        if (!leaf) {
            EntryPointProbeCollector collector = (EntryPointProbeCollector) super.createCollector(index, version, collectorFactory, param);

            if (collectors == null)
                collectors = new LinkedHashMap<String, EntryPointProbeCollector>();

            collectors.put(collector.getName(), collector);

            return collector;
        } else
            return super.createCollector(index, version, collectorFactory, param);
    }

    @Override
    protected void removeCollector(StackProbeCollector collector) {
        if (!leaf) {
            EntryPointProbeCollector entryPointCollector = (EntryPointProbeCollector) collector;
            if (collectors != null)
                collectors.remove(entryPointCollector.getName());
        }

        super.removeCollector(collector);
    }

    @Override
    protected final void beginMeasure(Object param) {
        lastBeginTime = getRoot().getContext().getTimeService().getCurrentTime();
        RequestInfo info = (RequestInfo) param;
        request = info.request;
        TraceTag tag = info.tag;

        if (leaf) {
            if (tag == null) {
                getRoot().beginTransaction(combineId, probe.getNextTransactionId(), lastBeginTime);
                lastTransactionStartTime = lastBeginTime;
            } else {
                getRoot().beginTransaction(combineId, tag.transactionId, tag.transactionStartTime);
                lastTransactionStartTime = tag.transactionStartTime;

                if (probe.getContext().getConfiguration().getStackProbe().getCombineType() != CombineType.STACK) {
                    if (stackIds == null)
                        stackIds = new LinkedHashSet<UUID>();

                    stackIds.add(tag.stackId);
                }
            }

            localScope.activate();

            hasMeasurement = true;
        } else {
            if (tag == null)
                lastTransactionStartTime = lastBeginTime;
            else
                lastTransactionStartTime = tag.transactionStartTime;
        }

        super.beginMeasure(param);
    }

    @Override
    protected final void endMeasure() {
        super.endMeasure();

        if (leaf)
            localScope.deactivate();

        request = null;
    }

    @Override
    protected final void createMeters() {
        super.createMeters();

        if (probe.getConfiguration().getStalledRequestsLog().isEnabled())
            stalledRequestsLog = getMeters().addLog(probe.getConfiguration().getComponentType() + ".stalls", probe.getConfiguration().getStalledRequestsLog());
        if (probe.getConfiguration().getTransactionTimeCounter().isEnabled())
            transactionTimeCounter = getMeters().addMeter("app.transaction.time", probe.getConfiguration().getTransactionTimeCounter(), null);

        if (probe.getConfiguration().getTimeCounter().isEnabled())
            timeCounter = getMeters().addMeter("app.request.time", probe.getConfiguration().getTimeCounter(), null);
        if (probe.getConfiguration().getReceiveBytesCounter().isEnabled())
            receiveBytesCounter = getMeters().addMeter("app.receive.bytes", probe.getConfiguration().getReceiveBytesCounter(), null);
        if (probe.getConfiguration().getSendBytesCounter().isEnabled())
            sendBytesCounter = getMeters().addMeter("app.send.bytes", probe.getConfiguration().getSendBytesCounter(), null);
        if (probe.getConfiguration().getErrorsLog().isEnabled())
            errorsLog = getMeters().addLog(getProbe().getConfiguration().getComponentType() + ".errors", probe.getConfiguration().getErrorsLog());

        doCreateMeters();
    }

    @Override
    protected final void clearMeters() {
        super.clearMeters();

        stalledRequestsLog = null;
        transactionTimeCounter = null;
        collectors = null;

        timeCounter = null;
        receiveBytesCounter = null;
        sendBytesCounter = null;
        errorsLog = null;

        doClearMeters();
    }

    @Override
    protected final void beginMeasureMeters(Object param) {
        IProbeContext context = getRoot().getContext();

        boolean hasInstanceFields = getMeters().hasInstanceFields();
        if (hasInstanceFields)
            context.setInstanceContext(request.getParameters());

        long currentCpuTime = context.getTimeSource().getCurrentTime();
        if (transactionTimeCounter != null) {
            transactionTimeDelta = (lastBeginTime - lastTransactionStartTime) * 1000000;
            transactionTimeCounter.beginMeasure(currentCpuTime);
        }

        doBeginMeasure(request, currentCpuTime);

        super.beginMeasureMeters(param);

        if (hasInstanceFields)
            context.setInstanceContext(null);
    }

    @Override
    protected final void endMeasureMeters(boolean disableInherent) {
        IProbeContext context = getRoot().getContext();

        boolean hasInstanceFields = getMeters().hasInstanceFields();
        if (hasInstanceFields)
            context.setInstanceContext(request.getParameters());

        super.endMeasureMeters(disableInherent);

        long currentCpuTime = context.getTimeSource().getCurrentTime();

        doEndMeasure(request, currentCpuTime);

        if (transactionTimeCounter != null)
            transactionTimeCounter.endMeasure(currentCpuTime + transactionTimeDelta);

        if (hasInstanceFields)
            context.setInstanceContext(null);
    }

    @Override
    protected final String getComponentType() {
        return (primary ? "primary." : "secondary.") + probe.getConfiguration().getComponentType();
    }

    @Override
    protected boolean canExtract() {
        return leaf;
    }

    @Override
    protected boolean hasOutliers() {
        return false;
    }

    @Override
    protected boolean allowConcurrency() {
        return false;
    }

    protected void doCreateMeters() {
    }

    protected void doClearMeters() {
    }

    protected void doBeginMeasure(IRequest request, long currentThreadCpuTime) {
    }

    protected void doEndMeasure(IRequest request, long currentThreadCpuTime) {
    }

    @Override
    protected void dump(Json json, int dumpFlags, double approximationMultiplier, long period) {
        if (localScope != null) {
            JsonObject object = localScope.dump(dumpFlags);
            json.putIf("probes", object, object != null);
        }

        super.dump(json, dumpFlags, approximationMultiplier, period);
    }

    private static NameMeasurementId createMeasurementId(StackProbeRootCollector root, StackProbeCollector parent,
                                                         ICallPath callPath, EntryPointProbe probe, boolean leaf, boolean primary) {
        String componentType = (primary ? "primary." : "secondary.") + probe.getConfiguration().getComponentType();
        if (leaf) {
            String prefix = getScopePrefix(probe, primary);
            String scopeName = root.getContext().getConfiguration().getNodeName() + "." + prefix + callPath.getLastSegment().toString();
            return new NameMeasurementId(ScopeName.get(scopeName), CallPath.root(), componentType);
        } else
            return new NameMeasurementId(parent.getId().getScope(),
                    ((ICallPath) parent.getId().getLocation()).getChild(callPath.getLastSegment()), componentType);
    }

    private static String getScopePrefix(EntryPointProbe probe, boolean primary) {
        return probe.getConfiguration().getEntryPointType() + "-";
    }
}
