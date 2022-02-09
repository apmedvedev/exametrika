/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;


/**
 * The {@link JvmNodeSelector} is an jvm node selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmNodeSelector extends ApplicationSelector {
    public JvmNodeSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "jvm.kpi");
    }

    @Override
    protected boolean isTransaction() {
        return false;
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");
        if (type.equals("processState"))
            return selectProcessState(parameters);
        else if (type.equals("processMemory"))
            return selectProcessMemory(parameters);
        else if (type.equals("processThreads"))
            return selectProcessThreads(parameters);
        else if (type.equals("processFileDescriptors"))
            return selectProcessFileDescriptors(parameters);
        else if (type.equals("processPageFaults"))
            return selectProcessPageFaults(parameters);
        else if (type.equals("processShortProperties"))
            return selectProcessShortProperties(parameters);
        else if (type.equals("heaps"))
            return selectHeaps(parameters);
        else if (type.equals("heapUsage"))
            return selectHeapUsage(parameters);
        else if (type.equals("heapGcTime"))
            return selectHeapGcTime(parameters);
        else if (type.equals("heapGcBytes"))
            return selectHeapGcBytes(parameters);
        else if (type.equals("heapGcTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "jvm.pool", "jvm.gc.time", 0, parameters);
        else if (type.equals("heapGcBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "jvm.pool", "jvm.gc.bytes", 0, parameters);
        else if (type.equals("heapGcStops"))
            return selectHeapGcStops(parameters);
        else if (type.equals("classes"))
            return selectClasses(parameters);
        else if (type.equals("compilationTime"))
            return selectCompilationTime(parameters);
        else if (type.equals("gcs"))
            return selectGcs(parameters);
        else if (type.equals("gcGcTime"))
            return selectGcGcTime(parameters);
        else if (type.equals("gcGcBytes"))
            return selectGcGcBytes(parameters);
        else if (type.equals("gcGcTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "jvm.gc", "jvm.gc.time", 0, parameters);
        else if (type.equals("gcGcBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "jvm.gc", "jvm.gc.bytes", 0, parameters);
        else if (type.equals("gcGcStops"))
            return selectGcGcStops(parameters);
        else if (type.equals("buffers"))
            return selectBuffers(parameters);
        else if (type.equals("bufferUsage"))
            return selectBufferUsage(parameters);
        else if (type.equals("bufferCount"))
            return selectBufferCount(parameters);
        else if (type.equals("threads"))
            return selectThreads(parameters);
        else if (type.equals("threadState"))
            return selectThreadState(parameters);
        else if (type.equals("threadCpu"))
            return selectThreadCpu(parameters);
        else if (type.equals("threadCount"))
            return selectThreadCount(parameters);
        else if (type.equals("threadAllocated"))
            return selectThreadAllocated(parameters);
        else
            return super.doSelect(parameters);
    }

    @Override
    protected JsonObjectBuilder doBuildKpiMetrics(long time, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                                  IComputeContext computeContext) {
        Json json = Json.object();
        json.put("time", time)
                .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.cpu.user.%user"))
                .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.resident.%resident"))
                .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.heap.used.%used"))
                .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.nonHeap.used.%used"))
                .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.used.%used"))
                .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.collectionTime.%collectionTime"))
                .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.rate"))
                .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.rate"))
                .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.threads.std.avg"))
                .put("y10", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.resident.std.avg"))
                .put("y11", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.heap.used.std.avg"))
                .put("y12", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.nonHeap.used.std.avg"))
                .put("y13", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.used.std.avg"))
                .put("y14", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.std.sum"))
                .put("y15", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.std.sum"));

        JsonArray cpuThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.cpu.workload.thresholds");
        JsonArray memoryThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.workload.thresholds");
        JsonArray gcThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.errors.thresholds");
        JsonArray swapThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.swap.errors.thresholds");

        Number totalMemory = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.max.std.avg");
        Number totalHeap = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.heap.max.std.avg");
        Number totalNonHeap = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.nonHeap.max.std.avg");
        Number totalBuffers = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.max.std.avg");

        Json jsonAnnotations = json.putArray("annotations");

        if (cpuThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "cpu-t1")
                    .put("type", "thresholdWarning")
                    .put("y", cpuThresholds.get(0))
                    .end()
                    .addObject()
                    .put("name", "cpu-t2")
                    .put("type", "thresholdError")
                    .put("y", cpuThresholds.get(1))
                    .end();
        }

        if (totalMemory != null) {
            jsonAnnotations.addObject()
                    .put("name", "mem-t1")
                    .put("type", "bound")
                    .put("y", totalMemory.longValue())
                    .end();
        }

        if (totalHeap != null) {
            jsonAnnotations.addObject()
                    .put("name", "heap-t1")
                    .put("type", "bound")
                    .put("y", totalHeap.longValue())
                    .end();

            if (memoryThresholds != null) {
                jsonAnnotations.addObject()
                        .put("name", "heap-t2")
                        .put("type", "thresholdWarning")
                        .put("y", totalHeap.longValue() * ((Number) memoryThresholds.get(0)).doubleValue() / 100)
                        .end()
                        .addObject()
                        .put("name", "heap-t3")
                        .put("type", "thresholdError")
                        .put("y", totalHeap.longValue() * ((Number) memoryThresholds.get(1)).doubleValue() / 100)
                        .end();
            }
        }

        if (totalNonHeap != null) {
            jsonAnnotations.addObject()
                    .put("name", "nonheap-t1")
                    .put("type", "bound")
                    .put("y", totalNonHeap.longValue())
                    .end();
        }

        if (totalBuffers != null) {
            jsonAnnotations.addObject()
                    .put("name", "buffers-t1")
                    .put("type", "bound")
                    .put("y", totalBuffers.longValue())
                    .end();
        }

        if (gcThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "gc-t1")
                    .put("type", "thresholdWarning")
                    .put("y", ((Number) gcThresholds.get(0)).doubleValue())
                    .end()
                    .addObject()
                    .put("name", "gc-t2")
                    .put("type", "thresholdError")
                    .put("y", ((Number) gcThresholds.get(1)).doubleValue())
                    .end();
        }

        if (swapThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "swap-t1")
                    .put("type", "thresholdWarning")
                    .put("y", ((Number) swapThresholds.get(0)).doubleValue())
                    .end()
                    .addObject()
                    .put("name", "swap-t2")
                    .put("type", "thresholdError")
                    .put("y", ((Number) swapThresholds.get(1)).doubleValue())
                    .end();
        }

        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.process.cpu.user", "%user", 1, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "jvm.memory.heap.used", "%used", 3, true, jsonAnnotations);

        return json.toObjectBuilder();
    }

    @Override
    protected String getHierarchicalProperty(String name) {
        if (name.equals("args"))
            return "arg";
        else if (name.equals("environment"))
            return "env";
        else if (name.equals("modules"))
            return "module";
        else if (name.equals("classPath"))
            return "classPath";
        else if (name.equals("libraryPath"))
            return "libraryPath";
        else if (name.equals("bootClassPath"))
            return "bootClassPath";
        else if (name.equals("vmArgs"))
            return "vmArg";
        else if (name.equals("systemProperties"))
            return "systemProperty";
        else
            return null;
    }

    private Object selectProcessState(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                String state = Selectors.getMetric(value, accessorFactory, computeContext, "host.process.state");
                if (state == null)
                    state = "unknown";
                Json json = Json.object();
                json.put("time", time)
                        .put("s1", state);

                return json.toObject();
            }
        });
    }

    private Object selectProcessMemory(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.total.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.resident.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.shared.std.avg"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.total.%total"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.resident.%resident"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.shared.%shared"));

                Number totalMemory = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.max.std.avg");

                Json jsonAnnotations = json.putArray("annotations");
                if (totalMemory != null) {
                    jsonAnnotations.addObject()
                            .put("name", "mem-t1")
                            .put("type", "bound")
                            .put("y", totalMemory.longValue())
                            .end();
                }
                return json.toObject();
            }
        });
    }

    private Object selectProcessThreads(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.threads.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectProcessFileDescriptors(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.fd.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectProcessPageFaults(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.rate"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.std.sum"));

                JsonArray swapThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.swap.errors.thresholds");

                Json jsonAnnotations = json.putArray("annotations");
                if (swapThresholds != null) {
                    jsonAnnotations.addObject()
                            .put("name", "swap-t1")
                            .put("type", "thresholdWarning")
                            .put("y", ((Number) swapThresholds.get(0)).doubleValue())
                            .end()
                            .addObject()
                            .put("name", "swap-t2")
                            .put("type", "thresholdError")
                            .put("y", ((Number) swapThresholds.get(1)).doubleValue())
                            .end();
                }

                return json.toObject();
            }
        });
    }

    private Object selectProcessShortProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = component.getScopeId();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode processNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), getKpiComponentType(parameters));
        if (processNode != null) {
            JsonObject metadata = processNode.getMetadata();
            if (metadata != null) {
                JsonObjectBuilder builder = jsonRows.addObject().toObjectBuilder();
                builder.putAll(metadata);

                JsonArray args = builder.get("args");
                StringBuilder str = new StringBuilder();
                boolean first = true;
                for (Object arg : args) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    str.append(" ");
                    str.append(arg);
                }
                builder.put("command", (String) builder.get("command") + str);
                builder.remove("environment");
                builder.remove("args");
                builder.remove("modules");
            }
        }

        return json.toObject();
    }

    private Object selectHeaps(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonHeapGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectHeapMetric(periodType, currentTime, "Memory pools", component.getScopeId(), jsonHeapGroup);
            Json jsonHeapChildren = jsonHeapGroup.putArray("children");

            INameNode heapsNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "jvm.pool");
            if (heapsNode != null) {
                for (INameNode heapNode : heapsNode.getScopeChildren())
                    selectHeapMetric(periodType, currentTime, heapNode.getScope().getLastSegment().toString(), heapNode.getLocation().getScopeId(),
                            jsonHeapChildren.addObject());
            }
        } else {
            selectHeapMetric(periodType, currentTime, "Group", component.getScopeId(), jsonHeapGroup);
            jsonHeapGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectHeapMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "jvm.pool", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        JsonObject metadata = aggregationNode.getMetadata();
                        if (metadata != null)
                            result.put("type", metadata.get("type"));

                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.memory.pool.init.%init", "jvm.memory.pool.init.std.avg",
                                null, "init", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.memory.pool.used.%used", "jvm.memory.pool.used.std.avg",
                                "jvm.memory.pool.anomaly(%used).level", "used", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.memory.pool.committed.%committed", "jvm.memory.pool.committed.std.avg",
                                null, "committed", result);
                        result.put("max", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.max.std.avg"));
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.gc.time.%time", "jvm.gc.time.std.sum",
                                null, "gcTime", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.rate", "jvm.gc.bytes.std.sum",
                                null, "gcBytes", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "jvm.gc.stops.rate", "jvm.gc.stops.std.sum",
                                null, "gcStops", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectHeapUsage(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.pool", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Number maxPool = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.max.std.avg");

                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.init.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.used.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.committed.std.avg"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.init.%init"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.used.%used"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.pool.committed.%committed"));

                if (maxPool != null) {
                    Json jsonAnnotations = json.putArray("annotations");

                    jsonAnnotations.addObject()
                            .put("name", "heap-t1")
                            .put("type", "bound")
                            .put("y", maxPool.longValue());
                }

                return json.toObject();
            }
        });
    }

    private Object selectHeapGcTime(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.pool", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.%time"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectHeapGcBytes(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.pool", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectHeapGcStops(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.pool", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectClasses(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "jvm.code", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.code.loadedClasses.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.code.unloadedClasses.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.code.currentClasses.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectCompilationTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "jvm.code", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.code.compilationTime.%compilationTime"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.code.compilationTime.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectGcs(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonGcGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectGcMetric(periodType, currentTime, "Garbage collectors", component.getScopeId(), jsonGcGroup);
            Json jsonGcChildren = jsonGcGroup.putArray("children");

            INameNode gcsNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "jvm.gc");
            if (gcsNode != null) {
                for (INameNode gcNode : gcsNode.getScopeChildren())
                    selectGcMetric(periodType, currentTime, gcNode.getScope().getLastSegment().toString(), gcNode.getLocation().getScopeId(),
                            jsonGcChildren.addObject());
            }
        } else {
            selectGcMetric(periodType, currentTime, "Group", component.getScopeId(), jsonGcGroup);
            jsonGcGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectGcMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "jvm.gc", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.gc.time.%time", "jvm.gc.time.std.sum",
                                null, "gcTime", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.rate", "jvm.gc.bytes.std.sum",
                                null, "gcBytes", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "jvm.gc.stops.rate", "jvm.gc.stops.std.sum",
                                null, "gcStops", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectGcGcTime(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.gc", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.%time"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectGcGcBytes(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.gc", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectGcGcStops(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.gc", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.gc.stops.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectBuffers(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonBufferGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectBufferMetric(periodType, currentTime, "Buffer pools", component.getScopeId(), jsonBufferGroup);
            Json jsonBufferChildren = jsonBufferGroup.putArray("children");

            INameNode buffersNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "jvm.buffer");
            if (buffersNode != null) {
                for (INameNode bufferNode : buffersNode.getScopeChildren())
                    selectBufferMetric(periodType, currentTime, bufferNode.getScope().getLastSegment().toString(), bufferNode.getLocation().getScopeId(),
                            jsonBufferChildren.addObject());
            }
        } else {
            selectBufferMetric(periodType, currentTime, "Group", component.getScopeId(), jsonBufferGroup);
            jsonBufferGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectBufferMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "jvm.buffer", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.count.std.avg"));
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.used.%used", "jvm.memory.buffer.used.std.avg",
                                "jvm.memory.buffer.anomaly(%used).level", "used", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.total.%total", "jvm.memory.buffer.total.std.avg",
                                null, "total", result);
                        result.put("max", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.max.std.avg"));

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectBufferUsage(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.buffer", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Number maxBuffer = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.max.std.avg");

                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.total.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.used.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.total.%total"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.used.%used"));

                if (maxBuffer != null) {
                    Json jsonAnnotations = json.putArray("annotations");

                    jsonAnnotations.addObject()
                            .put("name", "buffer-t1")
                            .put("type", "bound")
                            .put("y", maxBuffer.longValue());
                }

                return json.toObject();
            }
        });
    }

    private Object selectBufferCount(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.buffer", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.memory.buffer.count.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectThreads(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonThreadGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectThreadMetric(periodType, currentTime, "Threads", component.getScopeId(), jsonThreadGroup);
            Json jsonThreadChildren = jsonThreadGroup.putArray("children");

            INameNode threadsNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "jvm.thread");
            if (threadsNode != null) {
                for (INameNode threadNode : threadsNode.getScopeChildren())
                    selectThreadMetric(periodType, currentTime, threadNode.getScope().getLastSegment().toString(), threadNode.getLocation().getScopeId(),
                            jsonThreadChildren.addObject());
            }
        } else {
            selectThreadMetric(periodType, currentTime, "Group", component.getScopeId(), jsonThreadGroup);
            jsonThreadGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectThreadMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "jvm.thread", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        JsonObject metadata = aggregationNode.getMetadata();
                        if (metadata != null)
                            result.put("name", metadata.get("name"));

                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.thread.cpu.total.%total(cpu)", null,
                                null, "cpu", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.thread.time.waited.%waited(time)", null,
                                null, "waited", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "jvm.thread.time.blocked.%blocked(time)", null,
                                null, "blocked", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "jvm.thread.allocated.rate",
                                "jvm.thread.allocated.std.sum", null, "allocated", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectThreadState(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, (Long) parameters.get("subScopeId"), 0, "jvm.thread", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                String state = Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.state");
                if (state == null)
                    state = "unknown";
                Json json = Json.object();
                json.put("time", time)
                        .put("s1", state);

                return json.toObject();
            }
        });
    }

    private Object selectThreadCpu(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.thread", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.cpu.user.%user(cpu)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.cpu.sys.%sys(cpu)"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.time.waited.%waited(time)"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.time.blocked.%blocked(time)"));

                return json.toObject();
            }
        });
    }

    private Object selectThreadCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "jvm.threads", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.threads.started.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.threads.daemons.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectThreadAllocated(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "jvm.thread", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.allocated.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "jvm.thread.allocated.std.sum"));

                return json.toObject();
            }
        });
    }
}
