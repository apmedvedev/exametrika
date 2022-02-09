/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.StackProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.boot.StackProbeInterceptor;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistrar;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.ITransactionInfo;
import com.exametrika.spi.profiler.TraceTag;
import com.exametrika.spi.profiler.boot.Collector;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;


/**
 * The {@link ExitPointProbe} is an exit point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ExitPointProbe extends BaseProbe implements IStackProbeCollectorFactory, IThreadLocalProvider,
        IThreadLocalProviderRegistrar {
    static final int ROOT_INDEX_START = 100000000;
    protected final ExitPointProbeConfiguration configuration;
    private final IRequestMappingStrategy requestMappingStrategy;
    private final int index;
    private final String name;
    private final IdCache<Pair<IScopeName, ICallPath>> stackIds = new IdCache<Pair<IScopeName, ICallPath>>();
    protected IThreadLocalSlot slot;
    private volatile ExitPointProbeCalibrateInfo calibrateInfo = new ExitPointProbeCalibrateInfo();

    public ExitPointProbe(ExitPointProbeConfiguration configuration, IProbeContext context, int index, String name) {
        super(configuration, context);

        Assert.notNull(configuration);
        Assert.notNull(context);
        Assert.notNull(name);

        this.configuration = configuration;
        this.index = index;
        this.name = name;

        if (configuration.getRequestMappingStrategy() != null)
            this.requestMappingStrategy = configuration.getRequestMappingStrategy().createStrategy(context);
        else
            this.requestMappingStrategy = null;
    }

    public ExitPointProbeConfiguration getConfiguration() {
        return configuration;
    }

    public ExitPointProbeCalibrateInfo getCalibrateInfo() {
        return calibrateInfo;
    }

    @Override
    public synchronized boolean isCalibrated() {
        Pair<StackProbeConfiguration, StackProbe> pair = threadLocalAccessor.getScopeContext().findStackProbe();
        ExitPointProbeCalibrator calibrator = new ExitPointProbeCalibrator(context, configuration, this,
                pair.getKey(), pair.getValue(), name);
        ExitPointProbeCalibrateInfo calibrateInfo = calibrator.getCalibrateInfo();
        if (calibrateInfo != null) {
            this.calibrateInfo = calibrateInfo;
            return true;
        } else
            return false;
    }

    @Override
    public void calibrate(boolean force) {
        Pair<StackProbeConfiguration, StackProbe> pair = threadLocalAccessor.getScopeContext().findStackProbe();
        ExitPointProbeCalibrator calibrator = new ExitPointProbeCalibrator(context, configuration, this,
                pair.getKey(), pair.getValue(), name);
        calibrateInfo = calibrator.calibrate(force);
    }

    @Override
    public boolean isStack() {
        return true;
    }

    @Override
    public final IProbeCollector createCollector(IScope scope) {
        return null;
    }

    @Override
    public final void setSlot(IThreadLocalSlot slot) {
        Assert.notNull(slot);

        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new ExitPointInfo();
    }

    @Override
    public void register(IThreadLocalProviderRegistry registry) {
        registry.addProvider(this);
        if (requestMappingStrategy instanceof IThreadLocalProviderRegistrar)
            ((IThreadLocalProviderRegistrar) requestMappingStrategy).register(registry);
        else if (requestMappingStrategy instanceof IThreadLocalProvider)
            registry.addProvider((IThreadLocalProvider) requestMappingStrategy);
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void onTimer() {
        if (requestMappingStrategy != null)
            requestMappingStrategy.onTimer(context.getTimeService().getCurrentTime());
    }

    @Override
    public final StackProbeCollector createCollector(int index, int version, StackProbeCollector parent, Object param) {
        String name;
        JsonObject metadata = null;
        boolean leaf = parent instanceof ExitPointProbeCollector;
        if (leaf) {
            IRequest request = (IRequest) param;
            name = request.getName();
            metadata = request.getMetadata();
        } else
            name = configuration.getExitPointType();

        StackProbeRootCollector root = parent.getRoot();
        Json json = Json.object(metadata != null ? metadata : JsonUtils.EMPTY_OBJECT);
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
            json.put("parent", leaf ? parent.getParent().getComponentType() : parent.getComponentType())
                    .put("entry", entryPoint.getComponentType());
        } else {
            threadType = ",background";
            json.put("parent", leaf ? parent.getParent().getComponentType() : parent.getComponentType());
        }

        IMetricName metric = MetricName.get(Names.escape(name));
        ICallPath callPath = parent.getCallPath().getChild(metric);

        UUID stackId = null;
        if (leaf && configuration.isIntermediate()) {
            Pair<IScopeName, ICallPath> id = new Pair<IScopeName, ICallPath>(parent.getId().getScope(),
                    ((ICallPath) parent.getId().getLocation()).getChild(callPath.getLastSegment()));
            stackId = stackIds.get(id);
        }

        metadata = json
                .put("node", context.getConfiguration().getNodeName())
                .putIf("stackId", stackId != null ? stackId.toString() : null, stackId != null)
                .put("type", configuration.getType() + threadType)
                .toObject();

        if (!leaf)
            return doCreateCollector(index, "", null, callPath, root, parent, metadata, calibrateInfo, leaf);
        else
            return doCreateCollector(index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    public JsonObject dump(int flags) {
        Json json = Json.object();

        if ((flags & IProfilerMXBean.STATE_FLAG) != 0 && !calibrateInfo.configHash.isEmpty())
            json.put("calibrateInfo", calibrateInfo.toJson());

        if (requestMappingStrategy instanceof IDumpProvider) {
            IDumpProvider dumpProvider = (IDumpProvider) requestMappingStrategy;
            json.put("requestMappingStrategy", dumpProvider.dump(flags));
        }

        return json.toObject();
    }

    protected final void beginRequest(Container container, Object rawRequest) {
        int index = ROOT_INDEX_START + this.index;

        ExitPointInfo exitPoint = slot.get();

        StackProbeCollector top = (StackProbeCollector) container.top;
        if (top.isBlocked() || (top == top.getRoot() && (top.getRoot().getProbe().getStackMeasurementStrategy() == null ||
                top.getRoot().getProbe().getStackMeasurementStrategy().allow()))) {
            if (configuration.isPermanentHotspot() && StackProbeInterceptor.methods != null)
                top.setPermanentHotspot(true);

            return;
        }

        Assert.checkState(exitPoint.request == null);
        Assert.checkState(exitPoint.collector == null);

        exitPoint.top = top;
        container.setTop(null);

        ExitPointProbeCollector exitPointCollector;

        IRequest request = mapRequest(top.getRoot().getScope(), rawRequest);
        if (request == null)
            return;

        exitPoint.request = request;

        if (!request.canMeasure())
            return;

        ExitPointProbeCollector exitPointRoot = (ExitPointProbeCollector) top.beginMeasure(index, 0, this, request,
                ExitPointProbeCollector.class);
        if (exitPointRoot == null)
            return;

        container.setTop(exitPointRoot);
        exitPoint.collector = exitPointRoot;
        if (exitPointRoot.isBlocked())
            return;

        exitPointCollector = (ExitPointProbeCollector) exitPointRoot.beginMeasure(index, 0, this, request, null);

        if (exitPointCollector == null)
            return;

        if (configuration.isIntermediate()) {
            ITransactionInfo transaction = exitPointCollector.getRoot().getTransaction();

            String combineId;
            switch (context.getConfiguration().getStackProbe().getCombineType()) {
                case STACK:
                    combineId = exitPointCollector.getStackId().toString();
                    break;
                case TRANSACTION:
                    if (transaction == null)
                        combineId = exitPointCollector.getRoot().getProbe().getTypeId().toString();
                    else
                        combineId = transaction.getCombineId().toString();
                    break;
                case NODE:
                    combineId = context.getConfiguration().getNodeName();
                    break;
                case ALL:
                    combineId = "all";
                    break;
                default:
                    combineId = Assert.error();
            }
            long transactionId = 0;
            long transactionStartTime;
            if (transaction != null) {
                transactionId = transaction.getId();
                transactionStartTime = transaction.getStartTime();
            } else
                transactionStartTime = context.getTimeService().getCurrentTime();

            writeTag(rawRequest, new TraceTag(combineId, exitPointCollector.getStackId(), transactionId,
                    transactionStartTime, request.getVariant()));
        }

        exitPoint.collector = exitPointCollector;
        container.setTop(exitPointCollector);
    }

    protected final void endRequest(Container container, JsonObject error, long totalDelta, long childrenTotalDelta) {
        ExitPointInfo exitPoint = slot.get();

        if (exitPoint.request != null) {
            if (error != null)
                exitPoint.request.setError(error);

            exitPoint.request.end();
            exitPoint.request = null;
        }

        if (exitPoint.collector != null) {
            if (exitPoint.collector.getParent() instanceof ExitPointProbeCollector) {
                exitPoint.collector.addChildrenTotalDelta(childrenTotalDelta);
                exitPoint.collector.endMeasure(totalDelta);
                exitPoint.collector.getParent().endMeasure(totalDelta);
            } else
                exitPoint.collector.endMeasure(totalDelta);
        }

        if (exitPoint.top != null)
            container.setTop(exitPoint.top);

        exitPoint.collector = null;
        exitPoint.top = null;
    }

    protected final IRequest getRequest() {
        ExitPointInfo exitPoint = slot.get();
        return exitPoint.request;
    }

    protected final <T extends ExitPointProbeCollector> T getCollector() {
        ExitPointInfo exitPoint = slot.get();
        return (T) exitPoint.collector;
    }

    protected final boolean isRecursive() {
        ExitPointInfo exitPoint = slot.get();
        return exitPoint.recursive;
    }

    protected final void setRecursive(boolean value) {
        ExitPointInfo exitPoint = slot.get();
        exitPoint.recursive = value;
    }

    protected abstract ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                                 StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo,
                                                                 boolean leaf);

    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        if (requestMappingStrategy != null)
            return requestMappingStrategy.begin(scope, rawRequest);
        else
            return null;
    }

    protected final void writeTag(Container container, Object rawRequest) {
        Collector top = container.top;
        beginRequest(container, rawRequest);

        container.setTop(top);

        ExitPointInfo exitPoint = slot.get();
        exitPoint.collector = null;
        exitPoint.request = null;
    }

    protected void writeTag(Object request, TraceTag tag) {
        Assert.supports(false);
    }

    protected final long getStartTime() {
        if (Times.isTickCountAvaliable())
            return Times.getTickCount();
        else
            return context.getTimeSource().getCurrentTime();
    }

    protected final long getTimeDelta(long startTime) {
        if (Times.isTickCountAvaliable())
            return (long) ((Times.getTickCount() - startTime) / Times.getTickFrequency());
        else
            return context.getTimeSource().getCurrentTime() - startTime;
    }

    protected abstract Object createCalibratingRequest();

    protected static class ExitPointInfo {
        private boolean recursive;
        private IRequest request;
        private StackProbeCollector top;
        private StackProbeCollector collector;
    }
}
