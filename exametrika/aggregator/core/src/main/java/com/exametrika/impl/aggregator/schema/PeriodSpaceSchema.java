/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.ClosePeriodBatchOperation;
import com.exametrika.impl.aggregator.PeriodCycle;
import com.exametrika.impl.aggregator.PeriodSpaces;
import com.exametrika.impl.exadb.core.schema.SchemaObject;
import com.exametrika.spi.aggregator.IMeasurementRequestor;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link PeriodSpaceSchema} represents a schema of periodic node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSpaceSchema extends SchemaObject implements IPeriodSpaceSchema, ISpaceSchemaControl {
    private static final ILogger logger = Loggers.get(PeriodSpaceSchema.class);
    private static final IMessages messages = Messages.get(IMessages.class);
    private final PeriodSpaceSchemaConfiguration configuration;
    private final IDatabaseContext context;
    private final int version;
    private final List<ICycleSchema> cycles;
    private final Map<String, ICycleSchema> cyclesMap;
    private final Map<String, ICycleSchema> cyclesByAliasMap;
    private final Map<String, IAggregationNodeSchema> aggregationNodes;
    private final ICycleSchema firstStackCycle;
    private IDomainSchema parent;
    private boolean structuredChange;
    private boolean reconcileBlocked;
    private boolean requestBlocked;

    public PeriodSpaceSchema(IDatabaseContext context, PeriodSpaceSchemaConfiguration configuration, int version,
                             List<ICycleSchema> cycles) {
        super(TYPE);

        Assert.notNull(configuration);
        Assert.notNull(context);
        Assert.notNull(cycles);

        Map<String, ICycleSchema> cyclesMap = new HashMap<String, ICycleSchema>();
        Map<String, ICycleSchema> cyclesByAliasMap = new HashMap<String, ICycleSchema>();
        ICycleSchema firstStackCycle = null;
        for (ICycleSchema cycle : cycles) {
            Assert.isNull(cyclesMap.put(cycle.getConfiguration().getName(), cycle));
            Assert.isNull(cyclesByAliasMap.put(cycle.getConfiguration().getAlias(), cycle));

            if (firstStackCycle == null && cycle.findAggregationNode("app.stack") != null)
                firstStackCycle = cycle;
        }

        this.configuration = configuration;
        this.cycles = Immutables.wrap(cycles);
        this.cyclesMap = cyclesMap;
        this.cyclesByAliasMap = cyclesByAliasMap;
        this.context = context;
        this.version = version;
        this.firstStackCycle = firstStackCycle;

        boolean firstNonAggregating = cycles.get(0).getConfiguration().isNonAggregating();
        Map<String, IAggregationNodeSchema> aggregationNodes = new LinkedHashMap<String, IAggregationNodeSchema>();
        for (int i = 0; i < cycles.size(); i++) {
            ICycleSchema cycle = cycles.get(i);
            for (INodeSchema node : cycle.getNodes()) {
                if (!(node instanceof IAggregationNodeSchema))
                    continue;

                IAggregationNodeSchema aggregationNode = (IAggregationNodeSchema) node;
                if (aggregationNode.getConfiguration().isDerived())
                    continue;

                if ((i == 1 && firstNonAggregating) || !aggregationNodes.containsKey(aggregationNode.getConfiguration().getComponentType().getName()))
                    aggregationNodes.put(aggregationNode.getConfiguration().getComponentType().getName(), aggregationNode);
            }
        }

        this.aggregationNodes = aggregationNodes;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    @Override
    public void setParent(IDomainSchema domain, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(domain);

        this.parent = domain;

        super.setParent(domain, schemaObjects);

        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).setParent(this, schemaObjects);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).resolveDependencies();
    }

    @Override
    public PeriodSpaceSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public IDomainSchema getParent() {
        return parent;
    }

    @Override
    public final int getVersion() {
        return version;
    }

    @Override
    public boolean isCompatible(ISpaceSchema schema, IDataMigrator dataMigrator) {
        return true;
    }

    @Override
    public void read(RawPageDeserialization deserialization) {
        for (int j = 0; j < cycles.size(); j++) {
            CycleSchema cycle = (CycleSchema) cycles.get(j);
            cycle.setDataFileOffset(deserialization.getFileOffset());
            deserialization.readLong();
        }
    }

    @Override
    public void write(RawPageSerialization serialization) {
        for (int j = 0; j < cycles.size(); j++) {
            CycleSchema cycleSchema = (CycleSchema) cycles.get(j);
            cycleSchema.setDataFileOffset(serialization.getFileOffset());
            serialization.writeLong(0);
        }
    }

    @Override
    public List<String> beginSnapshot() {
        List<String> files = new ArrayList<String>();
        for (ICycleSchema c : cycles) {
            CycleSchema cycleSchema = (CycleSchema) c;
            PeriodCycle cycle = cycleSchema.getCurrentCycle();
            files.addAll(cycle.beginSnapshot());
            files.addAll(PeriodSpaces.getPeriodSpaceFileNames(
                    context.getConfiguration().getPaths(), cycle.getSchema().getParent().getConfiguration().getPathIndex(),
                    getParent().getConfiguration().getName(),
                    cycle.getSchema().getParent().getConfiguration(), cycle.getSchema().getConfiguration(),
                    cycle.getDataFileIndex(), cycle.getCycleSpaceFileIndex(), cycle.getForecastSpaceFileIndex(),
                    cycle.getAnomalyDetectorSpaceFileIndex(), cycle.getFastAnomalyDetectorSpaceFileIndex()));
        }

        return files;
    }

    @Override
    public void endSnapshot() {
        for (ICycleSchema c : cycles) {
            CycleSchema cycleSchema = (CycleSchema) c;
            PeriodCycle cycle = cycleSchema.getCurrentCycle();
            cycle.endSnapshot();
        }
    }

    @Override
    public List<ICycleSchema> getCycles() {
        return cycles;
    }

    @Override
    public final ICycleSchema findCycle(String periodName) {
        Assert.notNull(periodName);

        return cyclesMap.get(periodName);
    }

    @Override
    public final ICycleSchema findCycleByAlias(String periodAlias) {
        Assert.notNull(periodAlias);

        return cyclesByAliasMap.get(periodAlias);
    }

    @Override
    public void onTransactionStarted() {
        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).onTransactionStarted();
    }

    @Override
    public void onTransactionCommitted() {
        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).onTransactionCommitted();
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        boolean res = false;
        for (ICycleSchema cycle : cycles)
            res = ((CycleSchema) cycle).onBeforeTransactionRolledBack() || res;

        return res;
    }

    @Override
    public void onTransactionRolledBack() {
        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).onTransactionRolledBack();
    }

    @Override
    public void clearCaches() {
        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).clearCache();
    }

    @Override
    public void onTimer(long currentTime) {
        if (cycles.get(0).getConfiguration().isNonAggregating())
            ((CycleSchema) cycles.get(0)).updateNonAggregatingPeriod(currentTime);

        if (reconcileBlocked)
            return;

        if (!requestBlocked && firstStackCycle != null && ((CycleSchema) firstStackCycle).isPeriodReadyToClose(currentTime)) {
            IMeasurementRequestor requestor = context.getDatabase().findParameter(IMeasurementRequestor.NAME);
            if (requestor != null) {
                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.reconcileBlocked(firstStackCycle.getConfiguration().getName()));

                reconcileBlocked = true;
                requestor.requestMeasurements(new CompletionHandler() {
                    @Override
                    public void onSucceeded(Object result) {
                        requestCompleted();
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        requestCompleted();
                    }

                    private void requestCompleted() {
                        context.getDatabase().transaction(new Operation() {
                            @Override
                            public void run(ITransaction transaction) {
                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, messages.requestBlocked(firstStackCycle.getConfiguration().getName()));

                                if (logger.isLogEnabled(LogLevel.DEBUG))
                                    logger.log(LogLevel.DEBUG, messages.reconcileUnblocked(firstStackCycle.getConfiguration().getName()));
                                reconcileBlocked = false;
                                requestBlocked = true;
                            }
                        });
                    }
                });

                return;
            }
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.reconcileBlocked());

        reconcileBlocked = true;
        if (configuration.isUseBatching())
            context.getDatabase().transaction(new ClosePeriodBatchOperation());
        else
            reconcileCurrentPeriod(null, null);
    }

    public void reconcileCurrentPeriod(ClosePeriodBatchOperation batch, IBatchControl batchControl) {
        long currentTime;
        if (batch != null) {
            if (batch.getCurrentTime() == 0)
                batch.setCurrentTime(Times.getCurrentTime());

            currentTime = batch.getCurrentTime();
        } else
            currentTime = Times.getCurrentTime();

        for (ICycleSchema cycle : cycles) {
            if (batch != null) {
                if (batch.getPeriodType() == null)
                    batch.setPeriodType(cycle.getConfiguration().getName());
                else if (!batch.getPeriodType().equals(cycle.getConfiguration().getName()))
                    continue;
            }

            int res = ((CycleSchema) cycle).reconcileCurrentPeriod(batch, batchControl, currentTime);
            if (res == 1)
                return;

            if (res == 2 && cycle == firstStackCycle) {
                requestBlocked = false;

                if (logger.isLogEnabled(LogLevel.DEBUG))
                    logger.log(LogLevel.DEBUG, messages.requestUnblocked(firstStackCycle.getConfiguration().getName()));
            }

            if (batch != null)
                batch.setPeriodType(null);
        }

        if (batch != null)
            batch.setCompleted();

        unblockReconcile();
    }

    public void unblockReconcile() {
        reconcileBlocked = false;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.reconcileUnblocked());
    }

    @Override
    public void onCreated() {
        for (ICycleSchema cycle : cycles)
            ((CycleSchema) cycle).addPeriod();
    }

    @Override
    public void onModified(ISpaceSchema oldSchema, IDataMigrator dataMigrator) {
        if (!oldSchema.getConfiguration().equalsStructured(configuration)) {
            onCreated();
            structuredChange = true;
        } else {
            structuredChange = false;
            IPeriodSpaceSchema periodSpaceSchema = (IPeriodSpaceSchema) oldSchema;
            for (int i = 0; i < cycles.size(); i++) {
                ICycleSchema newCycle = cycles.get(i);
                ICycleSchema oldCycle = periodSpaceSchema.getCycles().get(i);
                if (oldCycle.getConfiguration().hasFullTextIndex() != newCycle.getConfiguration().hasFullTextIndex()) {
                    structuredChange = true;
                    break;
                }
            }

            if (structuredChange)
                onCreated();
            else {
                for (int i = 0; i < cycles.size(); i++) {
                    ICycleSchema newCycle = cycles.get(i);
                    ICycleSchema oldCycle = periodSpaceSchema.getCycles().get(i);
                    ((CycleSchema) newCycle).bindCycle((CycleSchema) oldCycle);
                }
            }
        }
    }

    @Override
    public void onAfterModified(ISpaceSchema oldSchema) {
        if (structuredChange) {
            structuredChange = false;
            ((PeriodSpaceSchema) oldSchema).onDeleted();
        }
    }

    @Override
    public void onDeleted() {
        for (ICycleSchema cycle : cycles) {
            PeriodCycle currentCycle = ((CycleSchema) cycle).getCurrentCycle();
            if (currentCycle != null)
                currentCycle.close(null, null, true);
        }
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        List<ISchemaObject> children = new ArrayList<ISchemaObject>();
        children.addAll(cycles);
        return children;
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        Assert.notNull(type);

        if (type.equals(ICycleSchema.TYPE))
            return (Iterable) cycles;
        else
            return super.getChildren(type);
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        Assert.notNull(type);
        Assert.notNull(name);

        if (type.equals(ICycleSchema.TYPE))
            return (T) cyclesMap.get(name);
        else
            return super.findChild(type, name);
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        Assert.notNull(type);
        Assert.notNull(alias);

        if (type.equals(ICycleSchema.TYPE))
            return (T) cyclesByAliasMap.get(alias);
        else
            return super.findChildByAlias(type, alias);
    }

    @Override
    public IAggregationNodeSchema findAggregationNode(String componentType) {
        Assert.notNull(componentType);

        return aggregationNodes.get(componentType);
    }

    @Override
    public void dump(File path, IDumpContext context) {
        final Set<String> periodTypes;
        if (context.getQuery() != null && context.getQuery().contains("periodTypes"))
            periodTypes = JsonUtils.toSet((JsonArray) context.getQuery().get("periodTypes"));
        else
            periodTypes = null;

        for (ICycleSchema cycle : cycles) {
            if (periodTypes != null && !periodTypes.contains(cycle.getConfiguration().getAlias()))
                continue;

            ((CycleSchema) cycle).dump(path, context);
        }
    }

    private interface IMessages {
        @DefaultMessage("Period reconcile is blocked. Period: {0}")
        ILocalizedMessage reconcileBlocked(String name);

        @DefaultMessage("Period reconcile is unblocked. Period: {0}")
        ILocalizedMessage reconcileUnblocked(String name);

        @DefaultMessage("Period reconcile is blocked.")
        ILocalizedMessage reconcileBlocked();

        @DefaultMessage("Period reconcile is unblocked.")
        ILocalizedMessage reconcileUnblocked();

        @DefaultMessage("Measurements request is blocked. Period: {0}")
        ILocalizedMessage requestBlocked(String name);

        @DefaultMessage("Measurements request is unblocked. Period: {0}")
        ILocalizedMessage requestUnblocked(String name);
    }
}
