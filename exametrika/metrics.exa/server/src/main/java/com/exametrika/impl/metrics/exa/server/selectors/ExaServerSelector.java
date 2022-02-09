/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.selectors.Selector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link ExaServerSelector} is an ExaServer selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerSelector extends Selector {
    private final ISelectionService selectionService;
    private static final String[] pageTypeNames = new String[]{"normal", "small", "smallMedium", "medium", "largeMedium", "large", "extraLarge"};
    private IPeriodNameManager nameManager;
    private long aggregatorMetricId;
    private long messagingMetricId;
    private long pageCacheMetricId;
    private long nodeCacheMetricId;
    private long pageTypeMetricIds[];

    public ExaServerSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema);

        selectionService = this.schema.getContext().getTransactionProvider().getTransaction().findDomainService(ISelectionService.NAME);
        Assert.notNull(selectionService);
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        ensureNames();

        String type = (String) parameters.get("type");
        if (type.equals("properties"))
            return selectProperties(parameters);
        else if (type.equals("aggregateTime"))
            return selectAggregateTime(parameters);
        else if (type.equals("aggregateTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator", "exa.aggregator.aggregateTime",
                    aggregatorMetricId, parameters);
        else if (type.equals("aggregateCount"))
            return selectAggregateCount(parameters);
        else if (type.equals("aggregateCountHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator", "exa.aggregator.aggregateCount",
                    aggregatorMetricId, parameters);
        else if (type.equals("closePeriodTime"))
            return selectClosePeriodTime(parameters);
        else if (type.equals("closePeriodTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator", "exa.aggregator.closePeriodTime",
                    aggregatorMetricId, parameters);
        else if (type.equals("selectTime"))
            return selectSelectTime(parameters);
        else if (type.equals("selectTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator", "exa.aggregator.selectTime",
                    aggregatorMetricId, parameters);
        else if (type.equals("selectSize"))
            return selectSelectSize(parameters);
        else if (type.equals("selectSizeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator", "exa.aggregator.selectSize",
                    aggregatorMetricId, parameters);
        else if (type.equals("sendBytes"))
            return selectSendBytes(parameters);
        else if (type.equals("sendBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.messaging", "exa.messaging.send.bytes",
                    messagingMetricId, parameters);
        else if (type.equals("receiveBytes"))
            return selectReceiveBytes(parameters);
        else if (type.equals("receiveBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.messaging", "exa.messaging.receive.bytes",
                    messagingMetricId, parameters);
        else if (type.equals("messagingErrorCount"))
            return selectMessagingErrorCount(parameters);
        else if (type.equals("fullText"))
            return selectFullText(parameters);
        else if (type.equals("logErrorCount"))
            return selectLogErrorCount(parameters);
        else if (type.equals("logErrors"))
            return Selectors.selectLog(component.getScopeId(), selectionService, "exa.log.errors", "exa.log.errors", 0, parameters);
        else if (type.equals("pagePool"))
            return selectPagePool(parameters);
        else if (type.equals("pagePoolHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.pagePool",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileReadTime"))
            return selectFileReadTime(parameters);
        else if (type.equals("fileReadTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.file.read.time",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileReadBytes"))
            return selectFileReadBytes(parameters);
        else if (type.equals("fileReadBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.file.read.bytes",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileWriteTime"))
            return selectFileWriteTime(parameters);
        else if (type.equals("fileWriteTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.file.write.time",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileWriteBytes"))
            return selectFileWriteBytes(parameters);
        else if (type.equals("fileWriteBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.file.write.bytes",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileCurrentLoaded"))
            return selectFileCurrentLoaded(parameters);
        else if (type.equals("fileCurrentLoadedHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.file.currentLoaded",
                    aggregatorMetricId, parameters);
        else if (type.equals("fileLoaded"))
            return selectFileLoaded(parameters);
        else if (type.equals("fileUnloaded"))
            return selectFileUnloaded(parameters);
        else if (type.equals("transactionLogFlushTime"))
            return selectTransactionLogFlushTime(parameters);
        else if (type.equals("transactionLogFlushTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.transactionLog.flush.time",
                    aggregatorMetricId, parameters);
        else if (type.equals("transactionLogFlushBytes"))
            return selectTransactionLogFlushBytes(parameters);
        else if (type.equals("transactionLogFlushBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.transactionLog.flush.bytes",
                    aggregatorMetricId, parameters);
        else if (type.equals("transactionQueueSize"))
            return selectTransactionQueueSize(parameters);
        else if (type.equals("transactionQueueSizeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.transaction.queue",
                    aggregatorMetricId, parameters);
        else if (type.equals("transactionTime"))
            return selectTransactionTime(parameters);
        else if (type.equals("transactionTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb", "exa.rawdb.transaction.time",
                    aggregatorMetricId, parameters);
        else if (type.equals("transactionErrorsCount"))
            return selectTransactionErrorsCount(parameters);
        else if (type.equals("transactionLogErrors"))
            return Selectors.selectLog(component.getScopeId(), selectionService, "exa.rawdb.transaction.errors.log", "exa.rawdb.transaction.errors.log",
                    aggregatorMetricId, parameters);
        else if (type.equals("regions"))
            return selectRegions(parameters);
        else if (type.equals("regionCount"))
            return selectRegionCount(parameters);
        else if (type.equals("regionsCountHistogram")) {
            ((Map) parameters).put("subScopeAsMetricId", true);
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageType", "exa.rawdb.pageType.regionsCount",
                    0, parameters);
        } else if (type.equals("regionSize"))
            return selectRegionSize(parameters);
        else if (type.equals("regionsSizeHistogram")) {
            ((Map) parameters).put("subScopeAsMetricId", true);
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageType", "exa.rawdb.pageType.regionsSize",
                    0, parameters);
        } else if (type.equals("regionAllocated"))
            return selectRegionAllocated(parameters);
        else if (type.equals("regionsAllocatedHistogram")) {
            ((Map) parameters).put("subScopeAsMetricId", true);
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageType", "exa.rawdb.pageType.allocated",
                    0, parameters);
        } else if (type.equals("regionFreed"))
            return selectRegionFree(parameters);
        else if (type.equals("regionsFreedHistogram")) {
            ((Map) parameters).put("subScopeAsMetricId", true);
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageType", "exa.rawdb.pageType.freed",
                    0, parameters);
        } else if (type.equals("pageCacheSize"))
            return selectPageCacheSize(parameters);
        else if (type.equals("pageCacheSizeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageCache", "exa.rawdb.pageCache.size",
                    pageCacheMetricId, parameters);
        else if (type.equals("pageCacheQuota"))
            return selectPageCacheQuota(parameters);
        else if (type.equals("pageCacheQuotaHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.rawdb.pageCache", "exa.rawdb.pageCache.quota",
                    pageCacheMetricId, parameters);
        else if (type.equals("pageLoaded"))
            return selectPageLoaded(parameters);
        else if (type.equals("pageUnloaded"))
            return selectPageUnloaded(parameters);
        else if (type.equals("pageUnloadedByOverflow"))
            return selectPageUnloadedByOverflow(parameters);
        else if (type.equals("pageUnloadedByTimer"))
            return selectPageUnloadedByTimer(parameters);
        else if (type.equals("nodeCacheSize"))
            return selectNodeCacheSize(parameters);
        else if (type.equals("nodeCacheSizeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.exadb.nodeCache", "exa.exadb.nodeCache.size",
                    nodeCacheMetricId, parameters);
        else if (type.equals("nodeCacheQuota"))
            return selectNodeCacheQuota(parameters);
        else if (type.equals("nodeCacheQuotaHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.exadb.nodeCache", "exa.exadb.nodeCache.quota",
                    nodeCacheMetricId, parameters);
        else if (type.equals("nodeLoaded"))
            return selectNodeLoaded(parameters);
        else if (type.equals("nodeUnloaded"))
            return selectNodeUnloaded(parameters);
        else if (type.equals("nodeUnloadedByOverflow"))
            return selectNodeUnloadedByOverflow(parameters);
        else if (type.equals("nodeUnloadedByTimer"))
            return selectNodeUnloadedByTimer(parameters);
        else if (type.equals("nameCacheSize"))
            return selectNameCacheSize(parameters);
        else if (type.equals("nameCacheSizeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator.nameCache", "exa.aggregator.nameCache.size",
                    aggregatorMetricId, parameters);
        else if (type.equals("nameCacheQuota"))
            return selectNameCacheQuota(parameters);
        else if (type.equals("nameCacheQuotaHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.aggregator.nameCache", "exa.aggregator.nameCache.quota",
                    aggregatorMetricId, parameters);
        else if (type.equals("nameLoaded"))
            return selectNameLoaded(parameters);
        else if (type.equals("nameUnloaded"))
            return selectNameUnloaded(parameters);
        else if (type.equals("nameUnloadedByOverflow"))
            return selectNameUnloadedByOverflow(parameters);
        else if (type.equals("nameUnloadedByTimer"))
            return selectNameUnloadedByTimer(parameters);
        else if (type.equals("memoryManager"))
            return selectMemoryManager(parameters);
        else
            return Assert.error();
    }

    private Object selectProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");
        IComponentVersion version = component.get();
        if (version != null) {
            StateInfo info = Selectors.buildState(version, true, null, periodType, currentTime, null);

            Json jsonRow = jsonRows.addObject();
            jsonRow.put("id", "server")
                    .put("elementId", "server")
                    .put("title", version.getTitle())
                    .put("description", version.getDescription())
                    .put("state", info.state);

            JsonObject properties = version.getProperties();
            if (properties != null) {
                String home = properties.get("home");
                String bootConfigurationPath = properties.get("bootConfigurationPath");
                String serviceConfigurationPath = properties.get("serviceConfigurationPath");
                if (bootConfigurationPath.startsWith(home))
                    bootConfigurationPath = "<home>" + bootConfigurationPath.substring(home.length());
                if (serviceConfigurationPath.startsWith(home))
                    serviceConfigurationPath = "<home>" + serviceConfigurationPath.substring(home.length());

                jsonRow.put("version", properties.get("version"))
                        .put("home", home)
                        .put("bootConfigurationPath", bootConfigurationPath)
                        .put("serviceConfigurationPath", serviceConfigurationPath);
            }
        }
        return json.toObject();
    }

    private Object selectAggregateTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.aggregateTime.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.aggregateTime.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.aggregateTime.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectAggregateCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.aggregateCount.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.aggregateCount.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectClosePeriodTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.closePeriodTime.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.closePeriodTime.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.closePeriodTime.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectSelectTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.selectTime.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.selectTime.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.selectTime.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectSelectSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.selectSize.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.selectSize.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectSendBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.send.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectReceiveBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.receive.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectMessagingErrorCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.errors.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFullText(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.exadb.fullText", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.addTime.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.updateTime.std.avg"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.deleteTime.std.avg"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.searchTime.std.avg"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.searcherUpdateTime.std.avg"))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.fullText.commitTime.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectLogErrorCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.log", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.log.errorCount.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectPagePool(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pagePool.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectFileReadTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.read.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.read.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.read.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectFileReadBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.read.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.read.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFileWriteTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.write.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.write.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.write.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectFileWriteBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.write.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.write.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFileCurrentLoaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.currentLoaded.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectFileLoaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.loaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.loaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectFileUnloaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.unloaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.file.unloaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectTransactionLogFlushTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transactionLog.flush.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transactionLog.flush.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transactionLog.flush.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectTransactionLogFlushBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transactionLog.flush.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transactionLog.flush.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectTransactionQueueSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transaction.queue.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectTransactionTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transaction.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transaction.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transaction.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectTransactionErrorsCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.rawdb", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.transaction.errors.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectRegions(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRegionGroup = json.putArray("rows").addObject();

        selectRegionMetric(periodType, currentTime, "Page types", component.getScopeId(), jsonRegionGroup);
        Json jsonRegionChildren = jsonRegionGroup.putArray("children");

        for (int i = 0; i < pageTypeNames.length; i++)
            selectRegionMetric(periodType, currentTime, pageTypeNames[i], pageTypeMetricIds[i], jsonRegionChildren.addObject());

        return json.toObject();
    }

    private void selectRegionMetric(String periodType, long currentTime, String title, long metricId, final Json result) {
        result.put("id", metricId)
                .put("elementId", metricId)
                .putObject("title")
                .put("title", title)
                .put("link", "#");

        if (selectionService.buildRepresentation(periodType, currentTime,
                new Location(component.getScopeId(), metricId), "exa.rawdb.pageType", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        result.put("count", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.regionsCount.std.avg"));
                        result.put("size", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.regionsSize.std.avg"));

                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.allocated.rate",
                                "exa.rawdb.pageType.allocated.std.sum", null, "allocated", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.freed.rate",
                                "exa.rawdb.pageType.freed.std.sum", null, "freed", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                }) == null)
            result.put("noData", true);
    }

    private Object selectRegionCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), (Long) parameters.get("subScopeId"), "exa.rawdb.pageType", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.regionsCount.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectRegionSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), (Long) parameters.get("subScopeId"), "exa.rawdb.pageType", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.regionsSize.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectRegionAllocated(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), (Long) parameters.get("subScopeId"), "exa.rawdb.pageType", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.allocated.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.allocated.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectRegionFree(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), (Long) parameters.get("subScopeId"), "exa.rawdb.pageType", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.freed.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageType.freed.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectPageCacheSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.size.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectPageCacheQuota(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.quota.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectPageLoaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.totalLoaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.totalLoaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectPageUnloaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.totalUnloaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.totalUnloaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectPageUnloadedByOverflow(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.unloadedByOverflow.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.unloadedByOverflow.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectPageUnloadedByTimer(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), pageCacheMetricId, "exa.rawdb.pageCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.unloadedByTimer.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.pageCache.unloadedByTimer.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeCacheSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.size.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeCacheQuota(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.quota.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeLoaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.totalLoaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.totalLoaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeUnloaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.totalUnloaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.totalUnloaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeUnloadedByOverflow(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.unloadedByOverflow.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.unloadedByOverflow.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNodeUnloadedByTimer(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), nodeCacheMetricId, "exa.exadb.nodeCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.unloadedByTimer.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.exadb.nodeCache.unloadedByTimer.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNameCacheSize(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.size.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectNameCacheQuota(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.quota.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectNameLoaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.totalLoaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.totalLoaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNameUnloaded(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.totalUnloaded.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.totalUnloaded.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNameUnloadedByOverflow(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.unloadedByOverflow.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.unloadedByOverflow.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectNameUnloadedByTimer(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), aggregatorMetricId, "exa.aggregator.nameCache", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.unloadedByTimer.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.aggregator.nameCache.unloadedByTimer.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectMemoryManager(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        final Json json = Json.object().put("fullUpdate", true);
        final boolean[] completed = new boolean[1];
        selectionService.buildRepresentation(periodType, currentTime,
                new Location(component.getScopeId(), aggregatorMetricId), "exa.rawdb", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        JsonObject memoryManager = Selectors.getMetric(value, accessorFactory, computeContext, "exa.rawdb.memoryManager");
                        if (memoryManager != null) {
                            int[] counter = new int[]{1};
                            json.put("rows", buildChildrenProperties(counter, memoryManager));
                            completed[0] = true;
                        }

                        return null;
                    }
                });
        if (!completed[0])
            json.putArray("rows");
        return json.toObject();
    }

    protected JsonArray buildChildrenProperties(int[] counter, IJsonCollection collection) {
        if (collection instanceof JsonArray) {
            JsonArray properties = (JsonArray) collection;
            Json json = Json.array();
            int i = 0;
            for (Object property : properties) {
                int id = counter[0]++;
                Json jsonRow = json.addObject();
                jsonRow.put("id", id)
                        .put("elementId", id)
                        .put("name", "[" + i + "]");

                if (property instanceof IJsonCollection)
                    jsonRow.put("children", buildChildrenProperties(counter, (IJsonCollection) property));
                else
                    jsonRow.put("value", property);

                i++;
            }

            return json.toArray();
        } else {
            JsonObject properties = (JsonObject) collection;
            Json json = Json.array();
            for (Map.Entry<String, Object> entry : properties) {
                int id = counter[0]++;
                String name = entry.getKey();
                if (name.charAt(0) == '<')
                    name = name.substring(1, name.length() - 1);

                Json jsonRow = json.addObject();
                jsonRow.put("id", id)
                        .put("elementId", id)
                        .put("name", name);

                Object property = entry.getValue();
                if (property instanceof IJsonCollection)
                    jsonRow.put("children", buildChildrenProperties(counter, (IJsonCollection) property));
                else
                    jsonRow.put("value", property);
            }

            return json.toArray();
        }
    }

    private void ensureNames() {
        if (aggregatorMetricId == 0)
            aggregatorMetricId = getMetricId("aggregators.exadb");
        if (messagingMetricId == 0) {
            IComponentVersion version = component.get();
            if (version != null) {
                JsonObject properties = version.getProperties();
                if (properties != null)
                    messagingMetricId = getMetricId("channels." + properties.get("node") + ".server");
            }
        }
        if (pageCacheMetricId == 0)
            pageCacheMetricId = getMetricId("aggregators.exadb.pages.default");
        if (nodeCacheMetricId == 0)
            nodeCacheMetricId = getMetricId("aggregators.exadb.nodes.default");
        if (pageTypeMetricIds == null) {
            pageTypeMetricIds = new long[pageTypeNames.length];
            for (int i = 0; i < pageTypeNames.length; i++)
                pageTypeMetricIds[i] = getMetricId("aggregators.exadb.pages." + pageTypeNames[i]);
        }
    }

    private long getMetricId(String metricName) {
        if (nameManager == null) {
            nameManager = this.schema.getContext().getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
            Assert.notNull(nameManager);
        }

        IPeriodName name = nameManager.findByName(Names.getMetric(metricName));
        if (name != null)
            return name.getId();
        else
            return 0;
    }
}
