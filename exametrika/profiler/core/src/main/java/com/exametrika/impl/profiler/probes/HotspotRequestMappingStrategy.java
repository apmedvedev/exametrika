/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.profiler.IProfilerMXBean;
import com.exametrika.api.profiler.config.HotspotRequestMappingStrategyConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequestGroupingStrategy;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link HotspotRequestMappingStrategy} is a hotspot request mapping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HotspotRequestMappingStrategy extends SimpleRequestMappingStrategy implements IThreadLocalProvider, IDumpProvider {
    private final HotspotRequestMappingStrategyConfiguration configuration;
    private final IExpression beginValueExpression;
    private final IExpression endValueExpression;
    private final IRequestGroupingStrategy groupingStrategy;
    private final HotspotRequests hotspotRequests;
    private IThreadLocalSlot slot;
    private long nextStartEstimationTime;
    private long nextEndEstimationTime;
    private volatile int estimationCount;
    private volatile boolean estimate = true;

    public HotspotRequestMappingStrategy(HotspotRequestMappingStrategyConfiguration configuration, IProbeContext context) {
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

        groupingStrategy = configuration.getGroupingStrategy().createStrategy(context);

        if (!configuration.isPerThreadStatistics())
            hotspotRequests = new HotspotRequests();
        else
            hotspotRequests = null;
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = slot;
    }

    @Override
    public Object allocate() {
        return new HotspotRequests();
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
        if (configuration.isPerThreadStatistics()) {
            HotspotRequests requests = slot.get();
            return createRequest(scope, request, name, requests);
        } else {
            synchronized (this) {
                return createRequest(scope, request, name, hotspotRequests);
            }
        }
    }

    private SimpleRequest createRequest(IScope scope, Object request, String name, HotspotRequests requests) {
        ScopeHotspotRequests scopeRequests = requests.map.get(scope.getName());
        if (scopeRequests == null) {
            scopeRequests = new ScopeHotspotRequests();
            requests.map.put(scope.getName(), scopeRequests);
        }

        int estimationCount = this.estimationCount;
        if (scopeRequests.estimationCount != estimationCount) {
            scopeRequests.estimationCount = estimationCount;
            detectHotspots(scopeRequests);
        }

        if (!estimate) {
            RequestInfo info = RequestInfo.findHotspot(scopeRequests.map.get(name));
            if (info != null)
                return new HotspotRequest(info.name, request, true, null);
            else
                return null;
        }

        RequestInfo info = getRequestInfo(scopeRequests, scope, request, name, name, 0);
        info.leaf = true;
        RequestInfo hotspot = RequestInfo.findHotspot(info);

        boolean canMeasure = false;
        String requestName = info.name;
        if (hotspot != null) {
            canMeasure = true;
            requestName = hotspot.name;
        }

        return new HotspotRequest(requestName, request, canMeasure, info);
    }

    private RequestInfo getRequestInfo(ScopeHotspotRequests scopeRequests, IScope scope, Object request, String requestName,
                                       String name, int level) {
        RequestInfo info = scopeRequests.map.get(name);
        if (info == null) {
            String group = groupingStrategy.getRequestGroupName(scope, request, requestName, level);
            RequestInfo parent = null;
            if (group != null)
                parent = getRequestInfo(scopeRequests, scope, request, requestName, group, level + 1);
            info = new RequestInfo(scopeRequests, name, parent);
            scopeRequests.map.put(name, info);
        }

        return info;
    }

    private void detectHotspots(ScopeHotspotRequests requests) {
        requests.hotspotCoverage = 0;
        long threshold = requests.total / requests.targetHotspotCount;

        for (RequestInfo info : requests.map.values())
            info.hasHotspots = false;

        for (RequestInfo info : requests.map.values()) {
            if (info.total >= threshold) {
                info.hotspot = true;

                if (info.leaf)
                    requests.hotspotCoverage += (double) info.total / requests.total * 100;

                RequestInfo parent = info.parent;
                while (parent != null) {
                    parent.total -= info.total;
                    parent.count -= info.count;
                    parent.hasHotspots = true;
                    if (parent.total < threshold)
                        parent.hotspot = false;
                    parent = parent.parent;
                }
            } else
                info.hotspot = false;
        }

        for (Iterator<Map.Entry<String, RequestInfo>> it = requests.map.entrySet().iterator(); it.hasNext(); ) {
            RequestInfo info = it.next().getValue();
            info.total = 0;
            info.count = 0;
            info.leaf = false;

            boolean remove = true;
            if (!info.hasHotspots) {
                while (info != null) {
                    if (info.hotspot) {
                        remove = false;
                        break;
                    }

                    info = info.parent;
                }
            } else
                remove = false;

            if (remove)
                it.remove();
        }

        if (requests.hotspotCoverage < configuration.getHotspotCoverage()) {
            if (requests.targetHotspotCount + configuration.getHotspotStep() <= configuration.getMaxHotspotCount())
                requests.targetHotspotCount += configuration.getHotspotStep();
        } else {
            if (requests.targetHotspotCount - configuration.getHotspotStep() >= configuration.getMinHotspotCount())
                requests.targetHotspotCount -= configuration.getHotspotStep();
        }

        requests.total = 0;
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
        private final ScopeHotspotRequests scopeRequests;
        private final String name;
        private final RequestInfo parent;
        private boolean leaf;
        private long total;
        private long count;
        private boolean hotspot;
        private boolean hasHotspots;

        public RequestInfo(ScopeHotspotRequests scopeRequests, String name, RequestInfo parent) {
            this.scopeRequests = scopeRequests;
            this.name = name;
            this.parent = parent;
        }

        public static RequestInfo findHotspot(RequestInfo info) {
            while (info != null) {
                if (info.hotspot)
                    return info;
                info = info.parent;
            }

            return null;
        }
    }

    private class HotspotRequest extends SimpleRequest {
        private final boolean canMeasure;
        private final RequestInfo info;
        private long beginValue;

        public HotspotRequest(String name, Object request, boolean canMeasure, RequestInfo info) {
            super(name, request);

            this.canMeasure = canMeasure;
            this.info = info;
            if (info != null)
                this.beginValue = getBeginValue(request);
        }

        @Override
        public boolean canMeasure() {
            return canMeasure;
        }

        @Override
        public void end() {
            if (configuration.isPerThreadStatistics())
                endRequest();
            else {
                synchronized (HotspotRequestMappingStrategy.this) {
                    endRequest();
                }
            }

            super.end();
        }

        @Override
        protected Object getMetadataRequest() {
            return this;
        }

        private void endRequest() {
            if (info != null) {
                long delta = getEndValue(getRawRequest()) - beginValue;
                info.total += delta;
                info.count++;
                info.scopeRequests.total += delta;

                RequestInfo parent = info.parent;
                while (parent != null) {
                    parent.total += delta;
                    parent.count++;
                    parent = parent.parent;
                }
            }
        }
    }

    private class HotspotRequests implements IDumpProvider {
        private final Map<IScopeName, ScopeHotspotRequests> map = new WeakHashMap<IScopeName, ScopeHotspotRequests>();

        @Override
        public String getName() {
            return HotspotRequestMappingStrategy.this.getName() + ".requests";
        }

        @Override
        public JsonObject dump(int flags) {
            if ((flags & IProfilerMXBean.STATE_FLAG) == 0)
                return null;

            Json json = Json.object();
            for (Map.Entry<IScopeName, ScopeHotspotRequests> entry : map.entrySet()) {
                String scopeName = entry.getKey() != null ? entry.getKey().toString() : "hotspots";
                ScopeHotspotRequests requests = entry.getValue();
                Json child = json.putObject(scopeName);
                child.put("targetHotspotCount", requests.targetHotspotCount)
                        .put("requestCount", requests.map.size())
                        .put("hotspotCoverage", requests.hotspotCoverage)
                        .put("total", requests.total);

                if ((flags & IProfilerMXBean.FULL_STATE_FLAG) == IProfilerMXBean.FULL_STATE_FLAG) {
                    Json requestsDump = child.putObject("estimatedRequests");
                    for (RequestInfo info : requests.map.values())
                        requestsDump.put(info.name, Long.toString(info.total) + "(" + ((double) info.total / info.scopeRequests.total * 100) + ")");
                }
            }

            return json.toObject();
        }
    }

    private class ScopeHotspotRequests {
        private int estimationCount;
        private long total;
        private int targetHotspotCount = configuration.getMinHotspotCount();
        private double hotspotCoverage;
        private final Map<String, RequestInfo> map = new LinkedHashMap<String, RequestInfo>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RequestInfo> eldest) {
                return size() > configuration.getMaxRequestCount();
            }
        };
    }
}
