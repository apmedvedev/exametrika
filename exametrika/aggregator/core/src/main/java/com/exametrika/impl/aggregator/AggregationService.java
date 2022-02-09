/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.IPeriodNode;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.api.aggregator.common.values.IStandardValue;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.IPrimaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.aggregator.nodes.IStackLogNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.INameNodeSchema;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.aggregator.schema.IStackLogNodeSchema;
import com.exametrika.api.aggregator.schema.IStackNodeSchema;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.services.Services;
import com.exametrika.common.tasks.ThreadInterruptedException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.RuleContext.RuleExecutorInfo;
import com.exametrika.impl.aggregator.common.values.AggregationContext;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.nodes.AggregationNode;
import com.exametrika.impl.aggregator.nodes.BackgroundRootNode;
import com.exametrika.impl.aggregator.nodes.EntryPointNode;
import com.exametrika.impl.aggregator.nodes.IntermediateExitPointNode;
import com.exametrika.impl.aggregator.nodes.NameNode;
import com.exametrika.impl.aggregator.nodes.PrimaryEntryPointNode;
import com.exametrika.impl.aggregator.nodes.RootNode;
import com.exametrika.impl.aggregator.nodes.SecondaryEntryPointNode;
import com.exametrika.impl.aggregator.nodes.StackErrorLogNode;
import com.exametrika.impl.aggregator.nodes.StackLogNode;
import com.exametrika.impl.aggregator.nodes.StackNameNode;
import com.exametrika.impl.aggregator.nodes.StackNode;
import com.exametrika.impl.aggregator.schema.AggregationServiceSchema;
import com.exametrika.spi.aggregator.IAggregationAnalyzer;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IAggregationService;
import com.exametrika.spi.aggregator.IComponentBindingStrategy;
import com.exametrika.spi.aggregator.IComponentDeletionStrategy;
import com.exametrika.spi.aggregator.IComponentDiscoveryStrategy;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IParentDomainHandlerFactory;
import com.exametrika.spi.aggregator.IPeriodClosureListener;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.aggregator.IRuleService;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.MetricHierarchy;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link AggregationService} is a aggregation service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregationService extends DomainService implements IAggregationService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AggregationService.class);
    private static final int START_STATE = 0;
    private static final int DISCOVER_COMPONENTS_STATE = 1;
    private static final int TRANSFER_END_NODES_STATE = 2;
    private static final int TRANSFORM_LOGS_STATE = 3;
    private static final int RESOLVE_SECONDARY_ENTRYPOINTS_STATE = 4;
    private static final int AGGREGATE_NAMES_STATE = 5;
    private static final int AGGREGATE_BACKGROUND_ROOTS_STATE = 6;
    private static final int AGGREGATE_TRANSACTION_ROOTS_STATE = 7;
    private static final int CLOSE_FIELDS_STATE = 8;
    private static final int TRANSFER_DERIVED_NODES_STATE = 9;
    private IPeriodSpaceSchema spaceSchema;
    private IPeriodNameManager nameManager;
    private final AggregationContext aggregationContext = new AggregationContext();
    private IMeasurementHandler parentDomainHandler;
    private final Set<IPeriodClosureListener> listeners = new HashSet<IPeriodClosureListener>();
    private TLongObjectMap<EntryPointHierarchy> hierarchyMap = new TLongObjectHashMap<EntryPointHierarchy>();
    private boolean transactionAggregation;
    private boolean closing;
    private List<MeasurementSet> pendingMeasurements = new ArrayList<MeasurementSet>();

    @Override
    public AggregationServiceSchema getSchema() {
        return (AggregationServiceSchema) super.getSchema();
    }

    @Override
    public IAggregationSchema getAggregationSchema() {
        return getSchema().getAggregationSchema();
    }

    @Override
    public void addPeriodClosureListener(IPeriodClosureListener listener) {
        Assert.notNull(listener);

        listeners.add(listener);
    }

    @Override
    public void removePeriodClosureListener(IPeriodClosureListener listener) {
        Assert.notNull(listener);

        listeners.remove(listener);
    }

    @Override
    public void aggregate(MeasurementSet measurements) {
        if (measurements.getSchemaVersion() != getSchema().getConfiguration().getAggregationSchema().getVersion())
            return;

        if (closing) {
            pendingMeasurements.add(measurements);
            return;
        }

        aggregatePendingMeasurements();
        aggregateMeasurements(measurements);
    }

    public boolean closePeriod(ClosePeriodBatchOperation batch, IBatchControl batchControl, Period period, boolean schemaChange) {
        int state = 0;
        if (batch != null)
            state = batch.getAggregationState();

        ensureSpaceSchema();

        boolean interceptResult;
        if (state == START_STATE) {
            aggregatePendingMeasurements();

            if (logger.isLogEnabled(LogLevel.TRACE))
                logger.log(LogLevel.TRACE, messages.beginClose(period.getSpace().getSchema().getConfiguration().getName()));

            interceptResult = AggregatorInterceptor.INSTANCE.onBeforePeriodClosed(((RawDatabase) context.getRawDatabase()).getInterceptId());
            if (batch != null) {
                batch.setInterceptResult(interceptResult);
                batch.setAggregationNodeId(0);

                closing = true;
            }
        } else
            interceptResult = batch.getInterceptResult();

        CombineType combineType = getSchema().getConfiguration().getAggregationSchema().getCombineType();

        Throwable exception = null;
        try {
            if (state <= DISCOVER_COMPONENTS_STATE) {
                if (!schemaChange) {
                    if (!discoverComponents(batch, batchControl, period))
                        return false;
                }

                if (batch != null)
                    batch.setAggregationNodeId(0);

                fireOnBeforePeriodClosed(period);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.transferEndNodes());
            }

            aggregationContext.reset();
            aggregationContext.setTime(period.getEndTime());
            aggregationContext.setPeriod(period.getEndTime() - period.getStartTime());

            if (state <= TRANSFER_END_NODES_STATE) {
                if (!transferEndNodes(batch, batchControl, period))
                    return false;

                if (batch != null)
                    batch.setAggregationNodeId(0);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.transformLogs());
            }

            aggregationContext.setDerived(true);

            RootNode root = period.getRootNode();

            if (state <= TRANSFORM_LOGS_STATE) {
                long startNodeId = batch != null ? batch.getAggregationNodeId() : 0;
                for (IAggregationNode node : root.getLogs()) {
                    if (startNodeId != 0 && node.getId() != startNodeId)
                        continue;
                    startNodeId = 0;
                    if (!canContinue(batch, batchControl, TRANSFORM_LOGS_STATE, ((IPeriodNode) node).getId()))
                        return false;

                    if (node instanceof StackLogNode)
                        ((StackLogNode) node).resolveReference();

                    transformLog(node);
                }

                if (batch != null)
                    batch.setAggregationNodeId(0);
            }

            if (state <= RESOLVE_SECONDARY_ENTRYPOINTS_STATE) {
                long startNodeId = batch != null ? batch.getAggregationNodeId() : 0;
                for (ISecondaryEntryPointNode node : root.getSecondaryEntryPoints()) {
                    if (startNodeId != 0 && node.getId() != startNodeId)
                        continue;
                    startNodeId = 0;
                    if (!canContinue(batch, batchControl, RESOLVE_SECONDARY_ENTRYPOINTS_STATE, ((IPeriodNode) node).getId()))
                        return false;

                    ((SecondaryEntryPointNode) node).resolveReferences();
                }

                if (batch != null)
                    batch.setAggregationNodeId(0);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.aggregateDerivedNames());
            }

            if (state <= AGGREGATE_NAMES_STATE) {
                long startNodeId = batch != null ? batch.getAggregationNodeId() : 0;
                for (INameNode node : root.getNameNodes()) {
                    if (startNodeId != 0 && node.getId() != startNodeId)
                        continue;
                    startNodeId = 0;
                    if (!canContinue(batch, batchControl, AGGREGATE_NAMES_STATE, ((IPeriodNode) node).getId()))
                        return false;

                    aggregateDerivedName(node);
                }

                if (batch != null)
                    batch.setAggregationNodeId(0);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.aggregateDerivedBackground());

                hierarchyMap.clear();
            }

            if (state <= AGGREGATE_BACKGROUND_ROOTS_STATE) {
                long startNodeId = 0;
                if (batch != null) {
                    startNodeId = batch.getAggregationNodeId();

                    if (combineType != CombineType.STACK && combineType != CombineType.TRANSACTION) {
                        if (batch.getHierarchyMap() != null)
                            hierarchyMap = batch.getHierarchyMap();
                        else
                            batch.setHierarchyMap(hierarchyMap);
                    }
                }

                for (IBackgroundRootNode node : root.getBackgroundRoots()) {
                    if (startNodeId != 0 && node.getId() != startNodeId)
                        continue;
                    startNodeId = 0;
                    if (!canContinue(batch, batchControl, AGGREGATE_BACKGROUND_ROOTS_STATE, ((IPeriodNode) node).getId()))
                        return false;

                    aggregateDerivedBackground(((BackgroundRootNode) node));
                }

                if (batch != null)
                    batch.setAggregationNodeId(0);

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.aggregateDerivedTransaction());
            }

            if (state <= AGGREGATE_TRANSACTION_ROOTS_STATE) {
                long startNodeId = 0;
                if (batch != null) {
                    startNodeId = batch.getAggregationNodeId();

                    if (combineType != CombineType.STACK && combineType != CombineType.TRANSACTION) {
                        if (batch.getHierarchyMap() != null)
                            hierarchyMap = batch.getHierarchyMap();
                        else
                            batch.setHierarchyMap(hierarchyMap);
                    }
                }

                for (IPrimaryEntryPointNode node : root.getTransactionRoots()) {
                    if (startNodeId != 0 && node.getId() != startNodeId)
                        continue;
                    startNodeId = 0;
                    if (!canContinue(batch, batchControl, AGGREGATE_TRANSACTION_ROOTS_STATE, ((IPeriodNode) node).getId()))
                        return false;
                    aggregateDerivedTransaction(root, ((PrimaryEntryPointNode) node));
                }

                if (batch != null) {
                    batch.setAggregationNodeId(0);
                    batch.setHierarchyMap(null);
                }

                if (logger.isLogEnabled(LogLevel.TRACE))
                    logger.log(LogLevel.TRACE, messages.transferDerived());

                hierarchyMap.clear();
            }

            aggregationContext.setDerived(false);

            if (state <= CLOSE_FIELDS_STATE) {
                if (!closeFields(batch, batchControl, period, root, schemaChange))
                    return false;

                if (batch != null)
                    batch.setAggregationNodeId(0);
            }

            if (state <= TRANSFER_DERIVED_NODES_STATE) {
                if (!transferDerivedNodes(batch, batchControl, root))
                    return false;

                if (batch != null)
                    batch.setAggregationNodeId(0);
            }
        } catch (Throwable e) {
            exception = e;
        }

        if (batch != null) {
            batch.setAggregationState(START_STATE);
            batch.setAggregationNodeId(0);
            batch.setInterceptResult(false);

            closing = false;
        }

        context.getCacheControl().unloadExcessive();

        if (interceptResult)
            AggregatorInterceptor.INSTANCE.onAfterPeriodClosed(((RawDatabase) context.getRawDatabase()).getInterceptId());

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.endClose(period.getSpace().getSchema().getConfiguration().getName()));

        if (exception != null)
            Exceptions.wrapAndThrow(exception);

        return true;
    }

    public void updateNonAggregatingPeriod(Period period) {
        Assert.isTrue(period.getSpace().getSchema().getConfiguration().isNonAggregating());

        ensureSpaceSchema();
        aggregatePendingMeasurements();

        RootNode root = period.getRootNode();
        for (IAggregationNode node : root.getLogs()) {
            if (node instanceof StackLogNode)
                ((StackLogNode) node).resolveReference();
        }

        for (ISecondaryEntryPointNode node : root.getSecondaryEntryPoints())
            ((SecondaryEntryPointNode) node).resolveReferences();

        for (INameNode node : root.getNameNodes()) {
            NameNode nameNode = (NameNode) node;
            if (!nameNode.areReferencesResolved()) {
                aggregateDerivedName(node, null, false);
                nameNode.setReferencesResolved();
            }
        }
    }

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        IParentDomainHandlerFactory factory = Services.loadProvider(IParentDomainHandlerFactory.class);
        if (factory != null)
            parentDomainHandler = factory.createHander(context);
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
        nameManager = null;
    }

    private void aggregatePendingMeasurements() {
        if (!pendingMeasurements.isEmpty()) {
            for (MeasurementSet set : pendingMeasurements)
                aggregateMeasurements(set);

            pendingMeasurements.clear();
        }
    }

    private void aggregateMeasurements(MeasurementSet measurements) {
        boolean interceptResult = AggregatorInterceptor.INSTANCE.onBeforeAggregated(((RawDatabase) context.getRawDatabase()).getInterceptId());

        ensureSpaceSchema();

        aggregationContext.reset();
        aggregationContext.setTime(measurements.getTime());

        boolean firstNonAggregating = spaceSchema.getCycles().get(0).getConfiguration().isNonAggregating();
        int measurementsCount = 0;
        for (Measurement measurement : measurements.getMeasurements()) {
            IAggregationNodeSchema nodeSchema = spaceSchema.findAggregationNode(measurement.getId().getComponentType());
            if (nodeSchema == null)
                continue;

            MeasurementId id = (MeasurementId) measurement.getId();
            Location location = new Location(id.getScopeId(), id.getLocationId());
            ICycleSchema cycleSchema = nodeSchema.getParent();

            if (!cycleSchema.getConfiguration().isNonAggregating()) {
                aggregateNode(null, measurement, nodeSchema, location, measurements.isDerived(), false);

                if (firstNonAggregating && nodeSchema.getParent().getIndex() == 1 && nodeSchema.getPreviousPeriodNode() != null)
                    addToNonAggregating(measurement, nodeSchema.getPreviousPeriodNode(), location);
            } else
                addToNonAggregating(measurement, nodeSchema, location);

            measurementsCount++;
        }

        if (interceptResult)
            AggregatorInterceptor.INSTANCE.onAfterAggregated(((RawDatabase) context.getRawDatabase()).getInterceptId(), measurementsCount);
    }

    private void transformLog(IAggregationNode node) {
        IAggregationNodeSchema schema = node.getSchema();
        List<IAggregationLogTransformer> transformers;
        if (schema instanceof INameNodeSchema) {
            INameNodeSchema nameSchema = (INameNodeSchema) schema;
            if (nameSchema.getLogTransformers() == null || nameSchema.getLogTransformers().isEmpty())
                return;

            transformers = nameSchema.getLogTransformers();
        } else if (schema instanceof IStackLogNodeSchema) {
            IStackLogNodeSchema logSchema = (IStackLogNodeSchema) schema;
            if (logSchema.getLogTransformers() == null || logSchema.getLogTransformers().isEmpty())
                return;

            transformers = logSchema.getLogTransformers();
        } else
            return;

        for (IAggregationLogTransformer transformer : transformers) {
            for (Measurement measurement : transformer.transform(node))
                aggregateNode(node.getPeriod(), measurement);
        }
    }

    private boolean aggregateNode(IPeriod period, Measurement measurement) {
        ICycleSchema cycleSchema = period.getSpace().getSchema();
        IAggregationNodeSchema nodeSchema = cycleSchema.findAggregationNode(measurement.getId().getComponentType());
        if (nodeSchema == null)
            return false;

        MeasurementId id = (MeasurementId) measurement.getId();
        Location location = new Location(id.getScopeId(), id.getLocationId());

        return aggregateNode(null, measurement, nodeSchema, location, false, true);
    }

    private boolean aggregateNode(IAggregationNode fromNode, Measurement measurement, IAggregationNodeSchema nodeSchema, Location location,
                                  boolean derivedAggregationBlocked, boolean aggregateDerived) {
        if (!aggregateDerived && nodeSchema.getFilter() != null && !nodeSchema.getFilter().allow(measurement))
            return false;

        IPeriod period = nodeSchema.getParent().getCurrentCycle().getSpace().getCurrentPeriod();

        aggregationContext.setPeriod(measurement.getPeriod());

        INodeIndex<Location, AggregationNode> index = period.getIndex(nodeSchema.getPrimaryField());
        AggregationNode node = index.find(location);
        if (node != null) {
            IPeriodAggregationField field = node.getField(nodeSchema.getAggregationField());
            boolean derived = aggregationContext.isDerived();
            aggregationContext.setDerived(false);
            field.aggregate(measurement.getValue(), aggregationContext);
            aggregationContext.setDerived(derived);
        } else {
            IComponentValue value = measurement.getValue();
            if (value.getMetadata() == null) {
                IAggregationNode prevNode = getPreviousPeriodNode((Period) period, location, nodeSchema);
                if (prevNode != null) {
                    IJsonField metadataField = prevNode.getField(prevNode.getSchema().getAggregationField().getMetadataFieldIndex());
                    JsonObject metadata = metadataField.get();
                    if (metadata != null)
                        value = new ComponentValue(value.getMetrics(), metadata);
                }
            }

            if (nodeSchema.isMetadataRequired() && value.getMetadata() == null)
                return false;

            node = period.createNode(location, nodeSchema);

            if (derivedAggregationBlocked)
                ((NameNode) node).setDerivedAggregationBlocked();
            if (aggregateDerived)
                node.setDerived();

            IPeriodAggregationField field = node.getField(nodeSchema.getAggregationField());
            boolean derived = aggregationContext.isDerived();
            aggregationContext.setDerived(false);
            field.aggregate(value, aggregationContext);
            aggregationContext.setDerived(derived);

            if (!derivedAggregationBlocked)
                node.init(nameManager, value.getMetadata(), true);
            else if (fromNode != null) {
                INameNode aggregationNode = (INameNode) fromNode;
                INameNode scopeParent = aggregationNode.getScopeParent();
                INameNode metricParent = aggregationNode.getMetricParent();
                ((NameNode) node).initEndDerived(scopeParent != null ? scopeParent.getLocation() : null,
                        metricParent != null ? metricParent.getLocation() : null);
            } else {
                Assert.isTrue(measurement.getNames() != null && measurement.getNames().size() == 4);
                List<NameId> names = measurement.getNames();
                Location scopeParentLocation = null;
                if (names.get(0).getId() != 0)
                    scopeParentLocation = new Location(names.get(0).getId(), names.get(1).getId());

                Location metricParentLocation = null;
                if (names.get(2).getId() != 0)
                    metricParentLocation = new Location(names.get(2).getId(), names.get(3).getId());

                ((NameNode) node).initEndDerived(scopeParentLocation, metricParentLocation);
            }
        }

        return true;
    }

    private void addToNonAggregating(Measurement measurement, IAggregationNodeSchema nodeSchema, Location location) {
        if (nodeSchema.getFilter() != null && !nodeSchema.getFilter().allow(measurement))
            return;

        IPeriod cyclePeriod = nodeSchema.getParent().getCurrentCycle().getSpace().getCyclePeriod();

        INodeIndex<Location, AggregationNode> index = cyclePeriod.getIndex(nodeSchema.getPrimaryField());
        AggregationNode node = index.find(location);
        if (node != null) {
            IAggregationField field = node.getField(nodeSchema.getAggregationField());
            if (field instanceof ILogAggregationField)
                ((ILogAggregationField) field).add(measurement.getValue(), aggregationContext.getTime(), measurement.getPeriod());
            else
                ((IPeriodAggregationField) field).add(measurement.getValue(), aggregationContext.getTime(), measurement.getPeriod());
        } else {
            IComponentValue value = measurement.getValue();
            if (value.getMetadata() == null) {
                IAggregationNode prevNode = getPreviousPeriodNode((Period) cyclePeriod, location, nodeSchema);
                if (prevNode != null) {
                    IJsonField metadataField = prevNode.getField(prevNode.getSchema().getAggregationField().getMetadataFieldIndex());
                    JsonObject metadata = metadataField.get();
                    if (metadata != null)
                        value = new ComponentValue(value.getMetrics(), metadata);
                }
            }

            if (nodeSchema.isMetadataRequired() && value.getMetadata() == null)
                return;

            node = cyclePeriod.createNode(location, nodeSchema);

            IAggregationField field = node.getField(nodeSchema.getAggregationField());
            if (field instanceof ILogAggregationField)
                ((ILogAggregationField) field).add(value, aggregationContext.getTime(), measurement.getPeriod());
            else
                ((IPeriodAggregationField) field).add(value, aggregationContext.getTime(), measurement.getPeriod());

            node.init(nameManager, value.getMetadata(), false);
        }
    }

    private boolean transferEndNodes(ClosePeriodBatchOperation batch, IBatchControl batchControl, Period fromPeriod) {
        String parentDomainName = getParentDomainName((RootNode) fromPeriod.getRootNode());
        List<Measurement> parentDomainMeasurements = null;
        if (parentDomainName != null)
            parentDomainMeasurements = new ArrayList<Measurement>();

        int totalEndNodes = 0;
        int aggregationEndNodes = 0;
        long startNodeId = 0;
        if (batch != null) {
            totalEndNodes = batch.getTotalEndNodes();
            aggregationEndNodes = batch.getAggregationEndNodes();
            startNodeId = batch.getAggregationNodeId();
        }

        for (Object node : fromPeriod.getNodes(startNodeId)) {
            if (!canContinue(batch, batchControl, TRANSFER_END_NODES_STATE, ((IPeriodNode) node).getId())) {
                batch.setTotalEndNodes(totalEndNodes);
                batch.setAggregationEndNodes(aggregationEndNodes);

                handleParentMeasurements(parentDomainName, parentDomainMeasurements, false);
                return false;
            }

            totalEndNodes++;
            if (!(node instanceof IAggregationNode))
                continue;

            aggregationEndNodes++;

            IAggregationNode aggregationNode = (IAggregationNode) node;
            IAggregationNodeSchema nodeSchema = aggregationNode.getSchema();
            if (nodeSchema instanceof INameNodeSchema) {
                INameNodeSchema nameSchema = (INameNodeSchema) nodeSchema;

                if (nodeSchema.getPreviousPeriodNode() == null && nameSchema.hasSumByGroupMetrics())
                    normalizeEndMetric(nodeSchema.getConfiguration().getComponentType(), aggregationNode.getAggregationField().getValue(false));
            }

            if (nodeSchema.getNextPeriodNode() == null)
                continue;

            boolean derivedAggregationBlocked = false;
            if (nodeSchema instanceof INameNodeSchema) {
                INameNodeSchema nameSchema = (INameNodeSchema) nodeSchema;
                if (nameSchema.isAllowTransferDerived())
                    continue;

                derivedAggregationBlocked = ((NameNode) aggregationNode).isDerivedAggregationBlocked();
            }

            transferNode(aggregationNode, derivedAggregationBlocked, parentDomainMeasurements);
        }

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.statEndNodes(totalEndNodes, aggregationEndNodes));

        if (batch != null) {
            batch.setTotalEndNodes(0);
            batch.setAggregationEndNodes(0);
        }

        handleParentMeasurements(parentDomainName, parentDomainMeasurements, false);

        return true;
    }

    private void transferNode(IAggregationNode aggregationNode, boolean derivedAggregationBlocked, List<Measurement> parentDomainMeasurements) {
        IPeriodAggregationField aggregationField = aggregationNode.getField(aggregationNode.getSchema().getAggregationField());
        Location location = aggregationNode.getLocation();
        MeasurementId id = new MeasurementId(location.getScopeId(), location.getMetricId(),
                aggregationNode.getSchema().getConfiguration().getComponentType().getName());

        if (!aggregationNode.getSchema().getAggregationField().isLogMetric()) {
            Measurement measurement = new Measurement(id, aggregationField.getValue(true), aggregationContext.getPeriod(), null);
            boolean aggregated = aggregateNode(aggregationNode, measurement, aggregationNode.getSchema().getNextPeriodNode(),
                    aggregationNode.getLocation(), derivedAggregationBlocked, false);
            if (aggregated && parentDomainMeasurements != null) {
                List<NameId> names = null;
                if (derivedAggregationBlocked)
                    names = getDerivedMeasurementNames(aggregationNode);

                parentDomainMeasurements.add(new Measurement(id, aggregationField.getValue(true), aggregationContext.getPeriod(), names));
            }
        } else {
            long time = aggregationContext.getTime();

            JsonArrayBuilder arrayBuilder = null;
            for (IAggregationRecord record : aggregationField.getPeriodRecords()) {
                Measurement measurement = new Measurement(id, record.getValue(), record.getPeriod(), null);
                aggregationContext.setTime(record.getTime());
                boolean aggregated = aggregateNode(aggregationNode, measurement, aggregationNode.getSchema().getNextPeriodNode(),
                        aggregationNode.getLocation(), derivedAggregationBlocked, false);
                if (aggregated && parentDomainMeasurements != null) {
                    if (arrayBuilder == null)
                        arrayBuilder = new JsonArrayBuilder();

                    arrayBuilder.add(((IObjectValue) record.getValue().getMetrics().get(0)).getObject());
                }
            }

            if (arrayBuilder != null) {
                List<NameId> names = null;
                if (derivedAggregationBlocked)
                    names = getDerivedMeasurementNames(aggregationNode);

                parentDomainMeasurements.add(new Measurement(id, new ComponentValue(Arrays.asList(new ObjectValue(arrayBuilder.toJson())),
                        aggregationField.getLog().getMetadata()), aggregationContext.getPeriod(), names));
            }

            aggregationContext.setTime(time);
        }
    }

    private boolean discoverComponents(ClosePeriodBatchOperation batch, IBatchControl batchControl, Period fromPeriod) {
        ArrayList<DiscoveryInfo> discoveryInfos = new ArrayList<DiscoveryInfo>();
        ArrayList<DeletionInfo> deletionInfos = new ArrayList<DeletionInfo>();

        for (INodeSchema schema : fromPeriod.getSpace().getSchema().getNodes()) {
            if (schema instanceof INameNodeSchema) {
                INameNodeSchema nameSchema = (INameNodeSchema) schema;
                if (nameSchema.getComponentDeletionStrategy() != null)
                    Collections.set(deletionInfos, schema.getIndex(), new DeletionInfo(nameSchema.getComponentDeletionStrategy()));
            }
            if (schema instanceof IStackNodeSchema) {
                IStackNodeSchema stackSchema = (IStackNodeSchema) schema;
                if (stackSchema.getComponentDeletionStrategy() != null)
                    Collections.set(deletionInfos, schema.getIndex(), new DeletionInfo(stackSchema.getComponentDeletionStrategy()));
            }
        }

        long startNodeId = batch != null ? batch.getAggregationNodeId() : 0;
        for (Object node : fromPeriod.getNodes(startNodeId)) {
            if (!canContinue(batch, batchControl, DISCOVER_COMPONENTS_STATE, ((IPeriodNode) node).getId())) {
                processDiscoveryDeletion(discoveryInfos, deletionInfos);
                return false;
            }

            if (!(node instanceof IAggregationNode))
                continue;

            if (node instanceof INameNode)
                discoverName((INameNode) node, discoveryInfos, deletionInfos);
            if (node instanceof IPrimaryEntryPointNode)
                discoverTransaction((IPrimaryEntryPointNode) node, discoveryInfos, deletionInfos);
        }

        processDiscoveryDeletion(discoveryInfos, deletionInfos);

        return true;
    }

    private void processDiscoveryDeletion(ArrayList<DiscoveryInfo> discoveryInfos, ArrayList<DeletionInfo> deletionInfos) {
        for (DiscoveryInfo info : discoveryInfos) {
            if (info != null)
                info.process();
        }

        for (DeletionInfo info : deletionInfos) {
            if (info != null)
                info.process();
        }
    }

    private void discoverName(INameNode node, ArrayList<DiscoveryInfo> discoveryInfos, ArrayList<DeletionInfo> deletionInfos) {
        INameNodeSchema schema = node.getSchema();
        int index = schema.getIndex();

        if (!schema.getComponentDiscoveryStrategies().isEmpty()) {
            DiscoveryInfo info = null;
            if (index < discoveryInfos.size())
                info = discoveryInfos.get(index);

            if (info == null) {
                info = new DiscoveryInfo(schema.getComponentDiscoveryStrategies());
                Collections.set(discoveryInfos, index, info);
            }

            IJsonField metadataField = node.getField(schema.getAggregationField().getMetadataFieldIndex());
            JsonObject metadata = metadataField.get();
            if (metadata != null)
                info.existingComponents.add(new Pair<Long, JsonObject>(node.getLocation().getScopeId(), metadata));
        }

        if (node.getSchema().getComponentDeletionStrategy() != null) {
            DeletionInfo info = deletionInfos.get(index);
            info.existingComponents.add(node.getLocation().getScopeId());
        }
    }

    private void discoverTransaction(IPrimaryEntryPointNode node, ArrayList<DiscoveryInfo> discoveryInfos, ArrayList<DeletionInfo> deletionInfos) {
        IStackNodeSchema schema = node.getSchema();

        int index = schema.getIndex();
        if (!schema.getComponentDiscoveryStrategies().isEmpty()) {
            DiscoveryInfo info = null;
            if (index < discoveryInfos.size())
                info = discoveryInfos.get(index);

            if (info == null) {
                info = new DiscoveryInfo(schema.getComponentDiscoveryStrategies());
                Collections.set(discoveryInfos, index, info);
            }

            IJsonField metadataField = node.getField(schema.getAggregationField().getMetadataFieldIndex());
            JsonObject metadata = metadataField.get();
            if (metadata != null)
                info.existingComponents.add(new Pair<Long, JsonObject>(node.getLocation().getScopeId(), metadata));
        }

        if (node.getSchema().getComponentDeletionStrategy() != null) {
            DeletionInfo info = deletionInfos.get(index);
            info.existingComponents.add(node.getLocation().getScopeId());
        }
    }

    private List<NameId> getDerivedMeasurementNames(IAggregationNode node) {
        if (node instanceof INameNode) {
            INameNode nameNode = (INameNode) node;
            INameNode scopeParent = nameNode.getScopeParent();
            INameNode metricParent = nameNode.getMetricParent();
            List<NameId> names = new ArrayList<NameId>(4);
            if (scopeParent != null) {
                names.add(new NameId(scopeParent.getScope()));
                names.add(new NameId(scopeParent.getMetric()));
            } else {
                names.add(new NameId(Names.rootScope()));
                names.add(new NameId(Names.rootMetric()));
            }

            if (metricParent != null) {
                names.add(new NameId(metricParent.getScope()));
                names.add(new NameId(metricParent.getMetric()));
            } else {
                names.add(new NameId(Names.rootScope()));
                names.add(new NameId(Names.rootMetric()));
            }

            return names;
        } else
            return null;
    }

    private void aggregateDerivedName(INameNode node) {
        aggregateDerivedName(node, null, true);
    }

    private void aggregateDerivedName(IAggregationNode node, TotalAggregationStrategy totalAggregationStrategy, boolean aggregate) {
        IAggregationNodeSchema schema = node.getSchema();

        IScopeName scope = node.getScope();
        IMetricLocation metricLocation = node.getMetric();
        IMetricName metric;
        boolean isStackNode;
        INameNodeSchema derivedSchema;

        if (node instanceof INameNode) {
            metric = (IMetricName) metricLocation;
            isStackNode = false;
            derivedSchema = (INameNodeSchema) schema;
        } else {
            Assert.checkState(node instanceof IStackNode);
            metric = ((ICallPath) metricLocation).getLastSegment();
            isStackNode = true;
            derivedSchema = ((IStackNodeSchema) schema).getStackNameNode();
            if (derivedSchema == null)
                return;
        }

        IAggregationFieldSchema fieldSchema = schema.getAggregationField();
        IAggregationFieldSchema derivedFieldSchema = derivedSchema.getAggregationField();

        IPeriod period = node.getPeriod();

        boolean secondaryTransactionSegmentNode = false;
        if (isStackNode && ((IStackNode) node).getRoot() instanceof ISecondaryEntryPointNode)
            secondaryTransactionSegmentNode = true;

        ArrayList<NameNode> parentScopeNodes = new ArrayList<NameNode>();
        ArrayList<NameNode> scopeNodes = new ArrayList<NameNode>();
        for (IScopeAggregationStrategy scopeAggregationStrategy : derivedSchema.getScopeAggregationStrategies()) {
            ScopeHierarchy scopeHierarchy = scopeAggregationStrategy.getAggregationHierarchy(node);
            for (IMetricAggregationStrategy metricAggregationStrategy : derivedSchema.getMetricAggregationStrategies()) {
                MetricHierarchy metricHierarchy = metricAggregationStrategy.getAggregationHierarchy(metric);

                parentScopeNodes.clear();
                scopeNodes.clear();

                int scopeCount = scopeHierarchy.getScopes().size();

                if (secondaryTransactionSegmentNode)
                    scopeCount--;

                for (int s = 0; s < scopeCount; s++) {
                    IScopeName derivedScope = scopeHierarchy.getScopes().get(s);
                    Assert.isTrue(!derivedScope.isEmpty());
                    IPeriodName name = nameManager.addName(derivedScope);
                    long scopeId = name.getId();

                    NameNode parentMetricNode = null;
                    int metricCount = metricHierarchy.getMetrics().size();
                    for (int m = 0; m < metricCount; m++) {
                        IMetricName derivedMetric = metricHierarchy.getMetrics().get(m);
                        long metricId;
                        if (!derivedMetric.isEmpty()) {
                            name = nameManager.addName(derivedMetric);
                            metricId = name.getId();
                        } else
                            metricId = 0;

                        if (!isStackNode && derivedScope.equals(scope) && derivedMetric.equals(metric)) {
                            NameNode nameNode = (NameNode) node;
                            if (parentMetricNode != null)
                                parentMetricNode.addMetricChild(nameNode);

                            if (s > 0) {
                                NameNode parentScopeNode = parentScopeNodes.get(m);
                                if (parentScopeNode != null)
                                    parentScopeNode.addScopeChild(nameNode);
                            }

                            Collections.set(scopeNodes, m, nameNode);
                            parentMetricNode = nameNode;
                            continue;
                        }

                        if (derivedSchema.getAggregationFilter() != null && derivedSchema.getAggregationFilter().deny(derivedScope, derivedMetric))
                            continue;

                        Location location = new Location(scopeId, metricId);
                        NameNode derivedNode = period.findOrCreateNode(location, derivedSchema);
                        boolean created = !derivedNode.isDerived();
                        derivedNode.setDerived();

                        Collections.set(scopeNodes, m, derivedNode);

                        if (created && parentMetricNode != null)
                            parentMetricNode.addMetricChild(derivedNode);

                        if (created && s > 0) {
                            NameNode parentScopeNode = parentScopeNodes.get(m);
                            if (parentScopeNode != null)
                                parentScopeNode.addScopeChild(derivedNode);
                        }

                        if (totalAggregationStrategy != null)
                            aggregationContext.setAllowTotal(totalAggregationStrategy.allowTotal(derivedScope, derivedMetric));

                        if (isStackNode && m == metricCount - 1)
                            aggregationContext.setAggregateMetadata(true);

                        if (isStackNode && s == scopeCount - 1 && m == metricCount - 1)
                            ((StackNode) node).addDependent((StackNameNode) derivedNode, aggregationContext.isAllowTotal());

                        if (aggregate) {
                            IPeriodAggregationField field = node.getField(fieldSchema);
                            IPeriodAggregationField derivedField = derivedNode.getField(derivedFieldSchema);

                            aggregateDerivedField(derivedSchema, fieldSchema.isLogMetric(), field, derivedField);
                        }

                        aggregationContext.setAggregateMetadata(false);
                        parentMetricNode = derivedNode;
                    }

                    ArrayList<NameNode> nodes = parentScopeNodes;
                    parentScopeNodes = scopeNodes;
                    scopeNodes = nodes;
                    scopeNodes.clear();
                }
            }
        }
    }

    private void aggregateDerivedStack(StackNode root) {
        CombineType combineType = getSchema().getConfiguration().getAggregationSchema().getCombineType();

        IStackNodeSchema schema = root.getSchema();
        IScopeName scope = root.getScope();
        aggregationContext.setAllowTotal(true);
        aggregationContext.setAggregateMetadata(true);

        for (IScopeAggregationStrategy scopeAggregationStrategy : schema.getScopeAggregationStrategies()) {
            ScopeHierarchy scopeHierarchy = scopeAggregationStrategy.getAggregationHierarchy(root);
            if (combineType == CombineType.STACK || combineType == CombineType.TRANSACTION)
                hierarchyMap.clear();

            IStackNode parentScopeNode = null;
            int hierarchySize = scopeHierarchy.getScopes().size();
            boolean mainHierarchy = false;
            if (hierarchySize > 0 && scopeHierarchy.getScopes().get(hierarchySize - 1).equals(scope))
                mainHierarchy = true;

            for (int i = 0; i < hierarchySize; i++) {
                IScopeName derivedScope = scopeHierarchy.getScopes().get(i);
                Assert.isTrue(!derivedScope.isEmpty());

                IStackNode derivedNode;
                if (!derivedScope.equals(scope)) {
                    IPeriodName name = nameManager.addName(derivedScope);
                    derivedNode = aggregateDerivedStackNode(derivedScope, name.getId(), null, root, scopeAggregationStrategy,
                            hierarchyMap, i, hierarchySize, mainHierarchy);
                } else
                    derivedNode = root;

                if (parentScopeNode != null) {
                    if (derivedNode instanceof BackgroundRootNode)
                        ((BackgroundRootNode) parentScopeNode).addScopeChild((BackgroundRootNode) derivedNode);
                    else if (derivedNode instanceof PrimaryEntryPointNode)
                        ((PrimaryEntryPointNode) parentScopeNode).addScopeChild((PrimaryEntryPointNode) derivedNode);
                    else
                        Assert.error();
                }

                parentScopeNode = derivedNode;
            }
        }

        if (combineType == CombineType.STACK || combineType == CombineType.TRANSACTION)
            hierarchyMap.clear();

        aggregationContext.setAggregateMetadata(false);
    }

    private StackNode aggregateDerivedStackNode(IScopeName scopeName, long scopeId, StackNode derivedParent,
                                                StackNode node, IScopeAggregationStrategy scopeAggregationStrategy,
                                                TLongObjectMap<EntryPointHierarchy> hierarchyMap, int hierarchyLevel, int hierarchySize, boolean mainHierarchy) {
        MetricInfo info = getDerivedStackNodeMetricId(node, scopeName, hierarchyMap, hierarchyLevel, hierarchySize,
                scopeAggregationStrategy);

        IStackNodeSchema schema = node.getSchema();
        IAggregationFieldSchema fieldSchema = schema.getAggregationField();

        IPeriod period = node.getPeriod();
        Location location = new Location(scopeId, info.metricId);
        StackNode derivedNode = period.findOrCreateNode(location, schema);
        boolean created = !derivedNode.isDerived();
        derivedNode.setDerived();

        if (created && derivedParent != null)
            derivedParent.addChild(derivedNode);

        if (created && node instanceof IntermediateExitPointNode) {
            if (((IntermediateExitPointNode) node).isSync())
                ((IntermediateExitPointNode) derivedNode).setSync();
        }

        if (node instanceof SecondaryEntryPointNode) {
            SecondaryEntryPointNode entryPoint = (SecondaryEntryPointNode) node;
            SecondaryEntryPointNode derivedEntryPoint = (SecondaryEntryPointNode) derivedNode;

            EntryPointHierarchy hierarchy = hierarchyMap.get(entryPoint.getId());
            Collections.set(hierarchy.nodeHierarchy, hierarchyLevel, derivedEntryPoint);

            if (created) {
                if (entryPoint.isSync())
                    derivedEntryPoint.setSync();

                if (hierarchyLevel > 0) {
                    SecondaryEntryPointNode scopeParentEntryPoint = hierarchy.nodeHierarchy.get(hierarchyLevel - 1);
                    scopeParentEntryPoint.addScopeChild(derivedEntryPoint);
                }
            }

            if (mainHierarchy && hierarchyLevel == hierarchySize - 2)
                derivedEntryPoint.addScopeChild(entryPoint);
        }

        IPeriodAggregationField field = node.getField(fieldSchema);
        IPeriodAggregationField derivedField = derivedNode.getField(fieldSchema);

        aggregateDerivedField(schema, fieldSchema.isLogMetric(), field, derivedField);

        for (IStackNode child : node.getChildren())
            aggregateDerivedStackNode(scopeName, scopeId, derivedNode, (StackNode) child, scopeAggregationStrategy,
                    hierarchyMap, hierarchyLevel, hierarchySize, mainHierarchy);

        if (node instanceof EntryPointNode)
            aggregateDerivedStackLogs(scopeId, node, period, derivedNode);

        CombineType combineType = getSchema().getConfiguration().getAggregationSchema().getCombineType();
        if (node instanceof IntermediateExitPointNode && ((IntermediateExitPointNode) node).getChildEntryPoint() != null &&
                (combineType == CombineType.STACK || combineType == CombineType.TRANSACTION ||
                        scopeAggregationStrategy.allowSecondary(transactionAggregation, ((IntermediateExitPointNode) node).getChildEntryPoint()))) {
            aggregateDerivedTransactionSegment(node, scopeAggregationStrategy, hierarchyMap,
                    hierarchyLevel, hierarchySize, mainHierarchy, info.entryScopeName, derivedNode, info.scopeInner);
        }

        return derivedNode;
    }

    private MetricInfo getDerivedStackNodeMetricId(StackNode node, IScopeName scopeName,
                                                   TLongObjectMap<EntryPointHierarchy> hierarchyMap, int hierarchyLevel, int hierarchySize, IScopeAggregationStrategy scopeAggregationStrategy) {
        if (node instanceof IntermediateExitPointNode && ((IntermediateExitPointNode) node).getChildEntryPoint() != null) {
            IntermediateExitPointNode exitNode = (IntermediateExitPointNode) node;
            ISecondaryEntryPointNode entryNode = exitNode.getChildEntryPoint();
            EntryPointHierarchy hierarchy = hierarchyMap.get(entryNode.getId());
            if (hierarchy == null) {
                hierarchy = new EntryPointHierarchy();
                hierarchy.scopeHierarchy = scopeAggregationStrategy.getAggregationHierarchy(entryNode);
                hierarchyMap.put(entryNode.getId(), hierarchy);

                if (hierarchy.scopeHierarchy.getScopes().size() < hierarchySize) {
                    List<IScopeName> scopes = new ArrayList<IScopeName>(hierarchy.scopeHierarchy.getScopes());
                    IScopeName lastScope;
                    if (!scopes.isEmpty())
                        lastScope = scopes.get(scopes.size() - 1);
                    else
                        lastScope = entryNode.getScope();

                    for (int i = scopes.size(); i < hierarchySize; i++)
                        scopes.add(lastScope);

                    hierarchy.scopeHierarchy = new ScopeHierarchy(scopes);
                }
            }

            ICallPath callPath = node.getMetric();
            IScopeName entryScopeName = hierarchy.scopeHierarchy.getScopes().get(hierarchyLevel);

            IMetricName segment = Names.getMetric(callPath.getLastSegment().toString() + "." + entryScopeName.toString());
            callPath = Names.getCallPath(callPath.getParent(), segment);
            IPeriodName name = nameManager.addName(callPath);

            MetricInfo info = new MetricInfo();
            info.metricId = name.getId();
            info.entryScopeName = entryScopeName;
            info.scopeInner = entryScopeName.equals(scopeName);

            return info;
        } else {
            MetricInfo info = new MetricInfo();
            info.metricId = node.getLocation().getMetricId();
            return info;
        }
    }

    private void aggregateDerivedTransactionSegment(StackNode node,
                                                    IScopeAggregationStrategy scopeAggregationStrategy, TLongObjectMap<EntryPointHierarchy> hierarchyMap,
                                                    int hierarchyLevel, int hierarchySize, boolean mainHierarchy, IScopeName entryScopeName, StackNode derivedNode, boolean scopeInner) {
        IntermediateExitPointNode exitNode = (IntermediateExitPointNode) node;
        IntermediateExitPointNode derivedExitNode = (IntermediateExitPointNode) derivedNode;
        ISecondaryEntryPointNode entryNode = exitNode.getChildEntryPoint();
        EntryPointHierarchy hierarchy = hierarchyMap.get(entryNode.getId());
        SecondaryEntryPointNode derivedEntryNode = Collections.get(hierarchy.nodeHierarchy, hierarchyLevel);
        if (derivedEntryNode == null) {
            derivedEntryNode = (SecondaryEntryPointNode) derivedExitNode.getChildEntryPoint();

            long entryScopeId;
            if (derivedEntryNode != null)
                entryScopeId = derivedEntryNode.getLocation().getScopeId();
            else {
                IScopeName actualScopeName = Names.getScope(entryScopeName.toString() + "." + Numbers.randomUUID());
                IPeriodName name = nameManager.addName(actualScopeName);
                entryScopeId = name.getId();
            }

            derivedEntryNode = (SecondaryEntryPointNode) aggregateDerivedStackNode(entryScopeName, entryScopeId,
                    null, (StackNode) entryNode, scopeAggregationStrategy, hierarchyMap, hierarchyLevel, hierarchySize, mainHierarchy);

            if (scopeInner) {
                derivedEntryNode.setScopeInner();
                derivedExitNode.setScopeInner();
            }
        }

        if (derivedExitNode.getChildEntryPoint() == null)
            derivedExitNode.setChildEntryPoint(derivedEntryNode);
    }

    private void aggregateDerivedStackLogs(long scopeId, StackNode node, IPeriod period, StackNode derivedNode) {
        Iterable<IStackLogNode> logs;
        if (node instanceof EntryPointNode)
            logs = ((EntryPointNode) node).getLogs();
        else
            logs = Assert.error();

        for (IStackLogNode logNode : logs) {
            IStackLogNodeSchema logSchema = logNode.getSchema();

            if (!logSchema.isAllowHierarchyAggregation())
                continue;

            StackLogNode logDerivedNode = period.findOrCreateNode(new Location(scopeId, 0), logSchema);
            logDerivedNode.setDerived();
            if (logDerivedNode.getMainNode() == null) {
                if (derivedNode instanceof EntryPointNode)
                    ((EntryPointNode) derivedNode).addLog(logDerivedNode);
            }

            IAggregationFieldSchema fieldSchema = logSchema.getAggregationField();
            IPeriodAggregationField logField = logNode.getField(fieldSchema);
            IPeriodAggregationField logDerivedField = logDerivedNode.getField(fieldSchema);
            aggregateDerivedField(logSchema, fieldSchema.isLogMetric(), logField, logDerivedField);
        }
    }

    private void aggregateDerivedField(IAggregationNodeSchema derivedNodeSchema,
                                       boolean isLogMetric, IPeriodAggregationField field, IPeriodAggregationField derivedField) {
        if (!isLogMetric) {
            IComponentValue value = field.getValue(false);
            derivedField.aggregate(value, aggregationContext);
        } else {
            IAggregationLogFilter logFilter = null;
            if (derivedNodeSchema instanceof INameNodeSchema)
                logFilter = ((INameNodeSchema) derivedNodeSchema).getLogFilter();
            else if (derivedNodeSchema instanceof IStackLogNodeSchema)
                logFilter = ((IStackLogNodeSchema) derivedNodeSchema).getLogFilter();
            else
                Assert.error();

            long time = aggregationContext.getTime();
            long aggregationPeriod = aggregationContext.getPeriod();

            for (IAggregationRecord record : field.getPeriodRecords()) {
                if (logFilter != null && !logFilter.allow(derivedField, record))
                    continue;

                aggregationContext.setTime(record.getTime());
                aggregationContext.setPeriod(record.getPeriod());
                derivedField.aggregate(record.getValue(), aggregationContext);
            }

            aggregationContext.setTime(time);
            aggregationContext.setPeriod(aggregationPeriod);
        }
    }

    private void aggregateDerivedStackName(StackNode node, TotalAggregationStrategy totalAggregationStrategy, TLongSet entryPointsSet) {
        totalAggregationStrategy.beginLevel();

        if (!isEmptyValue(node))
            aggregateDerivedName(node, totalAggregationStrategy, true);

        for (IStackNode child : node.getChildren())
            aggregateDerivedStackName((StackNode) child, totalAggregationStrategy, entryPointsSet);

        totalAggregationStrategy.endLevel();

        if (node instanceof IntermediateExitPointNode) {
            IntermediateExitPointNode exitPoint = (IntermediateExitPointNode) node;
            if (exitPoint.getChildEntryPoint() == null)
                return;

            SecondaryEntryPointNode entryPoint = (SecondaryEntryPointNode) exitPoint.getChildEntryPoint();
            if (!entryPointsSet.contains(entryPoint.getId())) {
                entryPointsSet.add(entryPoint.getId());

                if (!exitPoint.isSync())
                    totalAggregationStrategy.beginStack();

                aggregateDerivedStackName(entryPoint, totalAggregationStrategy, entryPointsSet);

                if (!exitPoint.isSync())
                    totalAggregationStrategy.endStack();
            }
        }
    }

    private void aggregateDerivedBackground(BackgroundRootNode root) {
        IStackNodeSchema schema = root.getSchema();
        transactionAggregation = false;
        if (schema.isAllowHierarchyAggregation())
            aggregateDerivedStack(root);

        if (schema.isAllowStackNameAggregation()) {
            TotalAggregationStrategy totalAggregationStrategy = new TotalAggregationStrategy();
            totalAggregationStrategy.beginStack();

            TLongSet entryPointsSet = new TLongHashSet(10, 0.5f, Long.MAX_VALUE);
            aggregateDerivedStackName(root, totalAggregationStrategy, entryPointsSet);

            totalAggregationStrategy.endStack();
        }
    }

    private void aggregateDerivedTransaction(RootNode root, PrimaryEntryPointNode transactionRoot) {
        IStackNodeSchema schema = transactionRoot.getSchema();
        transactionAggregation = true;
        if (schema.isAllowHierarchyAggregation())
            aggregateDerivedStack(transactionRoot);

        if (schema.isAllowStackNameAggregation()) {
            TotalAggregationStrategy totalAggregationStrategy = new TotalAggregationStrategy();
            totalAggregationStrategy.beginStack();

            TLongSet entryPointsSet = new TLongHashSet(10, 0.5f, Long.MAX_VALUE);
            aggregateDerivedStackName(transactionRoot, totalAggregationStrategy, entryPointsSet);

            totalAggregationStrategy.endStack();
        }

        for (IStackLogNode log : transactionRoot.getLogs()) {
            if (log.getSchema().isTransactionFailureErrorLog())
                aggregateDerivedTransactionFailures(root, transactionRoot, (StackErrorLogNode) log);
        }
    }

    private void aggregateDerivedTransactionFailures(RootNode root, PrimaryEntryPointNode transactionRoot, StackErrorLogNode failureLog) {
        TLongObjectMap<TransactionFailure> transactionFailureDependencies;
        if (transactionRoot.getSchema().isAllowTransactionFailureDependenciesAggregation())
            transactionFailureDependencies = new TLongObjectHashMap<TransactionFailure>();
        else
            transactionFailureDependencies = null;

        String componentType = transactionRoot.getSchema().getConfiguration().getComponentType().getName();

        IPeriodAggregationField field = failureLog.getField(failureLog.getSchema().getAggregationField());
        TLongSet failedTransactions = new TLongHashSet();
        JsonObject entryRequest = transactionRoot.getMetadata();

        for (IAggregationRecord record : field.getPeriodRecords()) {
            IObjectValue value = (IObjectValue) record.getValue().getMetrics().get(0);
            JsonObject object = (JsonObject) value.getObject();
            Long transactionId = object.get("transactionId", null);
            if (transactionId == null)
                continue;

            failedTransactions.add(transactionId);

            if (transactionFailureDependencies != null)
                addTransactionFailureDependency(transactionFailureDependencies, transactionRoot.getScope(),
                        transactionRoot.getLocation().getScopeId(), transactionRoot.getSchema().getConfiguration().getComponentType().getName(),
                        transactionId, record, entryRequest);
        }

        IPeriod period = transactionRoot.getPeriod();
        Set<TransactionFailureId> transactionFailures = new HashSet<TransactionFailureId>();
        IComponentValue value = new ComponentValue(java.util.Collections.singletonList(
                new NameValue(java.util.Collections.singletonList(new StandardValue(1, 1, 1, 1)))), null);

        for (IAggregationNode logNode : root.getLogs()) {
            if (!(logNode instanceof StackErrorLogNode))
                continue;

            StackErrorLogNode errorLog = (StackErrorLogNode) logNode;
            IStackLogNodeSchema errorLogSchema = errorLog.getSchema();
            if (!errorLogSchema.isAllowTransactionFailureAggregation())
                continue;

            IStackNode mainNode = errorLog.getMainNode();
            if (!(mainNode instanceof IEntryPointNode))
                continue;

            entryRequest = mainNode.getMetadata();

            INameNodeSchema failureSchema = errorLogSchema.getTransactionFailureNode();
            IPeriodAggregationField logField = errorLog.getField(errorLogSchema.getAggregationField());
            for (IAggregationRecord record : logField.getPeriodRecords()) {
                IObjectValue logValue = (IObjectValue) record.getValue().getMetrics().get(0);
                JsonObject object = (JsonObject) logValue.getObject();
                Long transactionId = object.get("transactionId", null);
                if (transactionId == null || !failedTransactions.contains(transactionId))
                    continue;

                String errorType = ErrorLogTransformer.getErrorType(object, componentType);
                if (errorLogSchema.getTransactionFailureFilter() != null && !errorLogSchema.getTransactionFailureFilter().match(errorType))
                    continue;

                String metricName = ErrorLogTransformer.getErrorMetricName(object, errorLogSchema.getStackTraceFilter());
                if (metricName != null) {
                    IMetricName metric = Names.getMetric(metricName);
                    aggregateDerivedTransactionFailure(period, failureSchema, transactionRoot, metric, transactionId, transactionFailures, value);
                }

                if (transactionFailureDependencies != null && errorLog != failureLog)
                    addTransactionFailureDependency(transactionFailureDependencies, errorLog.getScope(),
                            errorLog.getLocation().getScopeId(), errorLog.getSchema().getConfiguration().getComponentType().getName(),
                            transactionId, record, entryRequest);
            }
        }

        if (transactionFailureDependencies != null) {
            long time = aggregationContext.getTime();
            IStackLogNode failuresNode = transactionRoot.getTransactionFailures();
            IPeriodAggregationField failuresField = failuresNode.getField(failuresNode.getSchema().getAggregationField());
            for (TLongObjectIterator<TransactionFailure> it = transactionFailureDependencies.iterator(); it.hasNext(); ) {
                it.advance();
                TransactionFailure failure = it.value();
                aggregationContext.setTime(failure.time);
                JsonObject jsonFailure = Json.object()
                        .put("scopeId", failure.scopeId)
                        .put("failure", failure.failure)
                        .put("dependencies", failure.dependencies).toObject();

                IComponentValue failures = new ComponentValue(java.util.Collections.singletonList(new ObjectValue(jsonFailure)), null);
                failuresField.aggregate(failures, aggregationContext);
            }

            aggregationContext.setTime(time);
        }
    }

    private void addTransactionFailureDependency(TLongObjectMap<TransactionFailure> transactionFailureDependencies,
                                                 IScopeName scope, long scopeId, String componentType, long transactionId, IAggregationRecord record, JsonObject entryRequest) {
        JsonObject object = (JsonObject) ((IObjectValue) record.getValue().getMetrics().get(0)).getObject();
        TransactionFailure failure = transactionFailureDependencies.get(transactionId);
        if (failure == null) {
            failure = new TransactionFailure(scopeId, object, record.getTime());
            transactionFailureDependencies.put(transactionId, failure);
        }

        JsonObjectBuilder builder = new JsonObjectBuilder(object);
        builder.put("scope", scope);
        builder.put("componentType", componentType);
        builder.put("entryRequest", entryRequest);

        failure.dependencies.add(builder);
    }

    private void aggregateDerivedTransactionFailure(IPeriod period, INameNodeSchema schema, IPrimaryEntryPointNode transactionRoot,
                                                    IMetricName metric, long transactionId, Set<TransactionFailureId> transactionFailures, IComponentValue value) {
        IAggregationFieldSchema fieldSchema = schema.getAggregationField();

        ArrayList<NameNode> parentScopeNodes = new ArrayList<NameNode>();
        ArrayList<NameNode> scopeNodes = new ArrayList<NameNode>();
        for (IScopeAggregationStrategy scopeAggregationStrategy : schema.getScopeAggregationStrategies()) {
            ScopeHierarchy scopeHierarchy = scopeAggregationStrategy.getAggregationHierarchy(transactionRoot);
            for (IMetricAggregationStrategy metricAggregationStrategy : schema.getMetricAggregationStrategies()) {
                MetricHierarchy metricHierarchy = metricAggregationStrategy.getAggregationHierarchy(metric);

                parentScopeNodes.clear();

                int scopeCount = scopeHierarchy.getScopes().size();
                for (int s = 0; s < scopeCount; s++) {
                    IScopeName derivedScope = scopeHierarchy.getScopes().get(s);
                    Assert.isTrue(!derivedScope.isEmpty());
                    IPeriodName name = nameManager.addName(derivedScope);
                    long scopeId = name.getId();

                    NameNode parentMetricNode = null;
                    int metricCount = metricHierarchy.getMetrics().size();
                    for (int m = 0; m < metricCount; m++) {
                        IMetricName derivedMetric = metricHierarchy.getMetrics().get(m);
                        long metricId;
                        if (!derivedMetric.isEmpty()) {
                            name = nameManager.addName(derivedMetric);
                            metricId = name.getId();
                        } else
                            metricId = 0;

                        if (schema.getAggregationFilter() != null && schema.getAggregationFilter().deny(derivedScope, derivedMetric))
                            continue;

                        Location location = new Location(scopeId, metricId);

                        TransactionFailureId failure = new TransactionFailureId(transactionId, scopeId, metricId);
                        if (transactionFailures.contains(failure)) {
                            parentMetricNode = period.findNode(location, schema);
                            continue;
                        }

                        transactionFailures.add(failure);

                        NameNode derivedNode = period.findOrCreateNode(location, schema);
                        boolean created = !derivedNode.isDerived();
                        derivedNode.setDerived();

                        Collections.set(scopeNodes, m, derivedNode);

                        if (created && parentMetricNode != null)
                            parentMetricNode.addMetricChild(derivedNode);

                        if (created && s > 0) {
                            NameNode parentScopeNode = parentScopeNodes.get(m);
                            if (parentScopeNode != null)
                                parentScopeNode.addScopeChild(derivedNode);
                        }

                        IPeriodAggregationField derivedField = derivedNode.getField(fieldSchema);
                        derivedField.aggregate(value, aggregationContext);

                        parentMetricNode = derivedNode;
                    }

                    ArrayList<NameNode> nodes = parentScopeNodes;
                    parentScopeNodes = scopeNodes;
                    scopeNodes = nodes;
                    scopeNodes.clear();
                }
            }
        }
    }

    private void updateDerivedRoot(RootNode root, IAggregationNode node) {
        if (node instanceof NameNode) {
            NameNode nameNode = (NameNode) node;
            if (nameNode.getScopeParent() == null && nameNode.getMetricParent() == null)
                root.addDerivedRoot(node);
        } else if (node instanceof BackgroundRootNode) {
            BackgroundRootNode backgroundRootNode = (BackgroundRootNode) node;
            if (backgroundRootNode.getScopeParent() == null)
                root.addDerivedRoot(node);
        } else if (node instanceof PrimaryEntryPointNode) {
            PrimaryEntryPointNode primaryEntryPointNode = (PrimaryEntryPointNode) node;
            if (primaryEntryPointNode.getScopeParent() == null)
                root.addDerivedRoot(node);
        }
    }

    private boolean closeFields(ClosePeriodBatchOperation batch, IBatchControl batchControl, Period period, RootNode root, boolean schemaChange) {
        IRuleService ruleService = context.getTransactionProvider().getTransaction().findDomainService(IRuleService.NAME);
        RuleContext ruleContext;
        if (batch != null && batch.getRuleContext() != null)
            ruleContext = batch.getRuleContext();
        else
            ruleContext = new RuleContext();

        int totalNodes = 0;
        int aggregationNodes = 0;
        int endNodes = 0;
        int derivedNodes = 0;
        long startNodeId = 0;
        if (batch != null) {
            totalNodes = batch.getTotalNodes();
            aggregationNodes = batch.getAggregationNodes();
            endNodes = batch.getEndNodes();
            derivedNodes = batch.getDerivedNodes();
            startNodeId = batch.getAggregationNodeId();
        }

        for (Object node : period.getNodes(startNodeId)) {
            if (!canContinue(batch, batchControl, CLOSE_FIELDS_STATE, ((IPeriodNode) node).getId())) {
                batch.setTotalNodes(totalNodes);
                batch.setAggregationNodes(aggregationNodes);
                batch.setEndNodes(endNodes);
                batch.setDerivedNodes(derivedNodes);
                batch.setRuleContext(ruleContext);
                return false;
            }

            totalNodes++;
            if (!(node instanceof IAggregationNode))
                continue;

            aggregationNodes++;

            IAggregationNode aggregationNode = (IAggregationNode) node;
            IAggregationNodeSchema nodeSchema = aggregationNode.getSchema();

            if (aggregationNode.isDerived()) {
                derivedNodes++;
                updateDerivedRoot(root, aggregationNode);

                if (nodeSchema instanceof INameNodeSchema && nodeSchema.getPreviousPeriodNode() == null &&
                        ((INameNodeSchema) nodeSchema).hasSumByGroupMetrics())
                    normalizeDerivedMetric(nodeSchema.getConfiguration().getComponentType(), aggregationNode.getAggregationField().getValue(false));
            } else
                endNodes++;

            IPeriodAggregationField aggregationField = aggregationNode.getField(nodeSchema.getAggregationField());
            List<Measurement> measurements = aggregationField.onPeriodClosed(period);
            if (measurements != null) {
                for (Measurement measurement : measurements)
                    aggregateNode(period, measurement);
            }

            if (!nodeSchema.getAnalyzers().isEmpty()) {
                JsonObjectBuilder builder = new JsonObjectBuilder();
                for (IAggregationAnalyzer analyzer : nodeSchema.getAnalyzers())
                    analyzer.analyze(aggregationNode, builder);

                if (!builder.isEmpty()) {
                    IJsonField analysisField = aggregationNode.getField(aggregationField.getSchema().getAnalysisFieldIndex());
                    analysisField.set(builder.toJson());
                }
            }

            if (!schemaChange && !nodeSchema.getComponentBindingStrategies().isEmpty()) {
                for (IComponentBindingStrategy strategy : nodeSchema.getComponentBindingStrategies()) {
                    IScopeName scopeName = strategy.getComponentScope(aggregationNode);
                    if (scopeName == null)
                        continue;

                    IPeriodName name = nameManager.findByName(scopeName);
                    if (name == null)
                        continue;

                    IRuleExecutor ruleExecutor = ruleService.findRuleExecutor(name.getId());
                    if (ruleExecutor != null)
                        ruleExecutor.executeSimpleRules(aggregationNode, ruleContext);
                }
            }
        }

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.statNodes(totalNodes, aggregationNodes, endNodes, derivedNodes));

        if (batch != null) {
            batch.setTotalNodes(0);
            batch.setAggregationNodes(0);
            batch.setEndNodes(0);
            batch.setDerivedNodes(0);
            batch.setRuleContext(null);
        }

        if (!schemaChange && ruleContext.getExecutors() != null) {
            ruleContext.getExecutors().forEachValue(new TObjectProcedure<RuleExecutorInfo>() {
                @Override
                public boolean execute(RuleExecutorInfo info) {
                    info.getExecutor().executeComplexRules(info.getFacts());
                    return true;
                }
            });
        }

        return true;
    }

    private boolean transferDerivedNodes(ClosePeriodBatchOperation batch, IBatchControl batchControl, RootNode root) {
        String parentDomainName = getParentDomainName(root);
        List<Measurement> parentDomainMeasurements = null;
        if (parentDomainName != null)
            parentDomainMeasurements = new ArrayList<Measurement>();

        long startNodeId = batch != null ? batch.getAggregationNodeId() : 0;
        for (IAggregationNode node : root.getDerivedRoots()) {
            if (startNodeId != 0 && node.getId() != startNodeId)
                continue;
            startNodeId = 0;
            if (!canContinue(batch, batchControl, TRANSFER_DERIVED_NODES_STATE, ((IPeriodNode) node).getId())) {
                handleParentMeasurements(parentDomainName, parentDomainMeasurements, true);
                return false;
            }

            IAggregationNodeSchema nodeSchema = node.getSchema();
            if ((nodeSchema instanceof INameNodeSchema) &&
                    ((INameNodeSchema) nodeSchema).isAllowTransferDerived() && nodeSchema.getNextPeriodNode() != null)
                transferDerivedNode((INameNode) node, parentDomainMeasurements);
        }

        handleParentMeasurements(parentDomainName, parentDomainMeasurements, true);

        return true;
    }

    private void transferDerivedNode(INameNode node, List<Measurement> parentDomainMeasurements) {
        transferNode(node, true, parentDomainMeasurements);

        for (INameNode metricChild : node.getMetricChildren())
            transferDerivedNode(metricChild, parentDomainMeasurements);

        if (node.getMetricParent() == null) {
            for (INameNode scopeChild : node.getScopeChildren())
                transferDerivedNode(scopeChild, parentDomainMeasurements);
        }
    }

    private void normalizeEndMetric(AggregationComponentTypeSchemaConfiguration configuration, IComponentValue value) {
        for (int i = 0; i < value.getMetrics().size(); i++) {
            MetricTypeSchemaConfiguration metricType = configuration.getMetricTypes().get(i);
            if (!(metricType instanceof GaugeSchemaConfiguration) || !((GaugeSchemaConfiguration) metricType).isSumByGroup())
                continue;

            INameValue metric = (INameValue) value.getMetrics().get(i);
            IStandardValue std = (IStandardValue) metric.getFields().get(0);
            if (std.getCount() > 0) {
                for (int k = 0; k < metric.getFields().size(); k++) {
                    IFieldValueBuilder field = (IFieldValueBuilder) metric.getFields().get(k);
                    field.normalizeEnd(std.getCount());
                }
            }
        }
    }

    private void normalizeDerivedMetric(AggregationComponentTypeSchemaConfiguration configuration, IComponentValue value) {
        for (int i = 0; i < value.getMetrics().size(); i++) {
            MetricTypeSchemaConfiguration metricType = configuration.getMetricTypes().get(i);
            if (!(metricType instanceof GaugeSchemaConfiguration) || !((GaugeSchemaConfiguration) metricType).isSumByGroup())
                continue;

            NameValueSchemaConfiguration metricValueConfiguration = (NameValueSchemaConfiguration) metricType.getFields();
            INameValue metric = (INameValue) value.getMetrics().get(i);
            IStandardValue std = (IStandardValue) metric.getFields().get(0);
            if (std.getCount() > 0) {
                for (int k = 0; k < metric.getFields().size(); k++) {
                    IFieldValueBuilder field = (IFieldValueBuilder) metric.getFields().get(k);
                    field.normalizeDerived(metricValueConfiguration.getFields().get(k), std.getSum());
                }
            }
        }
    }

    private IAggregationNode getPreviousPeriodNode(Period period, Location location, IAggregationNodeSchema schema) {
        int periodIndex = period.getPeriodIndex();
        if (periodIndex > 0) {
            period = period.getSpace().getPeriod(periodIndex - 1);
            return period.findNode(location, schema);
        } else {
            PeriodCycle cycle = period.getSpace().getPreviousCycle();
            if (cycle == null)
                return null;

            PeriodSpace space = cycle.getSpace();
            if (space == null)
                return null;

            schema = (IAggregationNodeSchema) space.getSchema().findNode(schema.getConfiguration().getName());
            if (schema == null)
                return null;

            period = null;
            if (periodIndex == CyclePeriod.PERIOD_INDEX)
                period = space.getPeriod(periodIndex);
            else if (space.getPeriodsCount() > 0)
                period = space.getPeriod(space.getPeriodsCount() - 1);

            if (period != null)
                return period.findNode(location, schema);
        }

        return null;
    }

    private String getParentDomainName(RootNode root) {
        if (parentDomainHandler == null)
            return null;

        IPeriodNodeSchema nextSchema = root.getSchema().getNextPeriodNode();
        if (nextSchema == null)
            return null;

        return nextSchema.getParent().getConfiguration().getParentDomain();
    }

    private void handleParentMeasurements(String parentDomainName, List<Measurement> parentDomainMeasurements, boolean derived) {
        if (!Collections.isEmpty(parentDomainMeasurements)) {
            int schemaVersion = getSchema().getConfiguration().getAggregationSchema().getVersion();
            MeasurementSet measurements = new MeasurementSet(parentDomainMeasurements, parentDomainName, schemaVersion,
                    aggregationContext.getTime(), derived ? MeasurementSet.DERIVED_FLAG : 0);
            parentDomainHandler.handle(measurements);
        }
    }

    private boolean canContinue(ClosePeriodBatchOperation batch, IBatchControl batchControl, int state, long id) {
        if (batch == null)
            return true;

        batch.setAggregationState(state);
        batch.setAggregationNodeId(id);

        return batchControl.canContinue();
    }

    private void ensureSpaceSchema() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("aggregation");
            Assert.notNull(spaceSchema);

            nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
            Assert.notNull(nameManager);
        }
    }

    private boolean isEmptyValue(StackNode node) {
        IComponentValue value = node.getAggregationField().getValue(false);
        IStackValue stackValue = (IStackValue) value.getMetrics().get(0);
        IStandardValue standardValue = (IStandardValue) stackValue.getInherentFields().get(0);
        return standardValue.getCount() == 0;
    }

    private void fireOnBeforePeriodClosed(IPeriod period) {
        for (IPeriodClosureListener listener : listeners) {
            try {
                listener.onBeforePeriodClosed(period);
            } catch (ThreadInterruptedException e) {
                throw e;
            } catch (Exception e) {
                Exceptions.checkInterrupted(e);

                // Isolate exception from other listeners
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, e);
            }
        }
    }

    private static class TransactionFailureId {
        private final long transactionId;
        private final long scopeId;
        private final long locationId;
        private final int hashCode;

        public TransactionFailureId(long transactionId, long scopeId, long locationId) {
            this.transactionId = transactionId;
            this.scopeId = scopeId;
            this.locationId = locationId;
            this.hashCode = 31 * (31 * hashCode(transactionId) + hashCode(scopeId)) + hashCode(locationId);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TransactionFailureId))
                return false;

            TransactionFailureId failure = (TransactionFailureId) o;
            return transactionId == failure.transactionId && scopeId == failure.scopeId && locationId == failure.locationId;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        private static int hashCode(long value) {
            return (int) (value ^ (value >>> 32));
        }
    }

    private static class TransactionFailure {
        private final JsonObject failure;
        private final long scopeId;
        private final long time;
        private final JsonArrayBuilder dependencies = new JsonArrayBuilder();

        public TransactionFailure(long scopeId, JsonObject failure, long time) {
            this.scopeId = scopeId;
            this.failure = failure;
            this.time = time;
        }
    }

    private static class MetricInfo {
        private long metricId;
        private IScopeName entryScopeName;
        private boolean scopeInner;
    }

    public static class EntryPointHierarchy {
        public ScopeHierarchy scopeHierarchy;
        public ArrayList<SecondaryEntryPointNode> nodeHierarchy = new ArrayList<SecondaryEntryPointNode>();
    }

    private static class DiscoveryInfo {
        private final List<IComponentDiscoveryStrategy> strategies;
        private final List<Pair<Long, JsonObject>> existingComponents = new ArrayList<Pair<Long, JsonObject>>();

        public DiscoveryInfo(List<IComponentDiscoveryStrategy> strategies) {
            this.strategies = strategies;
        }

        public void process() {
            for (IComponentDiscoveryStrategy strategy : strategies)
                strategy.processDiscovered(existingComponents);
        }
    }

    private static class DeletionInfo {
        private final IComponentDeletionStrategy strategy;
        private final Set<Long> existingComponents = new HashSet<Long>();

        public DeletionInfo(IComponentDeletionStrategy strategy) {
            this.strategy = strategy;
        }

        public void process() {
            strategy.processDeleted(existingComponents);
        }
    }

    private interface IMessages {
        @DefaultMessage("Begin close ''{0}''.")
        ILocalizedMessage beginClose(String name);

        @DefaultMessage("Total nodes: {0}, aggregation nodes: {1}, end nodes: {2}, derived nodes: {3}.")
        ILocalizedMessage statNodes(int totalNodes, int aggregationNodes, int endNodes, int derivedNodes);

        @DefaultMessage("Total end nodes: {0}, aggregation end nodes: {1}.")
        ILocalizedMessage statEndNodes(int totalEndNodes, int aggregationEndNodes);

        @DefaultMessage("Transferring end nodes...")
        ILocalizedMessage transferEndNodes();

        @DefaultMessage("Transforming logs...")
        ILocalizedMessage transformLogs();

        @DefaultMessage("Aggregating derived names...")
        ILocalizedMessage aggregateDerivedNames();

        @DefaultMessage("Aggregating derived background stacks...")
        ILocalizedMessage aggregateDerivedBackground();

        @DefaultMessage("Aggregating derived transaction stacks...")
        ILocalizedMessage aggregateDerivedTransaction();

        @DefaultMessage("Transferring derived nodes...")
        ILocalizedMessage transferDerived();

        @DefaultMessage("End close ''{0}''.")
        ILocalizedMessage endClose(String name);
    }
}
