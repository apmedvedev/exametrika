/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.profiler.scopes.Scope;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IMeasurementStrategy;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistrar;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;
import com.exametrika.spi.profiler.IThreadLocalSlot;
import com.exametrika.spi.profiler.Probes;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.TraceTag;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration.PrimaryType;


/**
 * The {@link EntryPointProbe} is an entry point probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class EntryPointProbe extends BaseProbe implements IStackProbeCollectorFactory, IThreadLocalProvider,
        IThreadLocalProviderRegistrar {
    private static final int ROOT_INDEX_START = 100000000;
    private final EntryPointProbeConfiguration configuration;
    private final IRequestMappingStrategy requestMappingStrategy;
    private final int index;
    private final Map<String, Object> runtimeContext;
    private final IExpression primaryEntryPointExpression;
    protected IThreadLocalSlot slot;
    private final IdCache<String> typeIds = new IdCache<String>();
    private final AtomicLong nextTransactionId = new AtomicLong(Times.getCurrentTime());
    private final IMeasurementStrategy stackMeasurementStrategy;

    public EntryPointProbe(EntryPointProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context);

        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.index = index;

        if (configuration.getPrimaryEntryPointExpression() != null) {
            CompileContext compileContext = Expressions.createCompileContext(null);
            runtimeContext = Probes.createRuntimeContext(context);
            primaryEntryPointExpression = Expressions.compile(configuration.getPrimaryEntryPointExpression(), compileContext);
        } else {
            runtimeContext = null;
            primaryEntryPointExpression = null;
        }

        if (configuration.getRequestMappingStrategy() != null)
            this.requestMappingStrategy = configuration.getRequestMappingStrategy().createStrategy(context);
        else
            this.requestMappingStrategy = null;

        if (configuration.getStackMeasurementStrategy() != null) {
            stackMeasurementStrategy = context.findMeasurementStrategy(configuration.getStackMeasurementStrategy());
            Assert.checkState(stackMeasurementStrategy != null);
        } else
            stackMeasurementStrategy = null;
    }

    public final EntryPointProbeConfiguration getConfiguration() {
        return configuration;
    }

    public final IProbeContext getContext() {
        return context;
    }

    public final long getNextTransactionId() {
        return nextTransactionId.incrementAndGet();
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
        return new EntryPointInfo();
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
        Assert.isTrue(parent instanceof StackProbeRootCollector || parent instanceof EntryPointProbeCollector);

        RequestInfo request = (RequestInfo) param;
        StackProbeRootCollector root = parent.getRoot();
        boolean leaf = parent instanceof EntryPointProbeCollector;

        String name;
        JsonObject metadata;
        boolean primary;
        String type = configuration.getType();

        String combineId;
        if (request.tag == null) {
            if (leaf) {
                name = request.request.getName();
                combineId = getInitialCombineId(name);

                metadata = Json.object(request.request.getMetadata())
                        .put("node", context.getConfiguration().getNodeName())
                        .put("type", "primary,transaction," + type)
                        .put("entry", getComponentType(true))
                        .put("combineType", context.getConfiguration().getStackProbe().getCombineType().toString().toLowerCase())
                        .toObject();
            } else {
                name = configuration.getEntryPointType();
                metadata = Json.object()
                        .put("node", context.getConfiguration().getNodeName())
                        .put("type", "primary,transaction," + type)
                        .put("entry", getComponentType(true))
                        .toObject();
                combineId = null;
            }

            primary = true;
        } else {
            if (leaf) {
                name = request.request.getName();
                combineId = request.tag.combineId;
                metadata = Json.object(request.request.getMetadata() != null ? request.request.getMetadata() : JsonUtils.EMPTY_OBJECT)
                        .put("node", context.getConfiguration().getNodeName())
                        .put("type", "secondary,transaction," + type)
                        .put("entry", getComponentType(false))
                        .put("combineType", context.getConfiguration().getStackProbe().getCombineType().toString().toLowerCase())
                        .toObject();
            } else {
                name = configuration.getEntryPointType();
                metadata = Json.object()
                        .put("node", context.getConfiguration().getNodeName())
                        .put("type", "secondary,transaction," + type)
                        .put("entry", getComponentType(false))
                        .toObject();
                combineId = null;
            }

            primary = false;
        }

        IMetricName metric = MetricName.get(Names.escape(name));
        ICallPath callPath = parent.getCallPath().getChild(metric);

        if (!leaf)
            return doCreateCollector(index, null, callPath, "", root, parent, metadata, primary, leaf);
        else
            return doCreateCollector(index, combineId, callPath, name, root, parent, metadata, primary, leaf);
    }

    @Override
    public JsonObject dump(int flags) {
        Json json = Json.object();

        if (requestMappingStrategy instanceof IDumpProvider) {
            IDumpProvider dumpProvider = (IDumpProvider) requestMappingStrategy;
            json.put("requestMappingStrategy", dumpProvider.dump(flags));
        }

        return json.toObject();
    }

    protected final void beginRequest(Container container, Object rawRequest, TraceTag tag) {
        if (tag == null) {
            if (configuration.getAllowPrimary() == PrimaryType.NO ||
                    (primaryEntryPointExpression != null && !primaryEntryPointExpression.<Boolean>execute(rawRequest, runtimeContext)))
                return;
        } else {
            if (configuration.getAllowPrimary() == PrimaryType.ALWAYS) {
                if (primaryEntryPointExpression != null && !primaryEntryPointExpression.<Boolean>execute(rawRequest, runtimeContext))
                    return;

                tag = null;
            } else if (!configuration.isSecondaryAllowed())
                return;
        }

        int index = ROOT_INDEX_START + this.index;

        EntryPointInfo entryPoint = slot.get();

        Assert.checkState(entryPoint.scopeRequest == null);
        Assert.checkState(entryPoint.request == null);
        Assert.checkState(entryPoint.collector == null);

        String scopeName;
        IRequest scopeRequest = mapScope(rawRequest);
        if (scopeRequest == null)
            return;

        entryPoint.scopeRequest = scopeRequest;

        if (!scopeRequest.canMeasure())
            return;

        scopeName = scopeRequest.getName();

        entryPoint.scope = container.scopes.get(scopeName, configuration.getScopeType());
        entryPoint.scope.begin();

        StackProbeRootCollector top = (StackProbeRootCollector) container.top;
        if (top == null || top.isBlocked())
            return;

        container.setTop(null);

        IRequest request;
        if (tag == null) {
            request = mapRequest(entryPoint.scope, rawRequest);
            if (request == null)
                return;

            entryPoint.request = request;

            if (!request.canMeasure())
                return;
        } else {
            String name = tag.combineId;

            if (requestMappingStrategy != null)
                request = requestMappingStrategy.get(name, tag.variant, rawRequest);
            else
                request = new Request(name, rawRequest);

            entryPoint.request = request;
        }

        RequestInfo info = new RequestInfo(tag, request);

        EntryPointProbeCollector entryPointRoot = (EntryPointProbeCollector) top.beginMeasure(index, 0, this, info, null);
        if (entryPointRoot == null)
            return;

        container.setTop(entryPointRoot);
        entryPoint.collector = entryPointRoot;
        if (entryPointRoot.isBlocked())
            return;

        EntryPointProbeCollector entryPointCollector = (EntryPointProbeCollector) entryPointRoot.beginMeasure(index, 0, this, info, null);
        if (entryPointCollector == null)
            return;

        entryPoint.collector = entryPointCollector;

        if (tag != null || stackMeasurementStrategy == null || stackMeasurementStrategy.allow())
            container.setTop(entryPointCollector);
        else
            container.setTop(null);
    }

    protected final void endRequest(Container container, JsonObject error) {
        EntryPointInfo entryPoint = slot.get();

        if (entryPoint.scopeRequest != null) {
            entryPoint.scopeRequest.end();
            entryPoint.scopeRequest = null;
        }

        if (entryPoint.request != null) {
            if (error != null)
                entryPoint.request.setError(error);

            entryPoint.request.end();
            entryPoint.request = null;
        }

        if (entryPoint.collector != null) {
            if (entryPoint.collector.getParent() instanceof EntryPointProbeCollector) {
                entryPoint.collector.endMeasure();
                entryPoint.collector.getParent().endMeasure();
                container.setTop(entryPoint.collector.getParent().getParent());
            } else {
                entryPoint.collector.endMeasure();
                container.setTop(entryPoint.collector.getParent());
            }

            entryPoint.collector = null;
        }

        if (entryPoint.scope != null) {
            entryPoint.scope.end();
            entryPoint.scope = null;
        }
    }

    protected final IRequest getRequest() {
        EntryPointInfo entryPoint = slot.get();
        return entryPoint.request;
    }

    protected final <T extends EntryPointProbeCollector> T getCollector() {
        EntryPointInfo entryPoint = slot.get();
        return (T) entryPoint.collector;
    }

    protected abstract EntryPointProbeCollector doCreateCollector(int index, String combineId,
                                                                  ICallPath callPath, String name, StackProbeRootCollector root,
                                                                  StackProbeCollector parent, JsonObject metadata, boolean primary, boolean leaf);

    protected abstract IRequest mapScope(Object rawRequest);

    protected IRequest mapRequest(IScope scope, Object rawRequest) {
        if (requestMappingStrategy != null)
            return requestMappingStrategy.begin(scope, rawRequest);
        else
            return null;
    }

    protected final boolean isRecursive() {
        EntryPointInfo entryPoint = slot.get();
        return entryPoint.recursive;
    }

    protected final void setRecursive(boolean value) {
        EntryPointInfo entryPoint = slot.get();
        entryPoint.recursive = value;
    }

    protected final String getComponentType(boolean primary) {
        return (primary ? "primary." : "secondary.") + configuration.getComponentType();
    }

    protected final String getInitialCombineId(String name) {
        switch (context.getConfiguration().getStackProbe().getCombineType()) {
            case STACK:
                return null;
            case TRANSACTION:
                return typeIds.get(name).toString();
            case NODE:
                return null;
            case ALL:
                return null;
            default:
                return Assert.error();
        }
    }

    protected static class RequestInfo {
        public final TraceTag tag;
        public final IRequest request;

        public RequestInfo(TraceTag tag, IRequest request) {
            this.tag = tag;
            this.request = request;
        }
    }

    protected static class EntryPointInfo {
        private boolean recursive;
        private IRequest scopeRequest;
        private IRequest request;
        private EntryPointProbeCollector collector;
        private Scope scope;
    }
}
