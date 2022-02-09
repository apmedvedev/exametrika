/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.modelling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackIdsValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StatisticsValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.UniformHistogramValueSchemaConfiguration;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.values.AggregationSchema;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StackIdsValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link MeasurementsGenerator} is a generator of measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeasurementsGenerator {
    private final int nodesCount;
    private final int primaryEntryPointNodesCount;
    private final int transactionsPerNodeCount;
    private final int transactionSegmentsDepth;
    private final int logRecordsCount;
    private final int stackDepth;
    private final int leafStackEntriesCount;
    private final int maxEndExitPointsCount;
    private final int maxIntermediateExitPointsCount;
    private final int exitPointsPerEntryCount;
    private final CombineType combineType;
    private final int schemaVersion;
    private final MeasurementProfile measurementProfile;
    private final Random random = new Random();
    private Map<Pair<String, String>, List<UUID>> secondaryEntryPoints = new HashMap<Pair<String, String>, List<UUID>>();
    private Map<Pair<String, String>, List<UUID>> prevSecondaryEntryPoints = new HashMap<Pair<String, String>, List<UUID>>();
    private Map<String, Integer> secondaryEntryPointsCount = new HashMap<String, Integer>();
    private Map<String, Integer> prevSecondaryEntryPointsCount = new HashMap<String, Integer>();
    private Map<String, Integer> endExitPointsCount = new HashMap<String, Integer>();
    private Map<String, Integer> intermediateExitPointsCount = new HashMap<String, Integer>();
    private int totalCount;
    private int[] counts = new int[Type.values().length];
    private int actualStackDepth;
    private int actualTransactionDepth;

    public enum MeasurementProfile {
        PROD,
        PROD_MIN,
        PROD_INTERACTION
    }

    public MeasurementsGenerator(int nodesCount, int primaryEntryPointNodesCount, int transactionsPerNodeCount, int transactionSegmentsDepth,
                                 int logRecordsCount, int stackDepth, int leafStackEntriesCount, int maxEndExitPointsCount,
                                 int maxIntermediateExitPointsCount, int exitPointsPerEntryCount, CombineType combineType, int schemaVersion,
                                 MeasurementProfile measurementProfile) {
        Assert.notNull(measurementProfile);

        if (combineType == null) {
            switch (measurementProfile) {
                case PROD_MIN:
                    combineType = CombineType.NODE;
                    break;
                case PROD:
                case PROD_INTERACTION:
                    combineType = CombineType.TRANSACTION;
                    break;
            }
        }
        this.nodesCount = nodesCount;
        this.primaryEntryPointNodesCount = primaryEntryPointNodesCount;
        this.transactionsPerNodeCount = transactionsPerNodeCount;
        this.transactionSegmentsDepth = transactionSegmentsDepth;
        this.logRecordsCount = logRecordsCount;
        this.stackDepth = stackDepth;
        this.leafStackEntriesCount = leafStackEntriesCount;
        this.maxEndExitPointsCount = maxEndExitPointsCount;
        this.maxIntermediateExitPointsCount = maxIntermediateExitPointsCount;
        this.exitPointsPerEntryCount = exitPointsPerEntryCount;
        this.combineType = combineType;
        this.schemaVersion = schemaVersion;
        this.measurementProfile = measurementProfile;
    }

    public List<MeasurementSet> generate() {
        List<MeasurementSet> sets = new ArrayList<MeasurementSet>();
        for (int i = 0; i < nodesCount; i++) {
            String nodeName = "node" + i;
            if (measurementProfile == MeasurementProfile.PROD)
                sets.add(generateBackgroundRootTransactionSegment(nodeName));

            if (i < primaryEntryPointNodesCount) {
                for (int m = 0; m < transactionsPerNodeCount; m++) {
                    String transactionName = "transaction" + m;
                    sets.add(generatePrimaryEntryPointTransactionSegment(nodeName, transactionName));
                }
            }
        }

        for (int i = 0; i < transactionSegmentsDepth; i++) {
            if (!secondaryEntryPoints.isEmpty())
                actualTransactionDepth++;

            prevSecondaryEntryPoints = this.secondaryEntryPoints;
            this.secondaryEntryPoints = new HashMap<Pair<String, String>, List<UUID>>();
            prevSecondaryEntryPointsCount = this.secondaryEntryPointsCount;
            this.secondaryEntryPointsCount = new HashMap<String, Integer>();
            this.endExitPointsCount = new HashMap<String, Integer>();
            this.intermediateExitPointsCount = new HashMap<String, Integer>();
            for (Map.Entry<Pair<String, String>, List<UUID>> entry : prevSecondaryEntryPoints.entrySet())
                sets.add(generateSecondaryEntryPointTransactionSegment(entry.getKey().getValue(), entry.getKey().getKey()));
        }

        printStatistics();
        return sets;
    }

    public IAggregationSchema generateSchema() {
        Set<ComponentValueSchemaConfiguration> configurations = new HashSet<ComponentValueSchemaConfiguration>();
        configurations.add(generateSchema("app.stack.root"));
        configurations.add(generateSchema("app.stack"));
        configurations.add(generateSchema("primary.app.entryPoint"));
        configurations.add(generateSchema("secondary.app.entryPoint"));
        configurations.add(generateSchema("app.httpConnection"));
        configurations.add(generateSchema("app.file"));
        configurations.add(generateSchema("app.log.log"));
        return new AggregationSchema(configurations, 1);
    }

    private MeasurementSet generateBackgroundRootTransactionSegment(String nodeName) {
        long period = 1000;
        IScopeName scope = Names.getScope(nodeName + ".node");
        ICallPath callPath = Names.rootCallPath();
        String componentType = "app.stack.root";
        String combineId;
        switch (combineType) {
            case STACK:
                combineId = null;
                break;
            case TRANSACTION:
                combineId = "entryPoint-" + UUID.randomUUID().toString();
                break;
            case NODE:
                combineId = "entryPoint-" + nodeName;
                break;
            case ALL:
                combineId = "entryPoint-" + "all";
                break;
            default:
                combineId = Assert.error();
        }

        List<Measurement> measurements = new ArrayList<Measurement>();
        measurements.add(generateMeasurement(scope, callPath, nodeName, null, null, componentType, period, combineId, Type.BACKGROUND_ROOT));
        measurements.add(generateMeasurement(scope, callPath, nodeName, null, null, "app.log.log", period, combineId, Type.BACKGROUND_ROOT));
        generateStack(scope, callPath, nodeName, null, componentType, period, measurements, 0, 0, combineId, false);

        return new MeasurementSet(measurements, null, schemaVersion, Times.getCurrentTime(), 0);
    }

    private MeasurementSet generatePrimaryEntryPointTransactionSegment(String nodeName, String transactionName) {
        long period = 1000;
        IScopeName scope = Names.getScope(nodeName + "." + transactionName);
        ICallPath callPath = Names.rootCallPath();
        String componentType = "primary.app.entryPoint";
        String combineId;
        switch (combineType) {
            case STACK:
                combineId = null;
                break;
            case TRANSACTION:
                combineId = "entryPoint-" + UUID.randomUUID().toString();
                break;
            case NODE:
                combineId = "entryPoint-" + nodeName;
                break;
            case ALL:
                combineId = "entryPoint-" + "all";
                break;
            default:
                combineId = Assert.error();
        }

        List<Measurement> measurements = new ArrayList<Measurement>();
        measurements.add(generateMeasurement(scope, callPath, nodeName, componentType, componentType, componentType, period, combineId, Type.PRIMARY_ENTRY));
        measurements.add(generateMeasurement(scope, callPath, nodeName, componentType, null, "app.log.log", period, combineId, Type.PRIMARY_ENTRY));
        generateStack(scope, callPath, nodeName, componentType, componentType, period, measurements, Numbers.log2(transactionsPerNodeCount),
                0, combineId, true);

        return new MeasurementSet(measurements, null, schemaVersion, Times.getCurrentTime(), 0);
    }

    private MeasurementSet generateSecondaryEntryPointTransactionSegment(String nodeName, String combineId) {
        long period = 1000;
        IScopeName scope = Names.getScope(nodeName + "." + combineId);
        ICallPath callPath = Names.rootCallPath();
        String componentType = "secondary.app.entryPoint";

        if (combineType == CombineType.NODE)
            combineId = "entryPoint-" + nodeName;
        else if (combineType == CombineType.STACK)
            combineId = null;

        int count = prevSecondaryEntryPointsCount.get(nodeName);
        List<Measurement> measurements = new ArrayList<Measurement>();
        measurements.add(generateMeasurement(scope, callPath, nodeName, componentType, componentType, componentType, period, combineId, Type.SECONDARY_ENTRY));
        measurements.add(generateMeasurement(scope, callPath, nodeName, componentType, null, "app.log.log", period, combineId, Type.SECONDARY_ENTRY));
        generateStack(scope, callPath, nodeName, componentType, componentType, period, measurements, Numbers.log2(count), 0, combineId, true);

        return new MeasurementSet(measurements, null, schemaVersion, Times.getCurrentTime(), 0);
    }

    private void generateStack(IScopeName scope, ICallPath parentCallPath, String nodeName,
                               String entryComponentType, String parentComponentType, long period, List<Measurement> measurements, int depthOffset, int depth,
                               String combineId, boolean allowIntermediateEntryPoints) {
        if (actualStackDepth < depth)
            actualStackDepth = depth;

        int count = ((1 << Math.min(depthOffset + depth + 1, 20)) <= leafStackEntriesCount) ? 2 : 1;
        for (int i = 0; i < count; i++) {
            ICallPath callPath = Names.getCallPath(parentCallPath, Names.getMetric("level" + depth + "-" + i));
            measurements.add(generateMeasurement(scope, callPath, nodeName,
                    entryComponentType, parentComponentType, "app.stack", period, combineId, Type.STACK));

            if (measurementProfile != MeasurementProfile.PROD_INTERACTION && depth < this.stackDepth)
                generateStack(scope, callPath, nodeName, entryComponentType, "app.stack", period, measurements, depthOffset, depth + 1,
                        combineId, allowIntermediateEntryPoints);
        }

        if (measurementProfile == MeasurementProfile.PROD_INTERACTION || depth == this.stackDepth) {
            int exitCount;
            if (measurementProfile == MeasurementProfile.PROD_INTERACTION)
                exitCount = maxIntermediateExitPointsCount;
            else
                exitCount = 1;

            Integer exits = intermediateExitPointsCount.get(nodeName);
            if (exits == null)
                exits = 0;

            if (allowIntermediateEntryPoints && exits < maxIntermediateExitPointsCount) {
                ICallPath callPath = Names.getCallPath(parentCallPath, Names.getMetric("httpRequests"));
                measurements.add(generateMeasurement(scope, callPath, nodeName,
                        entryComponentType, parentComponentType, "app.httpConnection", period, combineId, Type.INTERMEDIATE_EXIT));

                for (int i = 0; i < exitCount; i++) {
                    callPath = Names.getCallPath(callPath, Names.getMetric("hotspot:http://testsite/mytestapp/level1.level2?query" + exits));
                    measurements.add(generateMeasurement(scope, callPath, nodeName,
                            entryComponentType, "app.httpConnection", "app.httpConnection", period, combineId, Type.INTERMEDIATE_EXIT));

                    exits++;
                }

                intermediateExitPointsCount.put(nodeName, exits);
            }

            exits = endExitPointsCount.get(nodeName);
            if (exits == null)
                exits = 0;
            if (measurementProfile == MeasurementProfile.PROD && exits < maxEndExitPointsCount) {
                ICallPath callPath = Names.getCallPath(parentCallPath, Names.getMetric("files"));
                measurements.add(generateMeasurement(scope, callPath, nodeName,
                        entryComponentType, parentComponentType, "app.file", period, combineId, Type.END_EXIT));
                callPath = Names.getCallPath(callPath, Names.getMetric("/home/user/dir1/dir2/dir3"));
                measurements.add(generateMeasurement(scope, callPath, nodeName,
                        entryComponentType, "app.file", "app.file", period, combineId, Type.END_EXIT));
                exits++;
            }
            endExitPointsCount.put(nodeName, exits);
        }
    }

    private Measurement generateMeasurement(IScopeName scope, IMetricLocation location, String nodeName,
                                            String entryComponentType, String parentComponentType, String componentType, long period, String combineId, Type type) {
        IMeasurementId id = new NameMeasurementId(scope, location, componentType);
        totalCount++;
        counts[type.ordinal()]++;
        if (totalCount > 0 && (totalCount % 5000) == 0)
            printStatistics();
        return new Measurement(id, new ComponentValue(generateMetrics(id, componentType, nodeName, combineId), generateMetadata(nodeName,
                entryComponentType, parentComponentType, componentType, combineId)), period, null);
    }

    private void printStatistics() {
        Debug.print("total: {0}, root: {1}, primary-entry: {2}, secondary-entry: {3}, intermediate-exit: {4}, end-exit: {5}, stack: {6}, "
                        + "stack-depth: {7}, tx-depth: {8}",
                totalCount, counts[Type.BACKGROUND_ROOT.ordinal()], counts[Type.PRIMARY_ENTRY.ordinal()], counts[Type.SECONDARY_ENTRY.ordinal()],
                counts[Type.INTERMEDIATE_EXIT.ordinal()], counts[Type.END_EXIT.ordinal()], counts[Type.STACK.ordinal()],
                actualStackDepth, actualTransactionDepth);
    }

    private JsonObject generateMetadata(String nodeName, String entryComponentType, String parentComponentType, String componentType,
                                        String combineId) {
        Json json = Json.object();

        if (componentType.equals("app.stack.root"))
            json.put("node", nodeName);
        else if (componentType.equals("app.stack")) {
            json.put("node", nodeName)
                    .put("type", "stack,jvm," + (entryComponentType != null ? "transaction" : "background"))
                    .put("class", "com.package1.package2.package3.TestClass")
                    .put("line", 100)
                    .put("method", "testMethod(Object, String, Date): String")
                    .put("parent", parentComponentType)
                    .putIf("entry", entryComponentType, entryComponentType != null);
        } else if (componentType.equals("primary.app.entryPoint") || componentType.equals("secondary.app.entryPoint")) {
            json.put("node", nodeName)
                    .put("type", (componentType.startsWith("primary.") ? "primary" : "secondary") + ",transaction,entry,jvm,http")
                    .put("combineType", combineType.toString().toLowerCase())
                    .put("entry", entryComponentType)
                    .put("requestType", "hotspot")
                    .put("url", "/mytestapp/level1.level2?query0")
                    .put("servlet", "testServlet")
                    .put("app", "testApp")
                    .put("group", "testApp");
        } else if (componentType.equals("app.httpConnection")) {
            UUID stackId = null;
            boolean leaf = parentComponentType.equals("app.httpConnection");
            if (leaf) {
                Integer exitCount = intermediateExitPointsCount.get(nodeName);
                if (exitCount == null)
                    exitCount = 0;
                int nodeIndex = Integer.parseInt(nodeName.substring(4)) + exitCount / exitPointsPerEntryCount;
                if (nodeIndex >= nodesCount)
                    nodeIndex = nodeIndex - nodesCount;
                stackId = UUID.randomUUID();
                if (combineType == CombineType.STACK)
                    combineId = stackId.toString();

                String entryNodeName = "node" + nodeIndex;
                Pair<String, String> pair = new Pair<String, String>(combineId, entryNodeName);
                List<UUID> stackIds = secondaryEntryPoints.get(pair);
                if (stackIds == null) {
                    stackIds = new ArrayList<UUID>();
                    secondaryEntryPoints.put(pair, stackIds);
                }

                stackIds.add(stackId);
                Integer count = secondaryEntryPointsCount.get(entryNodeName);
                if (count == null)
                    count = 0;
                secondaryEntryPointsCount.put(entryNodeName, count + 1);
            }

            json.put("node", nodeName)
                    .putIf("stackId", stackId, leaf)
                    .put("type", "exit,jvm,sync,intermediate,remote,http," + (entryComponentType != null ? "transaction" : "background"))
                    .put("parent", parentComponentType)
                    .putIf("entry", entryComponentType, entryComponentType != null)
                    .putIf("requestType", "hotspot", leaf)
                    .putIf("url", "http://testsite/mytestapp/level1.level2?query0", leaf);
        } else if (componentType.equals("app.file")) {
            json.put("node", nodeName)
                    .put("type", "exit,jvm,sync,end,remote,file," + (entryComponentType != null ? "transaction" : "background"))
                    .put("parent", parentComponentType)
                    .putIf("entry", entryComponentType, entryComponentType != null);
        } else if (componentType.equals("app.log.log")) {
            json.put("node", nodeName)
                    .put("type", "log,jvm," + (entryComponentType != null ? "transaction" : "background"))
                    .putIf("entry", entryComponentType, entryComponentType != null);
        }
        return json.toObject();
    }

    private ComponentValueSchemaConfiguration generateSchema(String componentType) {
        List<MetricValueSchemaConfiguration> metrics = new ArrayList<MetricValueSchemaConfiguration>();
        if (componentType.equals("app.stack.root"))
            metrics.add(generateStackSchema("app.cpu.time"));
        else if (componentType.equals("app.stack")) {
            metrics.add(generateStackSchema("app.cpu.time"));
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackSchema("stack.io.time"));
                metrics.add(generateStackSchema("stack.db.time"));
                metrics.add(generateStackSchema("stack.alloc.bytes"));
                metrics.add(generateStackSchema("stack.errors.count"));
                metrics.add(generateNameSchema1("app.concurrency"));
            }
        } else if (componentType.equals("primary.app.entryPoint") || componentType.equals("secondary.app.entryPoint")) {
            metrics.add(generateStackSchema("app.cpu.time"));
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackSchema("stack.io.time"));
                metrics.add(generateStackSchema("stack.db.time"));
                metrics.add(generateStackSchema("stack.alloc.bytes"));
                metrics.add(generateStackSchema("stack.errors.count"));
            }
            metrics.add(generateStackIdsSchema("stackIds"));
            metrics.add(generateNameSchema3("app.entryPoint.stalls.count"));
            metrics.add(generateNameSchema4("app.transaction.time"));
            metrics.add(generateNameSchema4("app.request.time"));
            metrics.add(generateNameSchema4("app.receive.bytes"));
            metrics.add(generateNameSchema4("app.send.bytes"));
            metrics.add(generateNameSchema3("app.entryPoint.errors.count"));
        } else if (componentType.equals("app.httpConnection")) {
            metrics.add(generateStackSchema("app.cpu.time"));
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackSchema("stack.io.time"));
                metrics.add(generateStackSchema("stack.db.time"));
                metrics.add(generateStackSchema("stack.alloc.bytes"));
                metrics.add(generateStackSchema("stack.errors.count"));
            }
            metrics.add(generateNameSchema4("app.http.time"));
            metrics.add(generateNameSchema4("app.http.receive.bytes"));
            metrics.add(generateNameSchema4("app.http.send.bytes"));
            metrics.add(generateNameSchema3("app.httpConnection.errors.count"));
        } else if (componentType.equals("app.file")) {
            metrics.add(generateStackSchema("app.cpu.time"));
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackSchema("stack.io.time"));
                metrics.add(generateStackSchema("stack.db.time"));
                metrics.add(generateStackSchema("stack.alloc.bytes"));
                metrics.add(generateStackSchema("stack.errors.count"));
            }
            metrics.add(generateNameSchema2("app.file.read.time"));
            metrics.add(generateNameSchema2("app.file.read.bytes"));
            metrics.add(generateNameSchema2("app.file.write.time"));
            metrics.add(generateNameSchema2("app.file.write.bytes"));
        } else if (componentType.equals("app.log.log"))
            metrics.add(generateObjectSchema("app.log.log"));

        return new ComponentValueSchemaConfiguration(componentType, metrics);
    }

    private List<IMetricValue> generateMetrics(IMeasurementId id, String componentType, String nodeName, String combineId) {
        List<IMetricValue> metrics = new ArrayList<IMetricValue>();
        if (componentType.equals("app.stack.root"))
            metrics.add(generateStackValue());
        else if (componentType.equals("app.stack")) {
            metrics.add(generateStackValue());
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateNameValue1());
            }
        } else if (componentType.equals("primary.app.entryPoint") || componentType.equals("secondary.app.entryPoint")) {
            List<UUID> ids = prevSecondaryEntryPoints.get(new Pair<String, String>(combineId, nodeName));
            metrics.add(generateStackValue());
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
            }
            metrics.add(generateStackIdsValue(ids != null ? new HashSet(ids) : null));
            metrics.add(generateNameValue3());
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue3());
        } else if (componentType.equals("app.httpConnection")) {
            metrics.add(generateStackValue());
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
            }
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue4(id));
            metrics.add(generateNameValue3());
        } else if (componentType.equals("app.file")) {
            metrics.add(generateStackValue());
            if (measurementProfile != MeasurementProfile.PROD_MIN) {
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
                metrics.add(generateStackValue());
            }
            metrics.add(generateNameValue2());
            metrics.add(generateNameValue2());
            metrics.add(generateNameValue2());
            metrics.add(generateNameValue2());
        } else if (componentType.equals("app.log.log"))
            metrics.add(generateObjectValue());

        return metrics;
    }

    private StackValueSchemaConfiguration generateStackSchema(String name) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();
        fields.add(new StandardValueSchemaConfiguration());
        return new StackValueSchemaConfiguration(name, fields);
    }

    private IStackValue generateStackValue() {
        List<IFieldValue> inherentFields = new ArrayList<IFieldValue>();
        inherentFields.add(new StandardValue(nextValue() + 1, nextValue(), nextValue(), nextValue()));

        List<IFieldValue> totalFields = new ArrayList<IFieldValue>();
        totalFields.add(new StandardValue(nextValue() + 1, nextValue(), nextValue(), nextValue()));

        return new StackValue(inherentFields, totalFields);
    }

    private StackIdsValueSchemaConfiguration generateStackIdsSchema(String name) {
        return new StackIdsValueSchemaConfiguration(name);
    }

    private IStackIdsValue generateStackIdsValue(Set<UUID> ids) {
        return new StackIdsValue(ids);
    }

    private NameValueSchemaConfiguration generateNameSchema1(String name) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();
        fields.add(new StandardValueSchemaConfiguration());
        fields.add(new UniformHistogramValueSchemaConfiguration(0, 10, 20));
        return new NameValueSchemaConfiguration(name, fields);
    }

    private INameValue generateNameValue1() {
        List<IFieldValue> fields = new ArrayList<IFieldValue>();
        fields.add(new StandardValue(nextValue() + 1, nextValue(), nextValue(), nextValue()));

        return new NameValue(fields);
    }

    private NameValueSchemaConfiguration generateNameSchema2(String name) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();
        fields.add(new StandardValueSchemaConfiguration());
        fields.add(new UniformHistogramValueSchemaConfiguration(0, 10, 40));
        return new NameValueSchemaConfiguration(name, fields);
    }

    private INameValue generateNameValue2() {
        List<IFieldValue> fields = new ArrayList<IFieldValue>();
        fields.add(new StandardValue(nextValue() + 1, nextValue(), nextValue(), nextValue()));
        fields.add(new HistogramValue(nextValue(40), 10, 10));

        return new NameValue(fields);
    }

    private NameValueSchemaConfiguration generateNameSchema3(String name) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();
        fields.add(new StandardValueSchemaConfiguration());
        return new NameValueSchemaConfiguration(name, fields);
    }

    private INameValue generateNameValue3() {
        List<IFieldValue> fields = new ArrayList<IFieldValue>();
        fields.add(new StandardValue(nextValue() + 1, nextValue(), nextValue(), nextValue()));

        return new NameValue(fields);
    }

    private NameValueSchemaConfiguration generateNameSchema4(String name) {
        List<FieldValueSchemaConfiguration> fields = new ArrayList<FieldValueSchemaConfiguration>();
        fields.add(new StandardValueSchemaConfiguration());
        fields.add(new StatisticsValueSchemaConfiguration());
        fields.add(new UniformHistogramValueSchemaConfiguration(0, 10, 40));
        fields.add(new InstanceValueSchemaConfiguration(10, true));
        return new NameValueSchemaConfiguration(name, fields);
    }

    private INameValue generateNameValue4(IMeasurementId id) {
        JsonObject context = Json.object().put("key" + nextValue(), "value" + nextValue()).toObject();
        List<InstanceRecord> records = new ArrayList<InstanceRecord>();
        for (int i = 0; i < 10; i++)
            records.add(new InstanceRecord(id, context, i, 0));

        List<IFieldValue> fields = new ArrayList<IFieldValue>();
        fields.add(new StandardValue(nextValue(), nextValue(), nextValue(), nextValue()));
        fields.add(new StatisticsValue(nextValue()));
        fields.add(new HistogramValue(nextValue(40), 10, 10));
        fields.add(new InstanceValue(records));

        return new NameValue(fields);
    }

    private ObjectValueSchemaConfiguration generateObjectSchema(String name) {
        return new ObjectValueSchemaConfiguration(name);
    }

    private IObjectValue generateObjectValue() {
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (int i = 0; i < logRecordsCount; i++) {
            JsonObject object = Json.object()
                    .put("type", "log")
                    .put("time", nextValue())
                    .put("message", "Very long test message.")
                    .put("thread", "[Test thread]")
                    .put("logger", "TestLogger")
                    .put("level", "debug")
                    .toObject();
            builder.add(object);
        }
        return new ObjectValue(builder.toJson());
    }

    private long nextValue() {
        return random.nextLong();
    }

    private long[] nextValue(int size) {
        long[] data = new long[size];
        for (int i = 0; i < size; i++)
            data[i] = random.nextLong();
        return data;
    }

    private enum Type {
        BACKGROUND_ROOT,
        PRIMARY_ENTRY,
        SECONDARY_ENTRY,
        INTERMEDIATE_EXIT,
        END_EXIT,
        STACK
    }
}
