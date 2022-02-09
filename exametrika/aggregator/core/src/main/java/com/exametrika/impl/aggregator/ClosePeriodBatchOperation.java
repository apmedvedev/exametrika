/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import gnu.trove.map.TLongObjectMap;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.RawBatchLock;
import com.exametrika.common.rawdb.RawBatchLock.Type;
import com.exametrika.impl.aggregator.AggregationService.EntryPointHierarchy;
import com.exametrika.impl.aggregator.schema.PeriodSpaceSchema;


/**
 * The {@link ClosePeriodBatchOperation} is a close period batch operation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ClosePeriodBatchOperation extends BatchOperation {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ClosePeriodBatchOperation.class);
    private long currentTime;
    private String periodType;
    private int cycleSchemaState;
    private int aggregationState;
    private long aggregationNodeId;
    private boolean interceptResult;
    private int totalEndNodes;
    private int aggregationEndNodes;
    private int totalNodes;
    private int aggregationNodes;
    private int endNodes;
    private int derivedNodes;
    private RuleContext ruleContext;
    private TLongObjectMap<EntryPointHierarchy> hierarchyMap;
    private int iteration;
    private boolean completed;
    private PeriodSpaceSchema periodSchema;

    public ClosePeriodBatchOperation() {
        super(IOperation.DISABLE_NODES_UNLOAD);
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public int getCycleSchemaState() {
        return cycleSchemaState;
    }

    public void setCycleSchemaState(int cycleSchemaState) {
        this.cycleSchemaState = cycleSchemaState;
    }

    public int getAggregationState() {
        return aggregationState;
    }

    public void setAggregationState(int aggregationState) {
        this.aggregationState = aggregationState;
    }

    public long getAggregationNodeId() {
        return aggregationNodeId;
    }

    public void setAggregationNodeId(long aggregationNodeId) {
        this.aggregationNodeId = aggregationNodeId;
    }

    public boolean getInterceptResult() {
        return interceptResult;
    }

    public void setInterceptResult(boolean interceptResult) {
        this.interceptResult = interceptResult;
    }

    public int getTotalEndNodes() {
        return totalEndNodes;
    }

    public void setTotalEndNodes(int totalEndNodes) {
        this.totalEndNodes = totalEndNodes;
    }

    public int getAggregationEndNodes() {
        return aggregationEndNodes;
    }

    public void setAggregationEndNodes(int aggregationEndNodes) {
        this.aggregationEndNodes = aggregationEndNodes;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public int getAggregationNodes() {
        return aggregationNodes;
    }

    public void setAggregationNodes(int aggregationNodes) {
        this.aggregationNodes = aggregationNodes;
    }

    public int getEndNodes() {
        return endNodes;
    }

    public void setEndNodes(int endNodes) {
        this.endNodes = endNodes;
    }

    public int getDerivedNodes() {
        return derivedNodes;
    }

    public void setDerivedNodes(int derivedNodes) {
        this.derivedNodes = derivedNodes;
    }

    public RuleContext getRuleContext() {
        return ruleContext;
    }

    public void setRuleContext(RuleContext ruleContext) {
        this.ruleContext = ruleContext;
    }

    public TLongObjectMap<EntryPointHierarchy> getHierarchyMap() {
        return hierarchyMap;
    }

    public void setHierarchyMap(TLongObjectMap<EntryPointHierarchy> hierarchyMap) {
        this.hierarchyMap = hierarchyMap;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public void setCompleted() {
        completed = true;
    }

    @Override
    public List<RawBatchLock> getLocks() {
        return Arrays.asList(new RawBatchLock(Type.SHARED, "db.aggregation"));
    }

    @Override
    public boolean run(ITransaction transaction, IBatchControl batchControl) {
        if (logger.isLogEnabled(LogLevel.DEBUG)) {
            if (iteration == 0)
                logger.log(LogLevel.DEBUG, messages.beginOperation());

            logger.log(LogLevel.DEBUG, messages.beginIteration(iteration));
        }

        periodSchema = transaction.getCurrentSchema().findDomain("aggregation").findSpace("aggregation");
        periodSchema.reconcileCurrentPeriod(this, batchControl);

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.endIteration(iteration));

        iteration++;

        return completed;
    }

    @Override
    public void onCommitted() {
        if (periodSchema != null)
            periodSchema.unblockReconcile();

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.endOperation());
    }

    @Override
    public void onRolledBack() {
        if (periodSchema != null)
            periodSchema.unblockReconcile();
    }

    private interface IMessages {
        @DefaultMessage("Begin iteration: {0}.")
        ILocalizedMessage beginIteration(int iteration);

        @DefaultMessage("End iteration: {0}.")
        ILocalizedMessage endIteration(int iteration);

        @DefaultMessage("Begin operation.")
        ILocalizedMessage beginOperation();

        @DefaultMessage("End operation.")
        ILocalizedMessage endOperation();
    }
}
