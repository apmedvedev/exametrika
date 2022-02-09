/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.selectors;

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
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.selectors.ComponentSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;


/**
 * The {@link HostSelector} is an host selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostSelector extends ComponentSelector {
    public HostSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "host.kpi");
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");
        if (type.equals("cpus"))
            return selectCpus(parameters);
        else if (type.equals("cpuProperties"))
            return selectCpuProperties(parameters);
        else if (type.equals("cpu"))
            return selectCpu(parameters);
        else if (type.equals("swap"))
            return selectSwap(parameters);
        else if (type.equals("disks"))
            return selectDisks(parameters);
        else if (type.equals("diskProperties"))
            return selectDiskProperties(parameters);
        else if (type.equals("diskUsage"))
            return selectDiskUsage(parameters);
        else if (type.equals("diskRates"))
            return selectDiskRates(parameters);
        else if (type.equals("diskFiles"))
            return selectDiskFiles(parameters);
        else if (type.equals("networks"))
            return selectNetworks(parameters);
        else if (type.equals("netProperties"))
            return selectNetProperties(parameters);
        else if (type.equals("netInterfaceProperties"))
            return selectNetInterfaceProperties(parameters);
        else if (type.equals("netRates"))
            return selectNetRates(parameters);
        else if (type.equals("netErrors"))
            return selectNetErrors(parameters);
        else if (type.equals("netStatistics"))
            return selectNetStatistics(parameters);
        else if (type.equals("processes"))
            return selectProcesses(parameters);
        else if (type.equals("processesStatistics"))
            return selectProcessesStatistics(parameters);
        else if (type.equals("threads"))
            return selectThreads(parameters);
        else if (type.equals("processState"))
            return selectProcessState(parameters);
        else if (type.equals("processCpu"))
            return selectProcessCpu(parameters);
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
        else if (type.equals("processProperties"))
            return selectProcessProperties(parameters);
        else
            return super.doSelect(parameters);
    }

    @Override
    protected JsonObjectBuilder doBuildKpiMetrics(long time, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                                  IComputeContext computeContext) {
        Json json = Json.object();
        json.put("time", time)
                .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.used.%used"))
                .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.memory.used.%used"))
                .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.used.%used"))
                .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.received.rate(bytes)"))
                .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.sent.rate(bytes)"))
                .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.read.rate(bytes)"))
                .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.write.rate(bytes)"))
                .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "host.memory.used.std.avg"))
                .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.used.std.avg"))
                .put("y10", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.received.std.sum"))
                .put("y11", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.sent.std.sum"))
                .put("y12", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.read.std.sum"))
                .put("y13", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.write.std.sum"));

        JsonArray cpuThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.workload.thresholds");
        JsonArray memoryThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "host.memory.workload.thresholds");
        JsonArray swapThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.workload.thresholds");
        Number totalMemory = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.memory.total.std.avg");
        Number totalSwap = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.total.std.avg");
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

            if (memoryThresholds != null) {
                jsonAnnotations.addObject()
                        .put("name", "mem-t2")
                        .put("type", "thresholdWarning")
                        .put("y", totalMemory.longValue() * ((Number) memoryThresholds.get(0)).doubleValue() / 100)
                        .end()
                        .addObject()
                        .put("name", "mem-t3")
                        .put("type", "thresholdError")
                        .put("y", totalMemory.longValue() * ((Number) memoryThresholds.get(1)).doubleValue() / 100)
                        .end();
            }
        }

        if (totalSwap != null) {
            jsonAnnotations.addObject()
                    .put("name", "swap-t1")
                    .put("type", "bound")
                    .put("y", totalSwap.longValue())
                    .end();

            if (swapThresholds != null) {
                jsonAnnotations.addObject()
                        .put("name", "swap-t2")
                        .put("type", "thresholdWarning")
                        .put("y", totalSwap.longValue() * ((Number) swapThresholds.get(0)).doubleValue() / 100)
                        .end()
                        .addObject()
                        .put("name", "swap-t3")
                        .put("type", "thresholdError")
                        .put("y", totalSwap.longValue() * ((Number) swapThresholds.get(1)).doubleValue() / 100)
                        .end();
            }
        }

        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.cpu.used", "%used", 1, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.memory.used", "%used", 2, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.swap.used", "%used", 3, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.net.received", "rate(bytes)", 4, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.net.sent", "rate(bytes)", 5, true, jsonAnnotations);

        return json.toObjectBuilder();
    }

    private Object selectCpus(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonCpuGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectCpuMetric(periodType, currentTime, "CPUs", component.getScopeId(), jsonCpuGroup);
            Json jsonCpuChildren = jsonCpuGroup.putArray("children");

            INameNode cpusNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "host.cpu");
            if (cpusNode != null) {
                int i = 1;
                for (INameNode cpuNode : cpusNode.getScopeChildren())
                    selectCpuMetric(periodType, currentTime, "CPU" + i++, cpuNode.getLocation().getScopeId(),
                            jsonCpuChildren.addObject());
            }
        } else {
            selectCpuMetric(periodType, currentTime, "Group", component.getScopeId(), jsonCpuGroup);
            jsonCpuGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectCpuMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", title)
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "host.cpu", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.user.%user", null,
                                "host.cpu.user.anomaly(%user).level", "user", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.sys.%sys", null,
                                "host.cpu.sys.anomaly(%sys).level", "sys", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.iowait.%iowait", null,
                                null, "io", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.idle.%idle", null,
                                null, "idle", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.irq.%irq", null,
                                null, "irq", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.softIrq.%softIrq", null,
                                null, "softIrq", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.nice.%nice", null,
                                null, "nice", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.stolen.%stolen", null,
                                null, "stolen", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectCpuProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode cpuNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), "host.cpu");
        if (cpuNode != null && cpuNode.isDerived()) {
            for (INameNode node : cpuNode.getScopeChildren()) {
                cpuNode = node;
                break;
            }
        }

        if (cpuNode != null) {
            JsonObject metadata = cpuNode.getMetadata();
            if (metadata != null)
                jsonRows.addObject().toObjectBuilder().putAll(metadata);
        }

        return json.toObject();
    }

    private Object selectCpu(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.cpu", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.user.%user"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.sys.%sys"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.iowait.%iowait"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.idle.%idle"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.irq.%irq"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.softIrq.%softIrq"))
                        .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.nice.%nice"))
                        .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "host.cpu.stolen.%stolen"));

                Json jsonAnnotations = json.putArray("annotations");

                Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.cpu.user", "%user", 1, false, jsonAnnotations);

                return json.toObject();
            }
        });
    }

    private Object selectSwap(Map<String, ?> parameters) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        final JsonArray swapThresholds = (JsonArray) selectionService.buildRepresentation(periodType, currentTime,
                new Location(component.getScopeId(), 0), getKpiComponentType(parameters), 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        return Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.errors.thresholds");
                    }
                });

        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "host.swap", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.pagesIn.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.pagesOut.rate"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.pagesIn.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.swap.pagesOut.std.sum"));

                if (swapThresholds != null) {
                    json.putArray("annotations")
                            .addObject()
                            .put("name", "swap-t1")
                            .put("type", "thresholdWarning")
                            .put("y", swapThresholds.get(0))
                            .end()
                            .addObject()
                            .put("name", "swap-t2")
                            .put("type", "thresholdError")
                            .put("y", swapThresholds.get(1))
                            .end();
                }

                return json.toObject();
            }
        });
    }

    private Object selectDisks(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonDiskGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectDiskMetric(periodType, currentTime, "Disks", component.getScopeId(), jsonDiskGroup);
            Json jsonDiskChildren = jsonDiskGroup.putArray("children");

            INameNode disksNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "host.fs");
            if (disksNode != null) {
                for (INameNode diskNode : disksNode.getScopeChildren())
                    selectDiskMetric(periodType, currentTime, diskNode.getScope().getLastSegment(), diskNode.getLocation().getScopeId(),
                            jsonDiskChildren.addObject());
            }
        } else {
            selectDiskMetric(periodType, currentTime, "Group", component.getScopeId(), jsonDiskGroup);
            jsonDiskGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectDiskMetric(String periodType, long currentTime, String title, long scopeId, final Json result) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "host.fs", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        JsonObject metadata = aggregationNode.getMetadata();

                        if (metadata != null)
                            result.put("dev", metadata.get("dev")).put("type", metadata.get("sysType"));

                        result.put("total", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.total.std.avg"));

                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.disk.used.%used", "host.disk.used.std.avg",
                                null, "used", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.disk.available.%available", "host.disk.available.std.avg",
                                null, "available", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectDiskProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = (Long) parameters.get("subScopeId");

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode diskNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), "host.fs");
        if (diskNode != null) {
            JsonObject metadata = diskNode.getMetadata();
            if (metadata != null)
                jsonRows.addObject().toObjectBuilder().putAll(metadata);
        }

        return json.toObject();
    }

    private Object selectDiskUsage(Map<String, ?> parameters) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        final JsonArray diskThresholds = parameters.containsKey("subScopeId") ? (JsonArray) selectionService.buildRepresentation(
                periodType, currentTime, new Location(component.getScopeId(), 0), getKpiComponentType(parameters), 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        return Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.workload.thresholds");
                    }
                }) : null;

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.fs", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Number totalDisk = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.total.std.avg");

                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.used.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.used.%used"));

                if (totalDisk != null) {
                    Json jsonAnnotations = json.putArray("annotations");

                    jsonAnnotations.addObject()
                            .put("name", "disk-t1")
                            .put("type", "bound")
                            .put("y", totalDisk.longValue());

                    if (diskThresholds != null) {
                        jsonAnnotations
                                .addObject()
                                .put("name", "disk-t2")
                                .put("type", "thresholdWarning")
                                .put("y", totalDisk.longValue() * ((Number) diskThresholds.get(0)).doubleValue() / 100)
                                .end()
                                .addObject()
                                .put("name", "disk-t3")
                                .put("type", "thresholdError")
                                .put("y", totalDisk.longValue() * ((Number) diskThresholds.get(1)).doubleValue() / 100)
                                .end();
                    }
                }

                return json.toObject();
            }
        });
    }

    private Object selectDiskRates(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.fs", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.read.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.write.rate(bytes)"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.read.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.write.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectDiskFiles(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.fs", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Number totalFiles = (Number) Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.files.total.std.avg");

                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.files.used.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.disk.files.used.%used"));

                if (totalFiles != null) {
                    Json jsonAnnotations = json.putArray("annotations");

                    jsonAnnotations.addObject()
                            .put("name", "disk-t1")
                            .put("type", "bound")
                            .put("y", totalFiles.longValue());
                }

                return json.toObject();
            }
        });
    }

    private Object selectNetworks(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonNetGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectNetMetric(periodType, currentTime, "Network Interfaces", component.getScopeId(), jsonNetGroup, false);
            Json jsonNetChildren = jsonNetGroup.putArray("children");

            INameNode netsNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "host.net");
            if (netsNode != null) {
                for (INameNode netNode : netsNode.getScopeChildren())
                    selectNetMetric(periodType, currentTime, netNode.getScope().getLastSegment(), netNode.getLocation().getScopeId(),
                            jsonNetChildren.addObject(), true);
            }
        } else {
            selectNetMetric(periodType, currentTime, "Group", component.getScopeId(), jsonNetGroup, false);
            jsonNetGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectNetMetric(String periodType, long currentTime, String title, long scopeId, final Json result, final boolean leaf) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "host.net", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        if (leaf) {
                            JsonObject metadata = aggregationNode.getMetadata();

                            if (metadata != null)
                                result.put("type", metadata.get("netType")).put("address", metadata.get("address"));
                        }

                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.net.received.rate(bytes)", "host.net.received.std.sum",
                                "host.net.received.anomaly(rate(bytes)).level", "receiveRate", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.net.sent.rate(bytes)", "host.net.sent.std.sum",
                                "host.net.sent.anomaly(rate(bytes)).level", "sendRate", result);
                        result.put("receiveErrors", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.rx.errors.std.sum"));
                        result.put("sendErrors", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.errors.std.sum"));

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectNetProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode netNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(component.getScopeId(), 0), "host.nets");
        if (netNode != null) {
            JsonObject metadata = netNode.getMetadata();
            if (metadata != null)
                jsonRows.addObject().toObjectBuilder().putAll(metadata);
        }

        return json.toObject();
    }

    private Object selectNetInterfaceProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = (Long) parameters.get("subScopeId");

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode netNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), "host.net");
        if (netNode != null) {
            JsonObject metadata = netNode.getMetadata();
            if (metadata != null)
                jsonRows.addObject().toObjectBuilder().putAll(metadata);
        }

        return json.toObject();
    }

    private Object selectNetRates(Map<String, ?> parameters) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        final JsonArray netThresholds = parameters.containsKey("subScopeId") ? (JsonArray) selectionService.buildRepresentation(
                periodType, currentTime, new Location(component.getScopeId(), 0), getKpiComponentType(parameters), 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        return Selectors.getMetric(value, accessorFactory, computeContext, "host.net.receive.workload.thresholds");
                    }
                }) : null;

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.net", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Number totalRate = null;
                JsonObject metadata = aggregationNode.getMetadata();
                if (metadata != null)
                    totalRate = metadata.get("speed", null);

                Number receiveRate = Selectors.getMetric(value, accessorFactory, computeContext, "host.net.received.rate(bytes)");
                Number sendRate = Selectors.getMetric(value, accessorFactory, computeContext, "host.net.sent.rate(bytes)");
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", receiveRate)
                        .put("y2", sendRate)
                        .put("y3", (totalRate != null && receiveRate != null) ? receiveRate.doubleValue() / totalRate.doubleValue() * 100 : null)
                        .put("y4", (totalRate != null && sendRate != null) ? sendRate.doubleValue() / totalRate.doubleValue() * 100 : null)
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.received.std.sum"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.sent.std.sum"));

                Json jsonAnnotations = json.putArray("annotations");

                if (totalRate != null) {
                    jsonAnnotations.addObject()
                            .put("name", "net-t1")
                            .put("type", "bound")
                            .put("y", totalRate.doubleValue());

                    if (netThresholds != null) {
                        jsonAnnotations
                                .addObject()
                                .put("name", "net-t2")
                                .put("type", "thresholdWarning")
                                .put("y", totalRate.doubleValue() * ((Number) netThresholds.get(0)).doubleValue() / 100)
                                .end()
                                .addObject()
                                .put("name", "net-t3")
                                .put("type", "thresholdError")
                                .put("y", totalRate.doubleValue() * ((Number) netThresholds.get(1)).doubleValue() / 100)
                                .end();
                    }
                }

                Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.net.received", "rate(bytes)", 0,
                        false, "receive", 52, 100, jsonAnnotations);
                Selectors.checkAnomaly(value, accessorFactory, computeContext, "host.net.sent", "rate(bytes)", 1,
                        false, "send", 0, 47, jsonAnnotations);

                return json.toObject();
            }
        });
    }

    private Object selectNetErrors(Map<String, ?> parameters) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();

        final JsonArray netThresholds = parameters.containsKey("subScopeId") ? (JsonArray) selectionService.buildRepresentation(
                periodType, currentTime, new Location(component.getScopeId(), 0), getKpiComponentType(parameters), 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        return Selectors.getMetric(value, accessorFactory, computeContext, "host.net.errors.thresholds");
                    }
                }) : null;

        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.net", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.rx.errors.std.sum"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.errors.std.sum"));

                Json jsonAnnotations = json.putArray("annotations");

                if (netThresholds != null) {
                    jsonAnnotations
                            .addObject()
                            .put("name", "net-t1")
                            .put("type", "thresholdWarning")
                            .put("y", ((Number) netThresholds.get(0)).doubleValue())
                            .end()
                            .addObject()
                            .put("name", "net-t2")
                            .put("type", "thresholdError")
                            .put("y", ((Number) netThresholds.get(1)).doubleValue())
                            .end();
                }

                return json.toObject();
            }
        });
    }

    private Object selectNetStatistics(Map<String, ?> parameters) {
        long scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : component.getScopeId();
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.net", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.rx.dropped.std.sum"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.dropped.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.rx.overruns.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.overruns.std.sum"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.rx.frame.std.sum"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.collisions.std.sum"))
                        .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "host.net.tx.carrier.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectProcesses(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonProcessGroup = json.putArray("rows").addObject();

        if (!(component instanceof IGroupComponent)) {
            selectProcessMetric(periodType, currentTime, "Processes", component.getScopeId(), jsonProcessGroup, false);
            Json jsonProcessChildren = jsonProcessGroup.putArray("children");

            INameNode processesNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), "host.process");
            if (processesNode != null) {
                for (INameNode processNode : processesNode.getScopeChildren())
                    selectProcessMetric(periodType, currentTime, processNode.getScope().getLastSegment(), processNode.getLocation().getScopeId(),
                            jsonProcessChildren.addObject(), true);
            }
        } else {
            selectProcessMetric(periodType, currentTime, "Group", component.getScopeId(), jsonProcessGroup, false);
            jsonProcessGroup.putArray("children");
        }

        return json.toObject();
    }

    private void selectProcessMetric(String periodType, long currentTime, String title, long scopeId, final Json result, final boolean leaf) {
        result.put("id", scopeId)
                .put("elementId", scopeId)
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, 0), "host.process", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        if (leaf) {
                            JsonObject metadata = aggregationNode.getMetadata();

                            if (metadata != null) {
                                JsonArray args = metadata.get("args");
                                StringBuilder argsStr = new StringBuilder();
                                boolean first = true;
                                for (Object arg : args) {
                                    if (first) {
                                        first = false;
                                        continue;
                                    } else
                                        argsStr.append(" ");

                                    argsStr.append(arg);
                                }

                                result.put("pid", metadata.get("id"))
                                        .put("parentId", metadata.get("parentId"))
                                        .put("startTime", metadata.get("startTime"))
                                        .put("command", metadata.get("command"))
                                        .put("args", argsStr)
                                        .put("user", metadata.get("user"))
                                        .put("priority", metadata.get("priority"));
                            }
                        }

                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.process.cpu.user.%user", null,
                                null, "cpu", result);
                        Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.process.memory.resident.%resident",
                                "host.process.memory.resident.std.avg", null, "memory", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.rate",
                                "host.process.memory.minorFaults.std.sum", null, "minorFaults", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.rate",
                                "host.process.memory.majorFaults.std.sum", null, "majorFaults", result);

                        result.put("threads", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.threads.std.avg"));
                        result.put("fileDescriptors", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.fd.std.avg"));

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectProcessesStatistics(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "host.processes", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.total.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.idle.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.running.std.avg"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.sleeping.std.avg"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.stopped.std.avg"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.zombie.std.avg"))
                        .put("y7", 100)
                        .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.idle.%idle"))
                        .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.running.%running"))
                        .put("y10", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.sleeping.%sleeping"))
                        .put("y11", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.stopped.%stopped"))
                        .put("y12", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.zombie.%zombie"));

                return json.toObject();
            }
        });
    }

    private Object selectThreads(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "host.processes", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.processes.threads.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectProcessState(Map<String, ?> parameters) {
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
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

    private Object selectProcessCpu(Map<String, ?> parameters) {
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.cpu.user.%user"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.cpu.sys.%sys"));

                return json.toObject();
            }
        });
    }

    private Object selectProcessMemory(Map<String, ?> parameters) {
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
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

                return json.toObject();
            }
        });
    }

    private Object selectProcessThreads(Map<String, ?> parameters) {
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
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
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
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
        long scopeId = (Long) parameters.get("subScopeId");
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "host.process", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.rate"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectProcessShortProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = (Long) parameters.get("subScopeId");

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode processNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), "host.process");
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

    private Object selectProcessProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        long scopeId = (Long) parameters.get("subScopeId");

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        INameNode processNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, 0), "host.process");
        if (processNode != null) {
            JsonObject metadata = processNode.getMetadata();
            if (metadata != null) {
                for (Map.Entry<String, Object> entry : metadata) {
                    String name = entry.getKey();
                    if (name.startsWith("node"))
                        continue;
                    if (name.equals("args") || name.equals("environment") || name.equals("modules")) {
                        JsonArray children = buildChildrenProperties(name.substring(0, name.length() - 1), (IJsonCollection) entry.getValue());
                        jsonRows.addObject()
                                .put("id", name)
                                .put("elementId", name)
                                .put("name", name)
                                .put("children", children);
                    } else
                        jsonRows.addObject()
                                .put("id", name)
                                .put("elementId", name)
                                .put("name", name)
                                .put("value", entry.getValue());
                }
            }
        }

        return json.toObject();
    }
}
