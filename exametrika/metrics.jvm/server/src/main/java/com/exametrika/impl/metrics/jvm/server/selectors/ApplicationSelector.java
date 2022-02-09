/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.nodes.Dependency;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IExitPointNode;
import com.exametrika.api.aggregator.nodes.IIntermediateExitPointNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.IPrimaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNameNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.ITransactionComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.nodes.NodeComponentVersionNode;
import com.exametrika.impl.component.selectors.ComponentSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link ApplicationSelector} is a base application selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ApplicationSelector extends ComponentSelector {
    private static int COUNTER;
    private final IComponentService componentService;
    private final IPeriodNameManager nameManager;

    public ApplicationSelector(IComponent component, ISelectorSchema schema, String kpiComponentType) {
        super(component, schema, kpiComponentType);

        componentService = this.schema.getContext().getTransactionProvider().getTransaction().findDomainService(IComponentService.NAME);
        nameManager = this.schema.getContext().getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
    }

    protected abstract boolean isTransaction();

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");
        Long scopeId = (Long) parameters.get("secondaryScopeId");
        if (scopeId == null)
            scopeId = component.getScopeId();
        String hotspotSuffix = "";
        if (parameters.containsKey("hotspot"))
            hotspotSuffix = ".name";

        boolean transaction = isTransaction();

        if (type.equals("callStack"))
            return selectCallStack(parameters);
        else if (type.equals("logCount"))
            return selectLogCount(scopeId, parameters);
        else if (type.equals("log"))
            return Selectors.selectLog(scopeId, selectionService, "app.log.log", "log", 0, parameters);
        else if (type.equals("fileReadTime"))
            return selectFileReadTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("fileReadTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.file" + hotspotSuffix, "app.file.read.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("fileReadBytes"))
            return selectFileReadBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("fileReadBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.file" + hotspotSuffix, "app.file.read.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("fileWriteTime"))
            return selectFileWriteTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("fileWriteTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.file" + hotspotSuffix, "app.file.write.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("fileWriteBytes"))
            return selectFileWriteBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("fileWriteBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.file" + hotspotSuffix, "app.file.write.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("tcpConnectTime"))
            return selectTcpConnectTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("tcpConnectTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.tcp" + hotspotSuffix, "app.tcp.connect.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("tcpReceiveTime"))
            return selectTcpReceiveTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("tcpReceiveTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.tcp" + hotspotSuffix, "app.tcp.receive.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("tcpReceiveBytes"))
            return selectTcpReceiveBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("tcpReceiveBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.tcp" + hotspotSuffix, "app.tcp.receive.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("tcpSendTime"))
            return selectTcpSendTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("tcpSendTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.tcp" + hotspotSuffix, "app.tcp.send.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("tcpSendBytes"))
            return selectTcpSendBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("tcpSendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.tcp" + hotspotSuffix, "app.tcp.send.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("udpReceiveTime"))
            return selectUdpReceiveTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("udpReceiveTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.udp" + hotspotSuffix, "app.udp.receive.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("udpReceiveBytes"))
            return selectUdpReceiveBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("udpReceiveBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.udp" + hotspotSuffix, "app.udp.receive.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("udpSendTime"))
            return selectUdpSendTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("udpSendTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.udp" + hotspotSuffix, "app.udp.send.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("udpSendBytes"))
            return selectUdpSendBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("udpSendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.udp" + hotspotSuffix, "app.udp.send.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("jdbcConnectionTime"))
            return selectJdbcConnectionTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("jdbcConnectionTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.jdbcConnection" + hotspotSuffix, "app.db.connect.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("jdbcQueryTime"))
            return selectJdbcQueryTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("jdbcQueryTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.jdbc" + hotspotSuffix, "app.db.query.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("jdbcQueryTimeInstances"))
            return selectJdbcQueryTimeInstances(scopeId, hotspotSuffix, parameters);
        else if (type.equals("instances"))
            return selectTransactionInstances(scopeId, parameters);
        else if (type.equals("entryRequestTime"))
            return selectEntryRequestTime(scopeId, parameters);
        else if (type.equals("entryRequestTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "secondary.app.entryPoint", "app.request.time", 0, parameters);
        else if (type.equals("entryTransactionTime"))
            return selectEntryTransactionTime(scopeId, parameters);
        else if (type.equals("entryTransactionTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "secondary.app.entryPoint", "app.transaction.time", 0, parameters);
        else if (type.equals("entryErrorsCount"))
            return selectEntryErrorsCount(scopeId, parameters);
        else if (type.equals("entryStallsCount"))
            return selectEntryStallsCount(scopeId, parameters);
        else if (type.equals("entryReceiveBytes"))
            return selectEntryReceiveBytes(scopeId, parameters);
        else if (type.equals("entryReceiveBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "secondary.app.entryPoint", "app.receive.bytes", 0, parameters);
        else if (type.equals("entrySendBytes"))
            return selectEntrySendBytes(scopeId, parameters);
        else if (type.equals("entrySendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "secondary.app.entryPoint", "app.send.bytes", 0, parameters);
        else if (type.equals("httpRequestTime"))
            return selectHttpRequestTime(scopeId, hotspotSuffix, parameters);
        else if (type.equals("httpRequestTimeHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.httpConnection" + hotspotSuffix, "app.http.time", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("httpReceiveBytes"))
            return selectHttpReceiveBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("httpReceiveBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.httpConnection" + hotspotSuffix, "app.http.receive.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("httpSendBytes"))
            return selectHttpSendBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("httpSendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.httpConnection" + hotspotSuffix, "app.http.send.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("jmsSendBytes"))
            return selectJmsSendBytes(scopeId, hotspotSuffix, parameters);
        else if (type.equals("jmsSendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "app.jmsProducer" + hotspotSuffix, "app.jms.bytes", (Long) parameters.get("metricId"), parameters);
        else if (type.equals("servletErrors"))
            return selectServletErrors(scopeId, parameters);
        else if (type.equals("stalls"))
            return selectStalls(scopeId, parameters);
        else if (type.equals("exceptionsCount"))
            return selectExceptionsCount(scopeId, parameters);
        else if (type.equals("exceptions"))
            return selectExceptions(scopeId, parameters);
        else if (type.equals("logErrorsCount"))
            return selectLogErrorsCount(scopeId, parameters);
        else if (type.equals("logErrors"))
            return Selectors.selectLog(scopeId, selectionService, "app.log.errors", "app.log.errors", 0, parameters);
        else if (type.equals("connectionErrors"))
            return selectConnectionErrors(scopeId, parameters);
        else if (type.equals("anomalies"))
            return selectAnomalies(parameters);
        else if (type.equals("failures"))
            return selectFailures(parameters);
        else if (type.equals("hotspotMethods"))
            return selectHotspotMethods(false, transaction, parameters);
        else if (type.equals("hotspotErrors"))
            return selectHotspotErrors(false, transaction, parameters);
        else if (type.equals("hotspotFailures"))
            return selectHotspotFailures(false, transaction, parameters);
        else if (type.equals("hotspotJdbcQueries"))
            return selectHotspotJdbcQueries(false, transaction, parameters);
        else if (type.equals("hotspotJdbcConnections"))
            return selectHotspotJdbcConnections(false, transaction, parameters);
        else if (type.equals("hotspotHttpConnections"))
            return selectHotspotHttpConnections(false, transaction, parameters);
        else if (type.equals("hotspotJmsProducers"))
            return selectHotspotJmsProducers(false, transaction, parameters);
        else if (type.equals("hotspotTcps"))
            return selectHotspotTcps(false, transaction, parameters);
        else if (type.equals("hotspotUdps"))
            return selectHotspotUdps(false, transaction, parameters);
        else if (type.equals("hotspotFiles"))
            return selectHotspotFiles(false, transaction, parameters);
        else if (type.equals("hotspotMethodInherentTimes"))
            return selectHotspotMethodInherentTimes(scopeId, parameters);
        else if (type.equals("hotspotMethodTotalTimes"))
            return selectHotspotMethodTotalTimes(scopeId, parameters);
        else if (type.equals("hotspotMethodAllocatedBytes"))
            return selectHotspotMethodAllocatedBytes(scopeId, parameters);
        else if (type.equals("hotspotMethodErrors"))
            return selectHotspotMethodErrors(scopeId, parameters);
        else if (type.equals("hotspotMethodConcurrencyLevel"))
            return selectHotspotMethodConcurrencyLevel(scopeId, parameters);
        else if (type.equals("backtraceMethods"))
            return selectBackTraceMethods(parameters);

        else if (type.equals("backtraceJdbcQueries"))
            return selectBackTraceJdbcQueries(parameters);
        else if (type.equals("backtraceFiles"))
            return selectBackTraceFiles(parameters);
        else if (type.equals("backtraceTcps"))
            return selectBackTraceTcps(parameters);
        else if (type.equals("backtraceUdps"))
            return selectBackTraceUdps(parameters);
        else if (type.equals("backtraceHttpConnections"))
            return selectBackTraceHttpConnections(parameters);
        else if (type.equals("backtraceJmsProducers"))
            return selectBackTraceJmsProducers(parameters);
        else if (type.equals("jdbcQueryRate"))
            return selectJdbcQueryRate(scopeId, hotspotSuffix, parameters);
        else if (type.equals("httpRequestRate"))
            return selectHttpRequestRate(scopeId, hotspotSuffix, parameters);
        else if (type.equals("jmsRequestRate"))
            return selectJmsRequestRate(scopeId, hotspotSuffix, parameters);
        else if (type.equals("entryRequestRate"))
            return selectEntryRequestRate(scopeId, hotspotSuffix, parameters);
        else
            return super.doSelect(parameters);
    }

    private Object selectCallStack(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        TraverseInfo traverseInfo = new TraverseInfo();
        IEntryPointNode stackRootNode = (IEntryPointNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(component.getScopeId(), 0), isTransaction() ? "primary.app.entryPoint" : "app.stack.root");
        if (stackRootNode != null) {
            boolean hasMetrics = currentTime - stackRootNode.getPeriod().getEndTime() < 10000;
            if (stackRootNode instanceof IBackgroundRootNode) {
                JsonObject ownerNode = Selectors.selectReference(componentService, stackRootNode.getLocation().getScopeId());
                for (IStackNode child : stackRootNode.getChildren())
                    selectCallStackElement(child, hasMetrics, jsonRows, ownerNode, traverseInfo);
            } else
                selectCallStackElement(stackRootNode, hasMetrics, jsonRows, null, traverseInfo);
        }

        return json.toObject();
    }

    private void selectCallStackElement(IStackNode node, boolean hasMetrics, final Json jsonRows, JsonObject ownerNode,
                                        TraverseInfo traverseInfo) {
        JsonObject metadata = node.getMetadata();
        if (metadata == null || traverseInfo.isTraversed(node.getId()))
            return;

        traverseInfo.beginLevel();
        traverseInfo.traverse(node.getId());

        final Json result = jsonRows.addObject();
        result.put("secondaryScopeId", node.getLocation().getScopeId());
        String nodeType = metadata.get("type");
        String type = "unknown";
        String title = node.getMetric().getLastSegment().toString();
        String tooltip = title;
        if (node instanceof IPrimaryEntryPointNode) {
            type = "primaryEntry";
            title = component.getTitle();
            tooltip = title;

            ownerNode = Selectors.selectReference(componentService, (String) metadata.get("node"));
        } else if (node instanceof ISecondaryEntryPointNode) {
            ownerNode = Selectors.selectReference(componentService, (String) metadata.get("node"));

            if (nodeType.contains("http")) {
                type = "httpEntry";
                title = "";
                String app = metadata.get("app", null);
                if (app != null) {
                    result.put("app", app);
                    title = app + ":";
                }
                String url = metadata.get("url", null);
                if (url != null) {
                    result.put("url", url);
                    title += url;
                } else
                    title += "all";
            } else if (nodeType.contains("jms")) {
                type = "jmsEntry";
                title = buildJmsProducerProperties(metadata, result);
            } else if (nodeType.contains("thread")) {
                type = "threadEntry";
                title = "Thread";
            }

            tooltip = title;
        } else if (node instanceof IExitPointNode) {
            boolean correctTitle = true;
            if (nodeType.contains("http")) {
                type = "httpExit";
                buildHttpConnectionProperties(metadata, result);
            } else if (nodeType.contains("jms")) {
                type = "jmsExit";
                title = buildJmsProducerProperties(metadata, result);
                tooltip = title;
                correctTitle = false;
            } else if (nodeType.contains("thread")) {
                type = "threadExit";
                title = "Thread request";
                tooltip = title;
                correctTitle = false;
            } else if (nodeType.contains("method"))
                type = "methodExit";
            else if (nodeType.contains("file"))
                type = "fileExit";
            else if (nodeType.contains("tcp"))
                type = "tcpExit";
            else if (nodeType.contains("udp"))
                type = "udpExit";
            else if (nodeType.contains("jdbcConnection"))
                type = "jdbcConnectionExit";
            else if (nodeType.contains("jdbc")) {
                type = "jdbcExit";
                buildJdbcQueryProperties(metadata, result);
            }

            if (correctTitle && node instanceof IIntermediateExitPointNode && node.isDerived()) {
                int pos = title.lastIndexOf(".", title.lastIndexOf(".") - 1);
                if (pos != -1)
                    title = title.substring(0, pos);
                pos = tooltip.lastIndexOf(".", tooltip.lastIndexOf(".") - 1);
                if (pos != -1)
                    tooltip = tooltip.substring(0, pos);
            }

            if (nodeType.contains("async")) {
                title += " (async)";
                tooltip += " (async)";
            }
        } else {
            type = "method";
            IMetricName metric = node.getMetric().getLastSegment();
            if (metric.getSegments().size() > 2)
                title = metric.getSegments().get(metric.getSegments().size() - 2) + "." + metric.getSegments().get(metric.getSegments().size() - 1);
            buildMethodProperties(metadata, result);
        }

        title = Selectors.removePrefix(title);

        Location location = node.getLocation();
        String id = location.getScopeId() + "-" + location.getMetricId();
        result.put("id", id)
                .put("elementId", location.getMetricId())
                .put("metricId", location.getMetricId())
                .putObject("title")
                .put("title", Names.unescape(title))
                .put("tooltip", Names.unescape(tooltip))
                .put("link", "#")
                .end()
                .put("platform", "jvm")
                .put("type", type);

        if (ownerNode != null)
            result.put("node", ownerNode);

        if (hasMetrics) {
            if (Selectors.buildRepresentation(node, 0, new IRepresentationBuilder() {
                @Override
                public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                    IComputeContext computeContext) {
                    buildStackMetrics(value, accessorFactory, computeContext, false, result);
                    return JsonUtils.EMPTY_OBJECT;
                }
            }) == null)
                result.put("noData", true);
        } else
            result.put("noData", true);

        Json children = null;
        for (IStackNode child : node.getChildren()) {
            if (children == null)
                children = result.putArray("children");

            selectCallStackElement(child, hasMetrics, children, ownerNode, traverseInfo);
        }

        if (node instanceof IIntermediateExitPointNode && ((IIntermediateExitPointNode) node).getChildEntryPoint() != null) {
            if (children == null)
                children = result.putArray("children");

            selectCallStackElement(((IIntermediateExitPointNode) node).getChildEntryPoint(), hasMetrics, children, ownerNode, traverseInfo);
        }

        traverseInfo.endLevel();
    }

    private Object selectExceptions(Long scopeId, Map<String, ?> parameters) {
        return Selectors.selectLog(scopeId, selectionService, "app.exceptions.log", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "app.exceptions.log");
                if (logEvent != null) {
                    Object exception = logEvent.get("exception", null);
                    String type = null;
                    if (exception != null) {
                        type = ((JsonObject) exception).get("class", null);
                        exception = exception.toString();
                    }

                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", logEvent.get("time"))
                            .put("id", id)
                            .put("elementId", id)
                            .put("level", logEvent.get("level", null))
                            .put("type", type)
                            .put("thread", logEvent.get("thread", null))
                            .put("message", logEvent.get("message", null))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("location", logEvent.get("errorLocation", null))
                            .put("exception", exception);

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    private void buildHttpConnectionProperties(JsonObject metadata, final Json result) {
        String url = metadata.get("url", null);
        if (url != null)
            result.put("url", url);
    }

    private String buildJmsProducerProperties(JsonObject metadata, final Json result) {
        String title = "";
        String destinationType = metadata.get("destinationType", null);
        if (destinationType != null) {
            result.put("destinationType", destinationType);
            title = destinationType + ":";
        }
        String destination = metadata.get("destination", null);
        if (destination != null) {
            result.put("destination", destination);
            title += destination;
        }

        if (title.isEmpty())
            title = "unknown";

        return title;
    }

    private void buildJdbcQueryProperties(JsonObject metadata, final Json result) {
        String database = metadata.get("db", null);
        if (database != null)
            result.put("database", database);
        String query = metadata.get("query", null);
        if (query != null)
            result.put("query", query);
    }

    private void buildMethodProperties(JsonObject metadata, final Json result) {
        String clazz = metadata.get("class", null);
        if (clazz != null)
            result.put("class", clazz);
        String method = metadata.get("method", null);
        if (method != null)
            result.put("method", method);
        String file = metadata.get("file", null);
        if (file != null)
            result.put("file", file);
        Number line = metadata.get("line", null);
        if (line != null)
            result.put("line", line.intValue());
    }

    private Object selectLogCount(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "app.log", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.log.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFileReadTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.file" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectFileReadBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.file" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFileWriteTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.file" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectFileWriteBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.file" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectTcpConnectTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.tcp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectTcpReceiveTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.tcp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectTcpReceiveBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.tcp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectTcpSendTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.tcp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectTcpSendBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.tcp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectUdpReceiveTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.udp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectUdpReceiveBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.udp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectUdpSendTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.udp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectUdpSendBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.udp" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectJdbcConnectionTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.jdbcConnection" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.connect.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.connect.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.connect.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectJdbcQueryTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.jdbc" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectJdbcQueryRate(Long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.jdbc" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectHttpRequestRate(Long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.httpConnection" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectJmsRequestRate(Long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.jmsProducer" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.rate(ops)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectEntryRequestRate(Long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "secondary.app.entryPoint" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectJdbcQueryTimeInstances(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        return Selectors.selectInstances(scopeId, selectionService, componentService, nameManager, "app.jdbc" + hotspotSuffix,
                "app.db.query.time", (Long) parameters.get("metricId"), parameters, new Selectors.IInstanceContextBuilder() {
                    @Override
                    public void build(JsonObject instance, Json result) {
                        result.put("database", instance.get("db"))
                                .put("query", instance.get("query"));
                    }
                });
    }

    private Object selectTransactionInstances(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectInstances(scopeId, selectionService, componentService, nameManager, "primary.app.entryPoint",
                "app.request.time", 0, parameters, new Selectors.IInstanceContextBuilder() {
                    @Override
                    public void build(JsonObject instance, Json result) {
                        String app = instance.get("app", null);
                        String url = instance.get("url", null);
                        String servlet = instance.get("servlet", null);
                        String name = "";
                        if (app != null)
                            name += app + ":";
                        if (url != null)
                            name += url;
                        else
                            name += "all";

                        result.put("app", app)
                                .put("url", url)
                                .put("servlet", servlet)
                                .put("name", name);
                    }
                });
    }

    private Object selectEntryRequestTime(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectEntryTransactionTime(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.transaction.time.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectEntryErrorsCount(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.%errors"));

                return json.toObject();
            }
        });
    }

    private Object selectEntryStallsCount(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.std.sum"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.%stalls"));

                return json.toObject();
            }
        });
    }

    private Object selectEntryReceiveBytes(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.receive.bytes.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectEntrySendBytes(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "secondary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.send.bytes.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectHttpRequestTime(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        final String percentageField = hotspotSuffix.length() != 0 ? "%period" : "%time";
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.httpConnection" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time." + percentageField))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectHttpReceiveBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.httpConnection" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.receive.bytes.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectHttpSendBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.httpConnection" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.send.bytes.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectJmsSendBytes(long scopeId, String hotspotSuffix, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.jmsProducer" + hotspotSuffix, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.rate(bytes)"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectExceptionsCount(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "app.exceptions", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.exceptions.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectLogErrorsCount(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, scopeId, 0, "app.log", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.log.errorCount.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectServletErrors(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectLog(scopeId, selectionService, "app.entryPoint.errors", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors");
                if (logEvent != null) {
                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", logEvent.get("time"))
                            .put("id", id)
                            .put("elementId", id)
                            .put("thread", logEvent.get("thread", null))
                            .put("message", logEvent.get("message", null))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("status", logEvent.get("status", null));

                    JsonObject request = logEvent.get("request", null);
                    if (request != null) {
                        json.put("url", request.get("url", null))
                                .put("servlet", request.get("servlet", null))
                                .put("app", request.get("app", null));
                    }

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    private Object selectStalls(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectLog(scopeId, selectionService, "app.entryPoint.stalls", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls");
                if (logEvent != null) {
                    Object stackTrace = logEvent.get("stackTrace", null);
                    if (stackTrace != null)
                        stackTrace = stackTrace.toString();

                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", logEvent.get("time"))
                            .put("id", id)
                            .put("elementId", id)
                            .put("thread", logEvent.get("thread", null))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("stackTrace", stackTrace)
                            .put("location", logEvent.get("errorLocation", null))
                            .put("duration", logEvent.get("duration", null));

                    JsonObject request = logEvent.get("request", null);
                    if (request != null) {
                        if (request.contains("app")) {
                            String url = request.get("url", null);
                            json.put("url", url)
                                    .put("servlet", request.get("servlet", null))
                                    .put("app", request.get("app", null))
                                    .put("request", url);
                        } else if (request.contains("destination")) {
                            String destination = request.get("destination", null);
                            String destinationType = request.get("destinationType", null);
                            json.put("destination", destination)
                                    .put("destinationType", destinationType)
                                    .put("request", destinationType + ":" + destination);
                        }
                    }

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    private Object selectConnectionErrors(long scopeId, Map<String, ?> parameters) {
        return Selectors.selectLog(scopeId, selectionService, "app.httpConnection.errors", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "app.httpConnection.errors");
                if (logEvent != null) {
                    Object stackTrace = logEvent.get("stackTrace", null);
                    if (stackTrace != null)
                        stackTrace = stackTrace.toString();

                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", logEvent.get("time"))
                            .put("id", id)
                            .put("elementId", id)
                            .put("thread", logEvent.get("thread", null))
                            .put("message", logEvent.get("message", null))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("location", logEvent.get("errorLocation", null))
                            .put("stackTrace", stackTrace)
                            .put("status", logEvent.get("status", null));

                    JsonObject request = logEvent.get("request", null);
                    if (request != null) {
                        String url = request.get("url", null);
                        json.put("url", url);
                    }

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    private Object selectAnomalies(Map<String, ?> parameters) {
        return Selectors.selectLog(component.getScopeId(), selectionService, "app.anomalies", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "log");
                if (logEvent != null) {
                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", time)
                            .put("id", id)
                            .put("elementId", id)
                            .put("transaction", Selectors.selectReference(componentService, (Long) logEvent.get("scopeId")))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("cause", logEvent.get("causes", null));

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    private Object selectFailures(Map<String, ?> parameters) {
        return Selectors.selectLog(component.getScopeId(), selectionService, "app.failures", 0, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, "log");
                if (logEvent != null) {
                    JsonObject failure = logEvent.get("failure");
                    String id = time + ":" + COUNTER++;
                    JsonObject transactionReference = Selectors.selectReference(componentService, (Long) logEvent.get("scopeId"));
                    Object transactionId = failure.get("transactionId", null);
                    Json json = Json.object();
                    json.put("time", time)
                            .put("id", id)
                            .put("elementId", id)
                            .put("type", "failure")
                            .put("typeTitle", "Failure")
                            .put("transaction", transactionReference)
                            .put("transactionId", transactionId)
                            .put("message", failure.get("message", null))
                            .put("thread", failure.get("thread", null))
                    ;
                    Json children = json.putArray("children");
                    int i = 0;
                    boolean first = true;
                    for (Object object : (JsonArray) logEvent.get("dependencies")) {
                        JsonObject dependency = (JsonObject) object;
                        String childId = id + "-" + i;
                        String type = dependency.get("type");
                        Json child = children.addObject();
                        String scope = dependency.get("scope");
                        child.put("time", time)
                                .put("id", childId)
                                .put("elementId", childId)
                                .put("message", dependency.get("message", null))
                                .put("thread", dependency.get("thread", null))
                                .put("transaction", transactionReference)
                                .put("transactionId", transactionId);

                        JsonObject entryRequest = dependency.get("entryRequest", null);
                        if (entryRequest != null) {
                            String entryType = entryRequest.get("type", null);
                            if (entryType.contains("http")) {
                                String app = entryRequest.get("app", null);
                                String url = entryRequest.get("url", null);
                                if (app != null) {
                                    scope = app + ":";
                                    if (url != null)
                                        scope += url;
                                    else
                                        scope += "all";
                                }
                            } else if (entryType.contains("jms")) {
                                String destinationType = entryRequest.get("destinationType", null);
                                String destination = entryRequest.get("destination", null);
                                if (destination != null && destinationType != null)
                                    scope = destinationType + ":" + destination;
                            } else if (entryType.contains("thread"))
                                scope = "Thread";

                            String ownerNodeScope = entryRequest.get("node", null);
                            if (ownerNodeScope != null) {
                                JsonObject nodeReference = Selectors.selectReference(componentService, ownerNodeScope);
                                child.put("node", nodeReference);
                                if (first)
                                    json.put("node", nodeReference);
                            }
                        }

                        child.put("scope", scope);

                        String typeTitle = type;
                        if (type.equals("log") || type.equals("exception")) {
                            Object exception = dependency.get("exception", null);
                            if (exception != null)
                                exception = exception.toString();

                            child.put("level", dependency.get("level", null))
                                    .put("logger", dependency.get("logger", null))
                                    .put("exception", exception);

                            if (type.equals("log"))
                                typeTitle = "Log";
                            else
                                typeTitle = "Exception";
                        } else if (type.equals("stall")) {
                            String componentType = dependency.get("entryType");
                            if (componentType.contains("http")) {
                                type = "httpStall";
                                JsonObject request = dependency.get("request", null);
                                if (request != null) {
                                    child.put("url", request.get("url", null))
                                            .put("servlet", request.get("servlet", null))
                                            .put("app", request.get("app", null));
                                }
                            } else if (componentType.contains("jms")) {
                                type = "jmsStall";
                                JsonObject request = dependency.get("request", null);
                                if (request != null) {
                                    child.put("destination", request.get("destination", null))
                                            .put("destinationType", request.get("destinationType", null));
                                }
                            } else if (componentType.contains("thread")) {
                                type = "threadStall";
                            } else if (componentType.contains("method")) {
                                type = "methodStall";
                            }

                            typeTitle = "Stall";
                        } else if (type.equals("httpServletError")) {
                            child.put("status", dependency.get("status", null));

                            JsonObject request = dependency.get("request", null);
                            if (request != null) {
                                child.put("url", request.get("url", null))
                                        .put("servlet", request.get("servlet", null))
                                        .put("app", request.get("app", null));
                            }

                            typeTitle = "HTTP servlet error";
                        } else if (type.equals("httpConnectionError")) {
                            child.put("status", dependency.get("status", null));

                            JsonObject request = dependency.get("request", null);
                            if (request != null) {
                                String url = request.get("url", null);
                                child.put("url", url);
                            }
                            typeTitle = "HTTP connection error";
                        }
                        child.put("type", type);
                        child.put("typeTitle", typeTitle);
                        i++;
                        first = false;
                    }

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    protected Object selectHotspotMethods(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.stack.name", "method", "$stack", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                buildStackMetrics(value, accessorFactory, computeContext, true, result);

                JsonObject metadata = aggregationNode.getMetadata();
                if (metadata != null)
                    buildMethodProperties(metadata, result);

                return result.toObject();
            }
        });
    }

    protected Object selectHotspotErrors(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.errors.name", "error", "$errors", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.errors.count.std.sum"));
                return result.toObject();
            }
        });
    }

    protected Object selectHotspotFailures(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.failures.name", "failure", "$failures", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.failures.count.std.sum"));
                return result.toObject();
            }
        });
    }

    protected Object selectHotspotJdbcQueries(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.jdbc.name", "jdbcExit", "jdbc", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.db.query.time.%period",
                        "app.db.query.time.std.sum", null, "queryTime", result);

                JsonObject metadata = aggregationNode.getMetadata();
                if (metadata != null)
                    buildJdbcQueryProperties(metadata, result);

                return result.toObject();
            }
        });
    }

    protected Object selectHotspotJdbcConnections(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.jdbcConnection.name", "jdbcConnectionExit", "jdbcConnections", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.db.connect.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.db.connect.time.%period",
                        "app.db.connect.time.std.sum", null, "connectTime", result);
                return result.toObject();
            }
        });
    }

    protected Object selectHotspotHttpConnections(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.httpConnection.name", "httpExit", "httpConnections", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.http.time.%period",
                        "app.http.time.std.sum", null, "requestTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.http.receive.bytes.rate(bytes)",
                        "app.http.receive.bytes.std.sum", null, "receiveBytes", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.http.send.bytes.rate(bytes)",
                        "app.http.send.bytes.std.sum", null, "sendBytes", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.httpConnection.errors.count.rate",
                        "app.httpConnection.errors.count.std.sum", null, "errorCount", result);

                JsonObject metadata = aggregationNode.getMetadata();
                if (metadata != null)
                    buildHttpConnectionProperties(metadata, result);

                return result.toObject();
            }
        });
    }

    protected Object selectHotspotJmsProducers(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.jmsProducer.name", "jmsExit", "jmsProducers", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.std.count"));
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.jms.bytes.rate(bytes)",
                        "app.jms.bytes.std.sum", null, "sendBytes", result);

                JsonObject metadata = aggregationNode.getMetadata();
                if (metadata != null)
                    buildJmsProducerProperties(metadata, result);

                return result.toObject();
            }
        });
    }

    protected Object selectHotspotTcps(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.tcp.name", "tcpExit", "tcp", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("connectCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.%period",
                        "app.tcp.connect.time.std.sum", null, "connectTime", result);
                result.put("receiveCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.%period",
                        "app.tcp.receive.time.std.sum", null, "receiveTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.tcp.receive.bytes.rate",
                        "app.tcp.receive.bytes.std.sum", null, "receiveBytes", result);
                result.put("sendCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.tcp.send.time.%period",
                        "app.tcp.send.time.std.sum", null, "sendTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.tcp.send.bytes.rate",
                        "app.tcp.send.bytes.std.sum", null, "sendBytes", result);


                return result.toObject();
            }
        });
    }

    protected Object selectHotspotUdps(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.udp.name", "udpExit", "udp", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("receiveCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.udp.receive.time.%period",
                        "app.udp.receive.time.std.sum", null, "receiveTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.udp.receive.bytes.rate",
                        "app.udp.receive.bytes.std.sum", null, "receiveBytes", result);
                result.put("sendCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.udp.send.time.%period",
                        "app.udp.send.time.std.sum", null, "sendTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.udp.send.bytes.rate",
                        "app.udp.send.bytes.std.sum", null, "sendBytes", result);

                return result.toObject();
            }
        });
    }

    protected Object selectHotspotFiles(boolean scope, boolean transaction, Map<String, ?> parameters) {
        return selectHotspots("app.file.name", "fileExit", "files", scope, transaction, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                Json result = Json.object();
                result.put("readCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.file.read.time.%period",
                        "app.file.read.time.std.sum", null, "readTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.file.read.bytes.rate",
                        "app.file.read.bytes.std.sum", null, "readBytes", result);
                result.put("writeCount", Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time.std.count"));
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "app.file.write.time.%period",
                        "app.file.write.time.std.sum", null, "writeTime", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.file.write.bytes.rate",
                        "app.file.write.bytes.std.sum", null, "writeBytes", result);

                return result.toObject();
            }
        });
    }

    private Object selectHotspots(String componentType, String type, String rootMetricName, boolean scope, boolean transaction,
                                  Map<String, ?> parameters, IRepresentationBuilder builder) {
        if (scope)
            return selectHotspotScopes(transaction, componentType, rootMetricName, parameters, builder);
        else
            return selectHotspotMetrics(componentType, type, rootMetricName, parameters, builder);
    }

    private Object selectHotspotScopes(boolean transaction, String componentType, String rootMetricName,
                                       Map<String, ?> parameters, IRepresentationBuilder builder) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        IPeriodName name = nameManager.findByName(Names.getMetric(rootMetricName));
        if (name != null) {
            IGroupComponentVersion version = (IGroupComponentVersion) componentService.getRootGroup().get();
            IGroupComponent rootGroup = null;
            if (version != null) {
                for (IGroupComponent child : version.getChildren()) {
                    if (child.getScope().toString().equals(transaction ? "transactions" : "jvmNodes")) {
                        rootGroup = child;
                        break;
                    }
                }
            }

            IGroupComponentVersion rootGroupVersion = null;
            if (rootGroup != null)
                rootGroupVersion = (IGroupComponentVersion) rootGroup.get();

            if (rootGroupVersion != null) {
                for (IGroupComponent child : rootGroupVersion.getChildren()) {
                    long scopeId = child.getScopeId();
                    INameNode rootNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                            new Location(scopeId, name.getId()), componentType);

                    if (rootNode != null) {
                        boolean hasMetrics = currentTime - rootNode.getPeriod().getEndTime() < 10000;
                        selectHotspotScope(rootNode, hasMetrics, periodType, transaction, currentTime, 0, jsonRows, builder);
                    }
                }
                for (IComponent child : rootGroupVersion.getComponents()) {
                    long scopeId = child.getScopeId();
                    INameNode rootNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                            new Location(scopeId, name.getId()), componentType);

                    if (rootNode != null) {
                        boolean hasMetrics = currentTime - rootNode.getPeriod().getEndTime() < 10000;
                        selectHotspotScope(rootNode, hasMetrics, periodType, transaction, currentTime, 0, jsonRows, builder);
                    }
                }
            }
        }

        return json.toObject();
    }

    private void selectHotspotScope(INameNode nameNode, boolean hasMetrics, String periodType, boolean transaction,
                                    long currentTime, long groupScopeId, final Json jsonRows, IRepresentationBuilder builder) {
        long scopeId = nameNode.getLocation().getScopeId();
        IComponent component = componentService.findComponent(scopeId);
        HealthComponentVersionNode version = null;
        if (component != null)
            version = (HealthComponentVersionNode) component.get();

        if (version == null)
            return;

        Json result = jsonRows.addObject();
        StateInfo info = Selectors.buildState(version, true, selectionService, periodType, currentTime,
                transaction ? "primary.app.entryPoint" : "jvm.kpi");
        boolean group = version instanceof IGroupComponentVersion;
        String id;
        if (group)
            id = Long.toString(scopeId);
        else
            id = Long.toString(groupScopeId) + "-" + scopeId;
        result.put("id", id)
                .put("elementId", Long.toString(scopeId))
                .putIf("group", true, group)
                .putIf("dynamic", true, version.isDynamic())
                .put("state", info.state)
                .put("stateTitle", info.stateTitle)
                .putIf("maintenanceMessage", version.getMaintenanceMessage(), !Strings.isEmpty(version.getMaintenanceMessage()))
                .put("title", Selectors.buildReference(version));

        if (!group && version instanceof ITransactionComponentVersion) {
            INodeComponent node = ((ITransactionComponentVersion) version).getPrimaryNode();
            if (node != null) {
                NodeComponentVersionNode nodeVersion = (NodeComponentVersionNode) node.get();
                if (nodeVersion != null) {
                    info = Selectors.buildState(nodeVersion, true, null, null, 0, null);
                    JsonObjectBuilder reference = Selectors.buildReference(nodeVersion);
                    reference.put("state", info.state);
                    result.put("node", reference);
                }
            }
        }

        JsonObject properties = null;
        if (!group)
            properties = version.getProperties();

        if (properties != null) {
            if (transaction) {
                String type = properties.get("type");
                if (type.contains("http")) {
                    result.put("type", "httpEntry");
                    result.put("app", properties.get("app"));
                    String url = properties.get("url", null);
                    if (url != null)
                        result.put("url", url);
                    String servlet = properties.get("servlet", null);
                    if (servlet != null)
                        result.put("servlet", servlet);
                } else if (type.contains("jms")) {
                    result.put("type", "jmsEntry");
                    result.put("destinationType", properties.get("destinationType"));
                    result.put("destination", properties.get("destination"));
                } else if (type.contains("method"))
                    result.put("type", "methodEntry");

                result.put("platform", "jvm");
                result.put("combineType", properties.get("combineType"));
            } else {
                result.put("vmName", properties.get("vmName"));
                result.put("vmVersion", properties.get("vmVersion"));
                result.put("vmVendor", properties.get("vmVendor"));
                result.put("vmHome", properties.get("vmHome"));
            }
        }

        if (hasMetrics) {
            Object value = Selectors.buildRepresentation(nameNode, 0, builder);
            if (value instanceof JsonObject) {
                for (Map.Entry<String, Object> entry : (JsonObject) value)
                    result.put(entry.getKey(), entry.getValue());
            } else
                result.put("noData", true);
        } else
            result.put("noData", true);

        Json children = null;
        for (INameNode child : nameNode.getScopeChildren()) {
            if (children == null)
                children = result.putArray("children");

            selectHotspotScope(child, hasMetrics, periodType, transaction, currentTime, scopeId, children, builder);
        }
    }

    private Object selectHotspotMetrics(String componentType, String type, String rootMetricName,
                                        Map<String, ?> parameters, IRepresentationBuilder builder) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        long scopeId = component.getScopeId();
        IPeriodName name = nameManager.findByName(Names.getMetric(rootMetricName));
        if (name != null) {
            INameNode rootNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(scopeId, name.getId()), componentType);

            if (rootNode != null) {
                boolean hasMetrics = currentTime - rootNode.getPeriod().getEndTime() < 10000;

                if (rootMetricName.startsWith("$")) {
                    for (INameNode child : rootNode.getMetricChildren())
                        selectHotspotMetric(child, hasMetrics, type, jsonRows.addObject(), builder);
                } else
                    selectHotspotMetric(rootNode, hasMetrics, type, jsonRows.addObject(), builder);
            }
        }

        return json.toObject();
    }

    private int selectHotspotMetric(INameNode nameNode, boolean hasMetrics, String type, final Json result, IRepresentationBuilder builder) {
        boolean errorMetric = type.equals("error");
        Location location = nameNode.getLocation();
        String lastSegment = nameNode.getMetric().getLastSegment().toString();
        boolean errorLocationStart = false;
        if (errorMetric && lastSegment.endsWith("&")) {
            errorLocationStart = true;
            lastSegment = lastSegment.substring(0, lastSegment.length() - 1);
        }

        lastSegment = Selectors.removePrefix(lastSegment);
        if (lastSegment.equals("files"))
            lastSegment = "Files";
        else if (lastSegment.equals("tcp"))
            lastSegment = "TCP requests";
        else if (lastSegment.equals("udp"))
            lastSegment = "UDP requests";
        else if (lastSegment.equals("idbcConnections"))
            lastSegment = "Database connections";
        else if (lastSegment.equals("jdbc"))
            lastSegment = "Database queries";
        else if (lastSegment.equals("jmsProducers"))
            lastSegment = "JMS producers";
        else if (lastSegment.equals("httpConnections"))
            lastSegment = "HTTP connections";

        result.put("id", location.getMetricId())
                .put("elementId", location.getMetricId())
                .put("metricId", location.getMetricId())
                .putObject("title")
                .put("title", Names.unescape(lastSegment))
                .put("link", "#")
                .end()
                .put("type", type);

        if (type.equals("method")) {
            String qualifiedName = nameNode.getMetric().toString();
            int pos = qualifiedName.indexOf("&");
            if (pos != -1)
                qualifiedName = qualifiedName.substring(pos + 2);
            result.put("qualifiedTitle", Names.unescape(qualifiedName));
        }
        if (hasMetrics) {
            Object value = Selectors.buildRepresentation(nameNode, 0, builder);
            if (value instanceof JsonObject) {
                for (Map.Entry<String, Object> entry : (JsonObject) value)
                    result.put(entry.getKey(), entry.getValue());
            } else
                result.put("noData", true);
        } else
            result.put("noData", true);

        Json children = null;
        int level = 0;
        for (INameNode child : nameNode.getMetricChildren()) {
            if (children == null)
                children = result.putArray(errorLocationStart ? "location" : "children");

            level = selectHotspotMetric(child, hasMetrics, errorLocationStart ? "method" : type, children.addObject(), builder) + 1;
        }

        if (level == 0)
            result.put("subType", "method");
        else if (level == 1)
            result.put("subType", "class");
        else
            result.put("subType", "package");

        return level;
    }

    private void buildStackMetrics(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                   IComputeContext computeContext, boolean hotspot, final Json result) {
        String percentageField = hotspot ? "%period" : "%cpu";
        result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.std.count"));
        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "app.cpu.time.inherent." + percentageField, "app.cpu.time.inherent.std.sum", "app.cpu.time.inherent.std.min",
                "app.cpu.time.inherent.std.max", "app.cpu.time.inherent.std.avg", null, "inherentTime", result);
        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "app.cpu.time.total." + percentageField, "app.cpu.time.total.std.sum", "app.cpu.time.total.std.min",
                "app.cpu.time.total.std.max", "app.cpu.time.total.std.avg", null, "totalTime", result);

        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "stack.io.time.inherent." + percentageField, "stack.io.time.inherent.std.sum", "stack.io.time.inherent.std.min",
                "stack.io.time.inherent.std.max", "stack.io.time.inherent.std.avg", null, "inherentIoTime", result);
        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "stack.io.time.total." + percentageField, "stack.io.time.total.std.sum", "stack.io.time.total.std.min",
                "stack.io.time.total.std.max", "stack.io.time.total.std.avg", null, "totalIoTime", result);

        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "stack.db.time.inherent." + percentageField, "stack.db.time.inherent.std.sum", "stack.db.time.inherent.std.min",
                "stack.db.time.inherent.std.max", "stack.db.time.inherent.std.avg", null, "inherentDbTime", result);
        Selectors.buildRelativeMetric(value, accessorFactory, computeContext,
                "stack.db.time.total." + percentageField, "stack.db.time.total.std.sum", "stack.db.time.total.std.min",
                "stack.db.time.total.std.max", "stack.db.time.total.std.avg", null, "totalDbTime", result);

        Selectors.buildRateMetric(value, accessorFactory, computeContext,
                "stack.alloc.bytes.inherent.rate", "stack.alloc.bytes.inherent.std.sum", "stack.alloc.bytes.inherent.std.min",
                "stack.alloc.bytes.inherent.std.max", "stack.alloc.bytes.inherent.std.avg", null, "inherentAllocBytes", result);
        Selectors.buildRateMetric(value, accessorFactory, computeContext,
                "stack.alloc.bytes.total.rate", "stack.alloc.bytes.total.std.sum", "stack.alloc.bytes.total.std.min",
                "stack.alloc.bytes.total.std.max", "stack.alloc.bytes.total.std.avg", null, "totalAllocBytes", result);

        Selectors.buildRateMetric(value, accessorFactory, computeContext,
                "stack.errors.count.inherent.rate", "stack.errors.count.inherent.std.sum", "stack.errors.count.inherent.std.min",
                "stack.errors.count.inherent.std.max", "stack.errors.count.inherent.std.avg", null, "inherentErrorsCount", result);
        Selectors.buildRateMetric(value, accessorFactory, computeContext,
                "stack.errors.count.total.rate", "stack.errors.count.total.std.sum", "stack.errors.count.total.std.min",
                "stack.errors.count.total.std.max", "stack.errors.count.total.std.avg", null, "totalErrorsCount", result);

        result.put("concurrencyLevel", Selectors.getMetric(value, accessorFactory, computeContext, "app.concurrency.std.avg"));
    }

    private Object selectHotspotMethodInherentTimes(Long scopeId, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.stack.name", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.%period"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.inherent.%period"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.inherent.%period"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.std.sum"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.inherent.std.sum"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.inherent.std.sum"))
                        .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.std.count"))
                        .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.inherent.std.count"))
                        .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.inherent.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectHotspotMethodTotalTimes(Long scopeId, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.stack.name", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.total.%period"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.total.%period"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.total.%period"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.total.std.sum"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.total.std.sum"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.total.std.sum"))
                        .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.total.std.count"))
                        .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.total.std.count"))
                        .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.total.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectHotspotMethodAllocatedBytes(Long scopeId, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.stack.name", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.inherent.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.total.rate"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.inherent.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.total.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectHotspotMethodErrors(Long scopeId, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.stack.name", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.inherent.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.total.rate"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.inherent.std.sum"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.total.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectHotspotMethodConcurrencyLevel(Long scopeId, Map<String, ?> parameters) {
        long metricId = (Long) parameters.get("metricId");
        return Selectors.selectTimedModel(selectionService, scopeId, metricId, "app.stack.name", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.concurrency.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectBackTraceMethods(Map<String, ?> parameters) {
        return selectBackTrace("app.stack.name", parameters, new ITraceTreeBuilder<MethodMetrics>() {
            @Override
            public MethodMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                              IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                MethodMetrics metrics = new MethodMetrics();
                Number count = Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.std.count");
                if (count != null)
                    metrics.count = count.longValue();
                Number inherentCpuTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.inherent.std.sum");
                if (inherentCpuTime != null)
                    metrics.inherentCpuTime = inherentCpuTime.longValue();
                if (total) {
                    Number totalCpuTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.cpu.time.total.std.sum");
                    if (totalCpuTime != null)
                        metrics.totalCpuTime = totalCpuTime.longValue();
                }

                Number inherentIoTime = Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.inherent.std.sum");
                if (inherentIoTime != null)
                    metrics.inherentIoTime = inherentIoTime.longValue();
                if (total) {
                    Number totalIoTime = Selectors.getMetric(value, accessorFactory, computeContext, "stack.io.time.total.std.sum");
                    if (totalIoTime != null)
                        metrics.totalIoTime = totalIoTime.longValue();
                }

                Number inherentDbTime = Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.inherent.std.sum");
                if (inherentDbTime != null)
                    metrics.inherentDbTime = inherentDbTime.longValue();
                if (total) {
                    Number totalDbTime = Selectors.getMetric(value, accessorFactory, computeContext, "stack.db.time.total.std.sum");
                    if (totalDbTime != null)
                        metrics.totalDbTime = totalDbTime.longValue();
                }

                Number inherentAllocBytes = Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.inherent.std.sum");
                if (inherentAllocBytes != null)
                    metrics.inherentAllocBytes = inherentAllocBytes.longValue();
                if (total) {
                    Number totalAllocBytes = Selectors.getMetric(value, accessorFactory, computeContext, "stack.alloc.bytes.total.std.sum");
                    if (totalAllocBytes != null)
                        metrics.totalAllocBytes = totalAllocBytes.longValue();
                }

                Number inherentErrorsCount = Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.inherent.std.sum");
                if (inherentErrorsCount != null)
                    metrics.inherentErrorsCount = inherentErrorsCount.longValue();
                if (total) {
                    Number totalErrorsCount = Selectors.getMetric(value, accessorFactory, computeContext, "stack.errors.count.total.std.sum");
                    if (totalErrorsCount != null)
                        metrics.totalErrorsCount = totalErrorsCount.longValue();
                }

                return metrics;
            }

            @Override
            public MethodMetrics createMetrics() {
                return new MethodMetrics();
            }

            @Override
            public void aggregateMetrics(MethodMetrics metrics, MethodMetrics newMetrics, boolean total) {
                metrics.count += newMetrics.count;
                metrics.inherentCpuTime += newMetrics.inherentCpuTime;
                metrics.totalCpuTime += newMetrics.totalCpuTime;

                metrics.inherentIoTime += newMetrics.inherentIoTime;
                metrics.totalIoTime += newMetrics.totalIoTime;

                metrics.inherentDbTime += newMetrics.inherentDbTime;
                metrics.totalDbTime += newMetrics.totalDbTime;

                metrics.inherentAllocBytes += newMetrics.inherentAllocBytes;
                metrics.totalAllocBytes += newMetrics.totalAllocBytes;

                metrics.inherentErrorsCount += newMetrics.inherentErrorsCount;
                metrics.totalErrorsCount += newMetrics.totalErrorsCount;
            }

            @Override
            public void computeMetrics(MethodMetrics metrics, MethodMetrics rootMetrics) {
                metrics.countPercentage = Numbers.percents(metrics.count, rootMetrics.count);
                metrics.inherentCpuTimePercentage = Numbers.percents(metrics.inherentCpuTime, rootMetrics.totalCpuTime);
                metrics.totalCpuTimePercentage = Numbers.percents(metrics.totalCpuTime, rootMetrics.totalCpuTime);

                metrics.inherentIoTimePercentage = Numbers.percents(metrics.inherentIoTime, rootMetrics.totalCpuTime);
                metrics.totalIoTimePercentage = Numbers.percents(metrics.totalIoTime, rootMetrics.totalCpuTime);

                metrics.inherentDbTimePercentage = Numbers.percents(metrics.inherentDbTime, rootMetrics.totalCpuTime);
                metrics.totalDbTimePercentage = Numbers.percents(metrics.totalDbTime, rootMetrics.totalCpuTime);

                metrics.inherentAllocBytesPercentage = Numbers.percents(metrics.inherentAllocBytes, rootMetrics.totalAllocBytes);
                metrics.totalAllocBytesPercentage = Numbers.percents(metrics.totalAllocBytes, rootMetrics.totalAllocBytes);

                metrics.inherentErrorsCountPercentage = Numbers.percents(metrics.inherentErrorsCount, rootMetrics.totalErrorsCount);
                metrics.totalErrorsCountPercentage = Numbers.percents(metrics.totalErrorsCount, rootMetrics.totalErrorsCount);
            }

            @Override
            public Object buildResult(MethodMetrics metrics) {
                return Json.object()
                        .putObject("count").put("relative", metrics.countPercentage).put("absolute", metrics.count).end()
                        .putObject("inherentTime").put("relative", metrics.inherentCpuTimePercentage).put("absolute", metrics.inherentCpuTime).end()
                        .putObject("totalTime").put("relative", metrics.totalCpuTimePercentage).put("absolute", metrics.totalCpuTime).end()
                        .putObject("inherentIoTime").put("relative", metrics.inherentIoTimePercentage).put("absolute", metrics.inherentIoTime).end()
                        .putObject("totalIoTime").put("relative", metrics.totalIoTimePercentage).put("absolute", metrics.totalIoTime).end()
                        .putObject("inherentDbTime").put("relative", metrics.inherentDbTimePercentage).put("absolute", metrics.inherentDbTime).end()
                        .putObject("totalDbTime").put("relative", metrics.totalDbTimePercentage).put("absolute", metrics.totalDbTime).end()
                        .putObject("inherentAllocBytes").put("relative", metrics.inherentAllocBytesPercentage).put("absolute", metrics.inherentAllocBytes).end()
                        .putObject("totalAllocBytes").put("relative", metrics.totalAllocBytesPercentage).put("absolute", metrics.totalAllocBytes).end()
                        .putObject("inherentErrorsCount").put("relative", metrics.inherentErrorsCountPercentage).put("absolute", metrics.inherentErrorsCount).end()
                        .putObject("totalErrorsCount").put("relative", metrics.totalErrorsCountPercentage).put("absolute", metrics.totalErrorsCount).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceJdbcQueries(Map<String, ?> parameters) {
        return selectBackTrace("app.jdbc.name", parameters, new ITraceTreeBuilder<JdbcQueryMetrics>() {
            @Override
            public JdbcQueryMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                                 IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                JdbcQueryMetrics metrics = new JdbcQueryMetrics();
                Number count = Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.count");
                if (count != null)
                    metrics.count = count.longValue();
                Number queryTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.db.query.time.std.sum");
                if (queryTime != null)
                    metrics.queryTime = queryTime.longValue();

                return metrics;
            }

            @Override
            public JdbcQueryMetrics createMetrics() {
                return new JdbcQueryMetrics();
            }

            @Override
            public void aggregateMetrics(JdbcQueryMetrics metrics, JdbcQueryMetrics newMetrics, boolean total) {
                metrics.count += newMetrics.count;
                metrics.queryTime += newMetrics.queryTime;
            }

            @Override
            public void computeMetrics(JdbcQueryMetrics metrics, JdbcQueryMetrics rootMetrics) {
                metrics.countPercentage = Numbers.percents(metrics.count, rootMetrics.count);
                metrics.queryTimePercentage = Numbers.percents(metrics.queryTime, rootMetrics.queryTime);
            }

            @Override
            public Object buildResult(JdbcQueryMetrics metrics) {
                return Json.object()
                        .putObject("count").put("relative", metrics.countPercentage).put("absolute", metrics.count).end()
                        .putObject("queryTime").put("relative", metrics.queryTimePercentage).put("absolute", metrics.queryTime).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceFiles(Map<String, ?> parameters) {
        return selectBackTrace("app.file.name", parameters, new ITraceTreeBuilder<FileMetrics>() {
            @Override
            public FileMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                            IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                FileMetrics metrics = new FileMetrics();
                Number readCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time.std.count");
                if (readCount != null)
                    metrics.readCount = readCount.longValue();
                Number readTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.time.std.sum");
                if (readTime != null)
                    metrics.readTime = readTime.longValue();
                Number readBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.read.bytes.std.sum");
                if (readBytes != null)
                    metrics.readBytes = readBytes.longValue();

                Number writeCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time.std.count");
                if (writeCount != null)
                    metrics.writeCount = writeCount.longValue();
                Number writeTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.time.std.sum");
                if (writeTime != null)
                    metrics.writeTime = writeTime.longValue();
                Number writeBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.file.write.bytes.std.sum");
                if (writeBytes != null)
                    metrics.writeBytes = writeBytes.longValue();

                return metrics;
            }

            @Override
            public FileMetrics createMetrics() {
                return new FileMetrics();
            }

            @Override
            public void aggregateMetrics(FileMetrics metrics, FileMetrics newMetrics, boolean total) {
                metrics.readCount += newMetrics.readCount;
                metrics.readTime += newMetrics.readTime;
                metrics.readBytes += newMetrics.readBytes;

                metrics.writeCount += newMetrics.writeCount;
                metrics.writeTime += newMetrics.writeTime;
                metrics.writeBytes += newMetrics.writeBytes;
            }

            @Override
            public void computeMetrics(FileMetrics metrics, FileMetrics rootMetrics) {
                metrics.readCountPercentage = Numbers.percents(metrics.readCount, rootMetrics.readCount);
                metrics.readTimePercentage = Numbers.percents(metrics.readTime, rootMetrics.readTime);
                metrics.readBytesPercentage = Numbers.percents(metrics.readBytes, rootMetrics.readBytes);

                metrics.writeCountPercentage = Numbers.percents(metrics.writeCount, rootMetrics.writeCount);
                metrics.writeTimePercentage = Numbers.percents(metrics.writeTime, rootMetrics.writeTime);
                metrics.writeBytesPercentage = Numbers.percents(metrics.writeBytes, rootMetrics.writeBytes);
            }

            @Override
            public Object buildResult(FileMetrics metrics) {
                return Json.object()
                        .putObject("readCount").put("relative", metrics.readCountPercentage).put("absolute", metrics.readCount).end()
                        .putObject("readTime").put("relative", metrics.readTimePercentage).put("absolute", metrics.readTime).end()
                        .putObject("readBytes").put("relative", metrics.readBytesPercentage).put("absolute", metrics.readBytes).end()

                        .putObject("writeCount").put("relative", metrics.writeCountPercentage).put("absolute", metrics.writeCount).end()
                        .putObject("writeTime").put("relative", metrics.writeTimePercentage).put("absolute", metrics.writeTime).end()
                        .putObject("writeBytes").put("relative", metrics.writeBytesPercentage).put("absolute", metrics.writeBytes).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceTcps(Map<String, ?> parameters) {
        return selectBackTrace("app.tcp.name", parameters, new ITraceTreeBuilder<TcpMetrics>() {
            @Override
            public TcpMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                           IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                TcpMetrics metrics = new TcpMetrics();

                Number connectCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.std.count");
                if (connectCount != null)
                    metrics.connectCount = connectCount.longValue();
                Number connectTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.connect.time.std.sum");
                if (connectTime != null)
                    metrics.connectTime = connectTime.longValue();

                Number receiveCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.std.count");
                if (receiveCount != null)
                    metrics.receiveCount = receiveCount.longValue();
                Number receiveTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.time.std.sum");
                if (receiveTime != null)
                    metrics.receiveTime = receiveTime.longValue();
                Number receiveBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.receive.bytes.std.sum");
                if (receiveBytes != null)
                    metrics.receiveBytes = receiveBytes.longValue();

                Number sendCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time.std.count");
                if (sendCount != null)
                    metrics.sendCount = sendCount.longValue();
                Number sendTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.time.std.sum");
                if (sendTime != null)
                    metrics.sendTime = sendTime.longValue();
                Number sendBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.tcp.send.bytes.std.sum");
                if (sendBytes != null)
                    metrics.sendBytes = sendBytes.longValue();

                return metrics;
            }

            @Override
            public TcpMetrics createMetrics() {
                return new TcpMetrics();
            }

            @Override
            public void aggregateMetrics(TcpMetrics metrics, TcpMetrics newMetrics, boolean total) {
                metrics.connectCount += newMetrics.connectCount;
                metrics.connectTime += newMetrics.connectTime;

                metrics.receiveCount += newMetrics.receiveCount;
                metrics.receiveTime += newMetrics.receiveTime;
                metrics.receiveBytes += newMetrics.receiveBytes;

                metrics.sendCount += newMetrics.sendCount;
                metrics.sendTime += newMetrics.sendTime;
                metrics.sendBytes += newMetrics.sendBytes;
            }

            @Override
            public void computeMetrics(TcpMetrics metrics, TcpMetrics rootMetrics) {
                metrics.connectCountPercentage = Numbers.percents(metrics.connectCount, rootMetrics.connectCount);
                metrics.connectTimePercentage = Numbers.percents(metrics.connectTime, rootMetrics.connectTime);

                metrics.receiveCountPercentage = Numbers.percents(metrics.receiveCount, rootMetrics.receiveCount);
                metrics.receiveTimePercentage = Numbers.percents(metrics.receiveTime, rootMetrics.receiveTime);
                metrics.receiveBytesPercentage = Numbers.percents(metrics.receiveBytes, rootMetrics.receiveBytes);

                metrics.sendCountPercentage = Numbers.percents(metrics.sendCount, rootMetrics.sendCount);
                metrics.sendTimePercentage = Numbers.percents(metrics.sendTime, rootMetrics.sendTime);
                metrics.sendBytesPercentage = Numbers.percents(metrics.sendBytes, rootMetrics.sendBytes);
            }

            @Override
            public Object buildResult(TcpMetrics metrics) {
                return Json.object()
                        .putObject("connectCount").put("relative", metrics.connectCountPercentage).put("absolute", metrics.connectCount).end()
                        .putObject("connectTime").put("relative", metrics.connectTimePercentage).put("absolute", metrics.connectTime).end()
                        .putObject("receiveCount").put("relative", metrics.receiveCountPercentage).put("absolute", metrics.receiveCount).end()
                        .putObject("receiveTime").put("relative", metrics.receiveTimePercentage).put("absolute", metrics.receiveTime).end()
                        .putObject("receiveBytes").put("relative", metrics.receiveBytesPercentage).put("absolute", metrics.receiveBytes).end()
                        .putObject("sendCount").put("relative", metrics.sendCountPercentage).put("absolute", metrics.sendCount).end()
                        .putObject("sendTime").put("relative", metrics.sendTimePercentage).put("absolute", metrics.sendTime).end()
                        .putObject("sendBytes").put("relative", metrics.sendBytesPercentage).put("absolute", metrics.sendBytes).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceUdps(Map<String, ?> parameters) {
        return selectBackTrace("app.udp.name", parameters, new ITraceTreeBuilder<UdpMetrics>() {
            @Override
            public UdpMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                           IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                UdpMetrics metrics = new UdpMetrics();

                Number receiveCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time.std.count");
                if (receiveCount != null)
                    metrics.receiveCount = receiveCount.longValue();
                Number receiveTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.time.std.sum");
                if (receiveTime != null)
                    metrics.receiveTime = receiveTime.longValue();
                Number receiveBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.receive.bytes.std.sum");
                if (receiveBytes != null)
                    metrics.receiveBytes = receiveBytes.longValue();

                Number sendCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time.std.count");
                if (sendCount != null)
                    metrics.sendCount = sendCount.longValue();
                Number sendTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.time.std.sum");
                if (sendTime != null)
                    metrics.sendTime = sendTime.longValue();
                Number sendBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.udp.send.bytes.std.sum");
                if (sendBytes != null)
                    metrics.sendBytes = sendBytes.longValue();

                return metrics;
            }

            @Override
            public UdpMetrics createMetrics() {
                return new UdpMetrics();
            }

            @Override
            public void aggregateMetrics(UdpMetrics metrics, UdpMetrics newMetrics, boolean total) {
                metrics.receiveCount += newMetrics.receiveCount;
                metrics.receiveTime += newMetrics.receiveTime;
                metrics.receiveBytes += newMetrics.receiveBytes;

                metrics.sendCount += newMetrics.sendCount;
                metrics.sendTime += newMetrics.sendTime;
                metrics.sendBytes += newMetrics.sendBytes;
            }

            @Override
            public void computeMetrics(UdpMetrics metrics, UdpMetrics rootMetrics) {
                metrics.receiveCountPercentage = Numbers.percents(metrics.receiveCount, rootMetrics.receiveCount);
                metrics.receiveTimePercentage = Numbers.percents(metrics.receiveTime, rootMetrics.receiveTime);
                metrics.receiveBytesPercentage = Numbers.percents(metrics.receiveBytes, rootMetrics.receiveBytes);

                metrics.sendCountPercentage = Numbers.percents(metrics.sendCount, rootMetrics.sendCount);
                metrics.sendTimePercentage = Numbers.percents(metrics.sendTime, rootMetrics.sendTime);
                metrics.sendBytesPercentage = Numbers.percents(metrics.sendBytes, rootMetrics.sendBytes);
            }

            @Override
            public Object buildResult(UdpMetrics metrics) {
                return Json.object()
                        .putObject("receiveCount").put("relative", metrics.receiveCountPercentage).put("absolute", metrics.receiveCount).end()
                        .putObject("receiveTime").put("relative", metrics.receiveTimePercentage).put("absolute", metrics.receiveTime).end()
                        .putObject("receiveBytes").put("relative", metrics.receiveBytesPercentage).put("absolute", metrics.receiveBytes).end()
                        .putObject("sendCount").put("relative", metrics.sendCountPercentage).put("absolute", metrics.sendCount).end()
                        .putObject("sendTime").put("relative", metrics.sendTimePercentage).put("absolute", metrics.sendTime).end()
                        .putObject("sendBytes").put("relative", metrics.sendBytesPercentage).put("absolute", metrics.sendBytes).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceHttpConnections(Map<String, ?> parameters) {
        return selectBackTrace("app.httpConnection.name", parameters, new ITraceTreeBuilder<HttpConnectionMetrics>() {
            @Override
            public HttpConnectionMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                                      IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                HttpConnectionMetrics metrics = new HttpConnectionMetrics();

                Number count = Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.count");
                if (count != null)
                    metrics.count = count.longValue();
                Number requestTime = Selectors.getMetric(value, accessorFactory, computeContext, "app.http.time.std.sum");
                if (requestTime != null)
                    metrics.requestTime = requestTime.longValue();

                Number receiveBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.http.receive.bytes.std.sum");
                if (receiveBytes != null)
                    metrics.receiveBytes = receiveBytes.longValue();

                Number sendBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.http.send.bytes.std.sum");
                if (sendBytes != null)
                    metrics.sendBytes = sendBytes.longValue();

                Number errorCount = Selectors.getMetric(value, accessorFactory, computeContext, "app.httpConnection.errors.count.std.sum");
                if (errorCount != null)
                    metrics.errorCount = errorCount.longValue();

                return metrics;
            }

            @Override
            public HttpConnectionMetrics createMetrics() {
                return new HttpConnectionMetrics();
            }

            @Override
            public void aggregateMetrics(HttpConnectionMetrics metrics, HttpConnectionMetrics newMetrics, boolean total) {
                metrics.count += newMetrics.count;
                metrics.requestTime += newMetrics.requestTime;
                metrics.receiveBytes += newMetrics.receiveBytes;
                metrics.sendBytes += newMetrics.sendBytes;
                metrics.errorCount += newMetrics.errorCount;
            }

            @Override
            public void computeMetrics(HttpConnectionMetrics metrics, HttpConnectionMetrics rootMetrics) {
                metrics.countPercentage = Numbers.percents(metrics.count, rootMetrics.count);
                metrics.requestTimePercentage = Numbers.percents(metrics.requestTime, rootMetrics.requestTime);
                metrics.receiveBytesPercentage = Numbers.percents(metrics.receiveBytes, rootMetrics.receiveBytes);
                metrics.sendBytesPercentage = Numbers.percents(metrics.sendBytes, rootMetrics.sendBytes);
                metrics.errorCountPercentage = Numbers.percents(metrics.errorCount, rootMetrics.errorCount);
            }

            @Override
            public Object buildResult(HttpConnectionMetrics metrics) {
                return Json.object()
                        .putObject("count").put("relative", metrics.countPercentage).put("absolute", metrics.count).end()
                        .putObject("requestTime").put("relative", metrics.requestTimePercentage).put("absolute", metrics.requestTime).end()
                        .putObject("receiveBytes").put("relative", metrics.receiveBytesPercentage).put("absolute", metrics.receiveBytes).end()
                        .putObject("sendBytes").put("relative", metrics.sendBytesPercentage).put("absolute", metrics.sendBytes).end()
                        .putObject("sendCount").put("relative", metrics.errorCountPercentage).put("absolute", metrics.errorCount).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTraceJmsProducers(Map<String, ?> parameters) {
        return selectBackTrace("app.jmsProducer.name", parameters, new ITraceTreeBuilder<JmsProducerMetrics>() {
            @Override
            public JmsProducerMetrics buildMetrics(IAggregationNode node, boolean total, IComponentValue value,
                                                   IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                JmsProducerMetrics metrics = new JmsProducerMetrics();

                Number count = Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.std.count");
                if (count != null)
                    metrics.count = count.longValue();

                Number sendBytes = Selectors.getMetric(value, accessorFactory, computeContext, "app.jms.bytes.std.sum");
                if (sendBytes != null)
                    metrics.sendBytes = sendBytes.longValue();

                return metrics;
            }

            @Override
            public JmsProducerMetrics createMetrics() {
                return new JmsProducerMetrics();
            }

            @Override
            public void aggregateMetrics(JmsProducerMetrics metrics, JmsProducerMetrics newMetrics, boolean total) {
                metrics.count += newMetrics.count;
                metrics.sendBytes += newMetrics.sendBytes;
            }

            @Override
            public void computeMetrics(JmsProducerMetrics metrics, JmsProducerMetrics rootMetrics) {
                metrics.countPercentage = Numbers.percents(metrics.count, rootMetrics.count);
                metrics.sendBytesPercentage = Numbers.percents(metrics.sendBytes, rootMetrics.sendBytes);
            }

            @Override
            public Object buildResult(JmsProducerMetrics metrics) {
                return Json.object()
                        .putObject("count").put("relative", metrics.countPercentage).put("absolute", metrics.count).end()
                        .putObject("sendBytes").put("relative", metrics.sendBytesPercentage).put("absolute", metrics.sendBytes).end()
                        .toObject();
            }
        });
    }

    private Object selectBackTrace(String componentType, Map<String, ?> parameters, ITraceTreeBuilder builder) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        long scopeId = component.getScopeId();
        long metricId = (Long) parameters.get("metricId");
        IStackNameNode hotspotNode = (IStackNameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                new Location(scopeId, metricId), componentType);

        if (hotspotNode != null) {
            boolean hasMetrics = currentTime - hotspotNode.getPeriod().getEndTime() < 10000;

            TraceElement rootTrace = new TraceElement(hotspotNode, hasMetrics, builder);
            for (Dependency<IStackNode> dependency : hotspotNode.getDependencies()) {
                Object metrics = null;
                if (hasMetrics)
                    metrics = buildMetrics(dependency.getNode(), dependency.isTotal(), builder);

                rootTrace.add(dependency.getNode(), metrics, dependency.isTotal(), hasMetrics, builder);
            }

            if (hasMetrics)
                rootTrace.computeMetrics(builder);

            rootTrace.buildResult(hasMetrics, jsonRows, null, builder);
        }

        return json.toObject();
    }

    private Object buildMetrics(IAggregationNode node, boolean total, ITraceTreeBuilder builder) {
        IAggregationField field = node.getAggregationField();
        IComputeContext computeContext = field.getComputeContext();
        return builder.buildMetrics(node, total, field.getValue(false),
                field.getSchema().getRepresentations().get(0).getAccessorFactory(), computeContext);
    }

    private String[] buildTraceTitle(JsonObject metadata, IStackNode node) {
        String nodeType = metadata.get("type");
        String type = "unknown";
        String title = node.getMetric().getLastSegment().toString();
        String tooltip = title;
        if (node instanceof ISecondaryEntryPointNode) {
            if (nodeType.contains("http")) {
                type = "httpEntry";
                title = "";
                String app = metadata.get("app", null);
                if (app != null)
                    title = app + ":";
                String url = metadata.get("url", null);
                if (url != null)
                    title += url;
                else
                    title += "all";
            } else if (nodeType.contains("jms")) {
                type = "jmsEntry";
                title = "";
                String destinationType = metadata.get("destinationType", null);
                if (destinationType != null)
                    title = destinationType + ":";
                String destination = metadata.get("destination", null);
                if (destination != null)
                    title += destination;

                if (title.isEmpty())
                    title = "JMS consumer";
            } else if (nodeType.contains("thread")) {
                type = "threadEntry";
                title = "Thread";
            }

            tooltip = title;
        } else if (node instanceof IExitPointNode) {
            boolean correctTitle = true;
            if (nodeType.contains("http"))
                type = "httpExit";
            else if (nodeType.contains("jms")) {
                type = "jmsExit";
                title = "";
                String destinationType = metadata.get("destinationType", null);
                if (destinationType != null)
                    title = destinationType + ":";
                String destination = metadata.get("destination", null);
                if (destination != null)
                    title += destination;

                if (title.isEmpty())
                    title = "JMS producer";
                tooltip = title;
                correctTitle = false;
            } else if (nodeType.contains("thread")) {
                type = "threadExit";
                title = "Thread request";
                tooltip = title;
                correctTitle = false;
            } else if (nodeType.contains("method"))
                type = "methodExit";
            else if (nodeType.contains("file"))
                type = "fileExit";
            else if (nodeType.contains("tcp"))
                type = "tcpExit";
            else if (nodeType.contains("udp"))
                type = "udpExit";
            else if (nodeType.contains("jdbcConnection"))
                type = "jdbcConnectionExit";
            else if (nodeType.contains("jdbc"))
                type = "jdbcExit";

            if (correctTitle && node instanceof IIntermediateExitPointNode && node.isDerived()) {
                int pos = title.lastIndexOf(".", title.lastIndexOf(".") - 1);
                if (pos != -1)
                    title = title.substring(0, pos);
                pos = tooltip.lastIndexOf(".", tooltip.lastIndexOf(".") - 1);
                if (pos != -1)
                    tooltip = tooltip.substring(0, pos);
            }

            if (nodeType.contains("async")) {
                title += " (async)";
                tooltip += " (async)";
            }
        } else {
            type = "method";
            IMetricName metric = node.getMetric().getLastSegment();
            if (metric.getSegments().size() > 2)
                title = metric.getSegments().get(metric.getSegments().size() - 2) + "." + metric.getSegments().get(metric.getSegments().size() - 1);
            String clazz = metadata.get("class", null);
            if (clazz != null)
                tooltip = "class: " + clazz;
            String method = metadata.get("method", null);
            if (method != null)
                tooltip += ", method: " + method;
            String file = metadata.get("file", null);
            if (file != null)
                tooltip += ", file: " + file;
            Number line = metadata.get("line", null);
            if (line != null)
                tooltip += ", line: " + line.toString();
        }

        title = Selectors.removePrefix(title);
        return new String[]{type, Names.unescape(title), Names.unescape(tooltip), metadata.get("node")};
    }

    private static class TraverseInfo {
        private TLongSet traversedNodes = new TLongHashSet(10, 0.5f, Long.MAX_VALUE);
        private List<TLongList> levels = new ArrayList<TLongList>();
        private int levelCount;

        public void beginLevel() {
            levelCount++;
            if (levelCount > levels.size())
                levels.add(new TLongArrayList());
        }

        public boolean isTraversed(long id) {
            return traversedNodes.contains(id);
        }

        public void traverse(long id) {
            TLongList list = levels.get(levelCount - 1);
            list.add(id);
            traversedNodes.add(id);
        }

        public void endLevel() {
            TLongList list = levels.get(levelCount - 1);
            for (int i = 0; i < list.size(); i++) {
                long id = list.get(i);
                traversedNodes.remove(id);
            }

            list.clear();
            levelCount--;
        }
    }

    private interface ITraceTreeBuilder<T> {
        T buildMetrics(IAggregationNode node, boolean total, IComponentValue value, IComponentAccessorFactory accessorFactory,
                       IComputeContext computeContext);

        T createMetrics();

        void aggregateMetrics(T metrics, T newMetrics, boolean total);

        void computeMetrics(T metrics, T rootMetrics);

        Object buildResult(T metrics);
    }

    private class TraceElement {
        private final TraceElement root;
        private final String id;
        private final String title;
        private final String type;
        private final String tooltip;
        private final String node;
        private final boolean secondaryEntryPoint;
        private List<TraceElement> children;
        private final Object metrics;
        private int counter;

        public TraceElement(IStackNameNode node, boolean hasMetrics, ITraceTreeBuilder builder) {
            this.root = this;
            this.id = null;
            this.title = null;
            this.type = null;
            this.tooltip = null;
            this.node = null;
            this.secondaryEntryPoint = false;
            if (hasMetrics)
                this.metrics = buildMetrics(node, true, builder);
            else
                this.metrics = null;
        }

        public TraceElement(TraceElement root, IStackNode node, JsonObject metadata, Object metrics) {
            Assert.notNull(root);
            Assert.notNull(node);
            Assert.notNull(metadata);

            this.root = root;
            String[] title = buildTraceTitle(metadata, node);
            this.type = title[0];
            this.title = title[1];
            this.tooltip = title[2];
            this.node = title[3];
            this.id = this.title + ":" + this.node;
            this.secondaryEntryPoint = node instanceof ISecondaryEntryPointNode;
            this.metrics = metrics;
        }

        public void add(IStackNode node, Object metrics, boolean total, boolean hasMetrics, ITraceTreeBuilder builder) {
            if (node == null || node instanceof IPrimaryEntryPointNode || node instanceof IBackgroundRootNode)
                return;

            TraceElement child = findElement(node);
            if (child == null) {
                JsonObject metadata = node.getMetadata();
                if (metadata == null)
                    return;

                child = new TraceElement(root, node, metadata, builder.createMetrics());

                if (children == null)
                    children = new ArrayList<TraceElement>();

                children.add(child);
            }

            if (hasMetrics)
                builder.aggregateMetrics(child.metrics, metrics, total);

            if (node instanceof ISecondaryEntryPointNode)
                child.add(((ISecondaryEntryPointNode) node).getParentExitPoint(), metrics, total, hasMetrics, builder);
            else
                child.add(node.getParent(), metrics, total, hasMetrics, builder);
        }

        public void computeMetrics(ITraceTreeBuilder builder) {
            if (children == null)
                return;

            for (int i = 0; i < children.size(); i++) {
                TraceElement child = children.get(i);
                builder.computeMetrics(child.metrics, child.root.metrics);

                child.computeMetrics(builder);
            }
        }

        public void buildResult(boolean hasMetrics, Json json, JsonObject ownerNode, ITraceTreeBuilder builder) {
            Json result = null;
            if (root != this) {
                result = json.addObject();
                String id = Integer.toString(root.counter++);
                result.put("id", id)
                        .put("elementId", id)
                        .put("metricId", id)
                        .putObject("title")
                        .put("title", title)
                        .put("tooltip", tooltip)
                        .put("link", "#")
                        .end()
                        .put("platform", "jvm")
                        .put("type", type);

                if (ownerNode == null)
                    ownerNode = Selectors.selectReference(componentService, node);

                if (ownerNode != null)
                    result.put("node", ownerNode);

                if (hasMetrics) {
                    Object value = builder.buildResult(metrics);
                    if (value instanceof JsonObject) {
                        for (Map.Entry<String, Object> entry : (JsonObject) value)
                            result.put(entry.getKey(), entry.getValue());
                    } else
                        result.put("noData", true);
                } else
                    result.put("noData", true);

                if (secondaryEntryPoint)
                    ownerNode = null;
            }

            if (this.children != null) {
                Json children = root == this ? json : null;
                for (TraceElement child : this.children) {
                    if (children == null)
                        children = result.putArray("children");

                    child.buildResult(hasMetrics, children, ownerNode, builder);
                }
            }
        }

        private TraceElement findElement(IStackNode node) {
            if (children == null)
                return null;

            JsonObject metadata = node.getMetadata();
            if (metadata == null)
                return null;

            String[] title = buildTraceTitle(metadata, node);
            String id = title[1] + ":" + title[3];
            for (int i = 0; i < children.size(); i++) {
                TraceElement child = children.get(i);
                if (child.id.equals(id))
                    return child;
            }
            return null;
        }
    }

    private static class MethodMetrics {
        long count;
        double countPercentage;
        long inherentCpuTime;
        double inherentCpuTimePercentage;
        long totalCpuTime;
        double totalCpuTimePercentage;

        long inherentIoTime;
        double inherentIoTimePercentage;
        long totalIoTime;
        double totalIoTimePercentage;

        long inherentDbTime;
        double inherentDbTimePercentage;
        long totalDbTime;
        double totalDbTimePercentage;

        long inherentAllocBytes;
        double inherentAllocBytesPercentage;
        long totalAllocBytes;
        double totalAllocBytesPercentage;

        long inherentErrorsCount;
        double inherentErrorsCountPercentage;
        long totalErrorsCount;
        double totalErrorsCountPercentage;
    }

    private static class JdbcQueryMetrics {
        long count;
        double countPercentage;
        long queryTime;
        double queryTimePercentage;
    }

    private static class FileMetrics {
        long readCount;
        double readCountPercentage;
        long readTime;
        double readTimePercentage;
        long readBytes;
        double readBytesPercentage;
        long writeCount;
        double writeCountPercentage;
        long writeTime;
        double writeTimePercentage;
        long writeBytes;
        double writeBytesPercentage;
    }

    private static class TcpMetrics {
        long connectCount;
        double connectCountPercentage;
        long connectTime;
        double connectTimePercentage;
        long receiveCount;
        double receiveCountPercentage;
        long receiveTime;
        double receiveTimePercentage;
        long receiveBytes;
        double receiveBytesPercentage;
        long sendCount;
        double sendCountPercentage;
        long sendTime;
        double sendTimePercentage;
        long sendBytes;
        double sendBytesPercentage;
    }

    private static class UdpMetrics {
        long receiveCount;
        double receiveCountPercentage;
        long receiveTime;
        double receiveTimePercentage;
        long receiveBytes;
        double receiveBytesPercentage;
        long sendCount;
        double sendCountPercentage;
        long sendTime;
        double sendTimePercentage;
        long sendBytes;
        double sendBytesPercentage;
    }

    private static class HttpConnectionMetrics {
        long count;
        double countPercentage;
        long requestTime;
        double requestTimePercentage;
        long receiveBytes;
        double receiveBytesPercentage;
        long sendBytes;
        double sendBytesPercentage;
        long errorCount;
        double errorCountPercentage;
    }

    private static class JmsProducerMetrics {
        long count;
        double countPercentage;
        long sendBytes;
        double sendBytesPercentage;
    }
}
