/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.profiler.probes.LogProbeCollector;
import com.exametrika.impl.profiler.probes.LogProbeCollector.CollectorInfo;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.profiler.config.LogProbeConfiguration;


/**
 * The {@link AbstractLogProbe} is a log probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AbstractLogProbe extends AbstractProbe implements IThreadLocalProvider {
    protected final LogProbeConfiguration configuration;
    private IThreadLocalSlot slot;
    private final IExpression transformer;
    private final Map<String, Object> runtimeContext;

    public AbstractLogProbe(LogProbeConfiguration configuration, IProbeContext context, String transformerExpression) {
        super(configuration, context);

        this.configuration = configuration;

        Assert.notNull(transformerExpression);

        CompileContext compileContext = Expressions.createCompileContext(null);
        transformer = Expressions.compile(transformerExpression, compileContext);
        runtimeContext = new HashMap<String, Object>(MeterExpressions.getRuntimeContext());
        runtimeContext.put("log", createLogContext());
    }

    @Override
    public boolean isProbeInterceptor(String className) {
        return className.equals(AbstractLogProbe.class.getName());
    }

    @Override
    public boolean isStack() {
        return false;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        Assert.isTrue(scope.isPermanent() || scope.getEntryPointComponentType() != null);
        JsonObject metadata = Json.object()
                .put("node", context.getConfiguration().getNodeName())
                .put("type", "log,jvm," + (scope.isPermanent() ? "background" : "transaction"))
                .putIf("entry", scope.getEntryPointComponentType(), scope.getEntryPointComponentType() != null)
                .toObject();

        return createLogCollector(scope, metadata, slot);
    }

    @Override
    public void onTimer() {
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = getContainer();
        if (container == null || container.inCall)
            return null;

        container.inCall = true;

        CollectorInfo info = getSlotInfo(slot);
        if (info.collector != null) {
            LogProbeEvent event = transformer.execute(params[0], runtimeContext);
            info.collector.measure(event);
        }

        container.inCall = false;
        return null;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    protected LogProbeCollector createLogCollector(IScope scope, JsonObject metadata, IThreadLocalSlot slot) {
        return new LogProbeCollector(configuration, context, scope, slot, getContainer(), metadata,
                configuration.getComponentType());
    }

    protected abstract ILogExpressionContext createLogContext();

    protected Container getContainer() {
        return (Container) threadLocalAccessor.getContainer();
    }

    protected CollectorInfo getSlotInfo(IThreadLocalSlot slot) {
        return slot.get();
    }
}
