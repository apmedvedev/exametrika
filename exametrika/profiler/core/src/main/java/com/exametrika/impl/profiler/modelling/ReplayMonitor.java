/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.modelling;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.profiler.config.ReplayMonitorConfiguration;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.profiler.monitors.MonitorContext;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link ReplayMonitor} is a monitor of measurements replay.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ReplayMonitor extends AbstractMonitor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ReplayMonitor.class);
    private final ReplayMonitorConfiguration configuration;
    private List<Measurement>[] nodeMeasurements;

    public ReplayMonitor(ReplayMonitorConfiguration configuration, IMonitorContext context) {
        super(null, configuration, context, true);

        this.configuration = configuration;
    }

    @Override
    public void measure(List<Measurement> measurements, final long time, final long period, final boolean force) {
        if (this.nodeMeasurements == null)
            buildMeasurements();

        int schemaVersion = context.getConfiguration().getSchemaVersion();
        List<Measurement> list = new ArrayList<Measurement>();
        int count = 0;
        for (int k = 0; k < configuration.getNodesCount(); k++) {
            measurements = nodeMeasurements[k];
            for (int i = 0; i < measurements.size(); i++) {
                list.add(measurements.get(i));

                if (list.size() >= 1000) {
                    context.getMeasurementHandler().handle(new MeasurementSet(list, null, schemaVersion, time, 0));
                    list = new ArrayList<Measurement>();
                }

                count++;
            }

            if (!list.isEmpty()) {
                context.getMeasurementHandler().handle(new MeasurementSet(list, null, schemaVersion, time, 0));
                list = new ArrayList<Measurement>();
            }
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.replay(count));
    }

    @Override
    protected void createMeters() {
    }

    private void buildMeasurements() {
        FileInputStream fileStream = null;
        try {
            NameDictionary dictionary = new NameDictionary();
            DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(dictionary, null);
            fileStream = new FileInputStream(configuration.getFileName());
            InputStream in = new BufferedInputStream(fileStream);
            ByteOutputStream stream = new ByteOutputStream();
            IOs.copy(in, stream);
            ByteInputStream inputStream = new ByteInputStream(stream.getBuffer(), 0, stream.getLength());
            DataDeserialization deserialization = new DataDeserialization(inputStream);
            deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

            IAggregationSchema schema = ((MonitorContext) context).getProfilingService().getAggregationSchema();
            List<MeasurementSet> measurements = new ArrayList<MeasurementSet>();
            while (true) {
                int count = deserialization.readInt();
                if (count == 0)
                    break;

                for (int i = 0; i < count; i++)
                    measurements.add(MeasurementSerializers.deserializeMeasurementSet(deserialization, schema, deserializeDictionary));
            }

            this.nodeMeasurements = buildMeasurements(dictionary, measurements);
        } catch (Exception e) {
            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        } finally {
            IOs.close(fileStream);
        }
    }

    private List<Measurement>[] buildMeasurements(NameDictionary dictionary, List<MeasurementSet> measurements) {
        String hostName = findScopeName(dictionary, measurements, "host.kpi");
        String nodeName = findScopeName(dictionary, measurements, "jvm.kpi");
        Map<MeasurementId, JsonObject> metadataMap = new HashMap<MeasurementId, JsonObject>();
        Map<String, UUID>[] nodesIds = new Map[configuration.getNodesCount()];
        long startTime = measurements.get(0).getTime();
        List<Measurement>[] nodeMeasurements = new List[configuration.getNodesCount()];
        for (MeasurementSet set : measurements) {
            for (Measurement measurement : set.getMeasurements()) {
                MeasurementId id = (MeasurementId) measurement.getId();

                if (set.getTime() - startTime < configuration.getStartPeriod()) {
                    JsonObject metadata = measurement.getValue().getMetadata();
                    if (metadata != null)
                        metadataMap.put(id, metadata);
                    continue;
                }

                NameMeasurementId nameId = new NameMeasurementId((IScopeName) dictionary.names.get(id.getScopeId()),
                        (IMetricLocation) dictionary.names.get(id.getLocationId()), id.getComponentType());

                boolean host = nameId.getComponentType().startsWith("host.");
                String subScope = nameId.getScope().toString().substring(host ? hostName.length() : nodeName.length());

                JsonObject metadata = measurement.getValue().getMetadata();
                if (metadata == null) {
                    metadata = metadataMap.get(id);
                    metadataMap.remove(id);
                }

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.originalMeasurement(nameId, metadata));

                for (int i = 0; i < configuration.getNodesCount(); i++) {
                    Map<String, UUID> ids = nodesIds[i];
                    if (ids == null) {
                        ids = new HashMap<String, UUID>();
                        nodesIds[i] = ids;
                    }

                    String scope = host ? "Host" + i : "Node" + i;
                    String nodeSubScope = subScope;
                    JsonObject nodeMetadata = metadata;
                    if (nodeMetadata != null) {
                        JsonObjectBuilder builder = new JsonObjectBuilder(nodeMetadata);
                        builder.put("node", scope);
                        if (id.getComponentType().equals("host.kpi") || id.getComponentType().equals("jvm.kpi")) {
                            JsonObject nodeProperties = builder.get("nodeProperties", null);
                            if (nodeProperties != null) {
                                nodeProperties = new JsonObjectBuilder(nodeProperties);
                                nodeProperties.put("title", scope);
                                builder.put("nodeProperties", nodeProperties);
                            }
                        }

                        String combineType = builder.get("combineType", null);
                        if (combineType != null && !combineType.equals("stack")) {
                            if (logger.isLogEnabled(LogLevel.ERROR))
                                logger.log(LogLevel.ERROR, messages.combineTypeNoSupported(combineType));
                        }

                        if (id.getComponentType().startsWith("app.") || id.getComponentType().startsWith("secondary.app.")) {
                            int pos = subScope.indexOf("-");
                            if (pos != -1) {
                                String stackId = subScope.substring(pos + 1);
                                if (stackId.length() == 36) {
                                    UUID nodeStackId = ids.get(stackId);
                                    if (nodeStackId == null) {
                                        nodeStackId = UUID.randomUUID();
                                        ids.put(stackId, nodeStackId);
                                    }

                                    nodeSubScope = subScope.substring(0, pos + 1) + nodeStackId.toString();
                                }
                            }
                        }

                        String type = builder.get("type", null);
                        if (type != null && type.contains("intermediate") && type.contains("exit")) {
                            String stackId = builder.get("stackId");
                            UUID nodeStackId = ids.get(stackId);
                            if (nodeStackId == null) {
                                nodeStackId = UUID.randomUUID();
                                ids.put(stackId, nodeStackId);
                            }

                            builder.put("stackId", nodeStackId.toString());
                        }
                        nodeMetadata = builder.toJson();
                    }

                    NameMeasurementId nodeId = new NameMeasurementId(Names.getScope(scope + nodeSubScope), nameId.getLocation(), nameId.getComponentType());

                    IComponentValue value = new ComponentValue(measurement.getValue().getMetrics(), nodeMetadata);
                    List<Measurement> result = nodeMeasurements[i];
                    if (result == null) {
                        result = new ArrayList<Measurement>();
                        nodeMeasurements[i] = result;
                    }

                    result.add(new Measurement(nodeId, value, measurement.getPeriod(), null));

                    if (logger.isLogEnabled(LogLevel.TRACE))
                        logger.log(LogLevel.TRACE, messages.transformedMeasurement(nodeId, nodeMetadata));
                }
            }
        }
        return nodeMeasurements;
    }

    private String findScopeName(NameDictionary dictionary, List<MeasurementSet> measurements, String componentType) {
        for (MeasurementSet set : measurements) {
            for (Measurement measurement : set.getMeasurements()) {
                MeasurementId id = (MeasurementId) measurement.getId();
                if (id.getComponentType().equals(componentType))
                    return dictionary.names.get(id.getScopeId()).toString();
            }
        }
        return null;
    }

    private static class NameDictionary implements INameDictionary {
        private long id = 1;
        private Map<Long, IName> names = new HashMap<Long, IName>();

        public NameDictionary() {
            names.put(0l, Names.rootCallPath());
        }

        @Override
        public long getName(IName name) {
            Assert.notNull(name);

            long id = this.id++;
            Assert.isNull(names.put(id, name));
            return id;
        }

        @Override
        public long getCallPath(long parentCallPathId, long metricId) {
            ICallPath callPath;
            if (parentCallPathId == 0 && metricId == 0)
                callPath = Names.rootCallPath();
            else {
                ICallPath parentCallPath;
                if (parentCallPathId != 0)
                    parentCallPath = (ICallPath) names.get(parentCallPathId);
                else
                    parentCallPath = Names.rootCallPath();

                IMetricName metricName = (IMetricName) names.get(metricId);
                Assert.notNull(metricName);

                callPath = Names.getCallPath(parentCallPath, metricName);
            }

            long id = this.id++;
            Assert.isNull(names.put(id, callPath));
            return id;
        }

        @Override
        public IName getName(long persistentNameId) {
            Assert.supports(false);
            return null;
        }
    }

    private interface IMessages {
        @DefaultMessage("Replay measurements: {0}.")
        ILocalizedMessage replay(int measurementsCount);

        @DefaultMessage("Original measurement: {0}, metadata: \n{1}.")
        ILocalizedMessage originalMeasurement(NameMeasurementId nameId, JsonObject metadata);

        @DefaultMessage("Transformed measurement: {0}, metadata: \n{1}.")
        ILocalizedMessage transformedMeasurement(NameMeasurementId nodeId, JsonObject nodeMetadata);

        @DefaultMessage("Combine type ''{0}'' is not supported.")
        ILocalizedMessage combineTypeNoSupported(String combineType);
    }
}
