/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.ThresholdRequestMappingStrategyConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ThresholdRequestMappingStrategy} is a threshold request mapping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ThresholdRequestMappingStrategy extends SimpleRequestMappingStrategy implements IThreadLocalProvider, IDumpProvider {
    private final ThresholdRequestMappingStrategyConfiguration configuration;
    private final IExpression beginValueExpression;
    private final IExpression endValueExpression;
    private IThreadLocalSlot slot;
    private long nextStartEstimationTime;
    private long nextEndEstimationTime;
    private volatile int estimationCount;
    private volatile boolean estimate = true;

    public ThresholdRequestMappingStrategy(ThresholdRequestMappingStrategyConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        CompileContext compileContext = Expressions.createCompileContext(null);

        this.configuration = configuration;

        if (configuration.getBeginValueExpression() != null)
            beginValueExpression = Expressions.compile(configuration.getBeginValueExpression(), compileContext);
        else
            beginValueExpression = null;
        if (configuration.getEndValueExpression() != null)
            endValueExpression = Expressions.compile(configuration.getEndValueExpression(), compileContext);
        else
            endValueExpression = null;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new ThresholdRequests();
    }

    @Override
    public void onTimer(long currentTime) {
        long fullPeriod = configuration.getEstimationPeriod() + configuration.getMeasurementPeriod();

        if (nextStartEstimationTime == 0) {
            nextEndEstimationTime = (currentTime / fullPeriod + 1) * fullPeriod;
            nextStartEstimationTime = currentTime;
        } else if (currentTime >= nextStartEstimationTime && !estimate)
            estimate = true;

        if (currentTime >= nextEndEstimationTime) {
            nextEndEstimationTime = (currentTime / fullPeriod + 1) * fullPeriod;
            nextStartEstimationTime = nextEndEstimationTime - configuration.getEstimationPeriod();
            estimationCount++;
            if (configuration.getMeasurementPeriod() > 0)
                estimate = false;
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public JsonObject dump(int flags) {
        if ((flags & IProfilerMXBean.STATE_FLAG) == 0)
            return null;

        return Json.object().put("slot", slot).toObject();
    }

    @Override
    protected SimpleRequest createRequest(IScope scope, Object request, String name) {
        ThresholdRequests requests = slot.get();
        ScopeThresholdRequests scopeRequests = requests.map.get(scope);
        if (scopeRequests == null) {
            scopeRequests = new ScopeThresholdRequests();
            requests.map.put(scope, scopeRequests);
        }

        int estimationCount = this.estimationCount;
        if (scopeRequests.estimationCount != estimationCount) {
            scopeRequests.estimationCount = estimationCount;
            detectThresholds(scopeRequests);
        }

        RequestInfo info = scopeRequests.map.get(name);
        if (!estimate) {
            if (info != null && info.threshold)
                return new ThresholdRequest(scopeRequests, name, request, true, null, false);
            else
                return null;
        }

        if (info != null)
            return new ThresholdRequest(scopeRequests, name, request, info.threshold, info, true);
        else
            return new ThresholdRequest(scopeRequests, name, request, false, null, true);
    }

    private void detectThresholds(ScopeThresholdRequests requests) {
        for (Iterator<Map.Entry<String, RequestInfo>> it = requests.map.entrySet().iterator(); it.hasNext(); ) {
            RequestInfo info = it.next().getValue();
            info.threshold = Numbers.percents(info.thresholdCount, info.count) >= configuration.getRequestPercentage();
            info.count = 0;
            info.thresholdCount = 0;

            if (!info.threshold)
                it.remove();
        }
    }

    private long getBeginValue(Object request) {
        if (configuration.getBeginValueExpression() == null) {
            if (configuration.getEndValueExpression() == null)
                return context.getTimeSource().getCurrentTime();
            else
                return 0;
        } else
            return beginValueExpression.execute(request, runtimeContext);
    }

    private long getEndValue(Object request) {
        if (configuration.getEndValueExpression() == null)
            return context.getTimeSource().getCurrentTime();
        else
            return endValueExpression.execute(request, runtimeContext);
    }

    private static class RequestInfo {
        boolean threshold;
        long count;
        long thresholdCount;
    }

    private class ThresholdRequest extends SimpleRequest {
        private final ScopeThresholdRequests parent;
        private final boolean canMeasure;
        private final RequestInfo info;
        private final boolean estimate;
        private long beginValue;

        public ThresholdRequest(ScopeThresholdRequests parent, String name, Object request, boolean canMeasure,
                                RequestInfo info, boolean estimate) {
            super(name, request);

            this.parent = parent;
            this.canMeasure = canMeasure;
            this.info = info;
            this.estimate = estimate;

            if (estimate)
                this.beginValue = getBeginValue(request);
        }

        @Override
        public boolean canMeasure() {
            return canMeasure;
        }

        @Override
        public void end() {
            if (estimate) {
                boolean thresholdExceeded = getEndValue(getRawRequest()) - beginValue >= configuration.getThreshold();
                RequestInfo info = this.info;
                if (info == null) {
                    info = new RequestInfo();
                    parent.map.put(getName(), info);
                }

                if (thresholdExceeded)
                    info.thresholdCount++;

                info.count++;
            }

            super.end();
        }
    }

    private class ThresholdRequests implements IDumpProvider {
        private final Map<IScope, ScopeThresholdRequests> map = new WeakHashMap<IScope, ScopeThresholdRequests>();

        @Override
        public String getName() {
            return ThresholdRequestMappingStrategy.this.getName() + ".requests";
        }

        @Override
        public JsonObject dump(int flags) {
            if ((flags & IProfilerMXBean.STATE_FLAG) == 0)
                return null;

            Json json = Json.object();
            for (Map.Entry<IScope, ScopeThresholdRequests> entry : map.entrySet()) {
                String scopeName = entry.getKey() != null ? entry.getKey().toString() : "thresholds";
                json.put(scopeName, JsonUtils.toJson(entry.getValue().map.keySet()));
            }

            return json.toObject();
        }
    }

    private class ScopeThresholdRequests {
        private int estimationCount;
        private final Map<String, RequestInfo> map = new LinkedHashMap<String, RequestInfo>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RequestInfo> eldest) {
                return size() > configuration.getMaxRequestCount();
            }
        };
    }
}
