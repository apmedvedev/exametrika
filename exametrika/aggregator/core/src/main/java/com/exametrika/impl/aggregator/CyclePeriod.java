/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Times;


/**
 * The {@link CyclePeriod} is a cycle period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class CyclePeriod extends Period {
    public static final int PERIOD_INDEX = -1;
    private final CyclePeriodSpace cycleSpace;

    public CyclePeriod(CyclePeriodSpace cycleSpace, int fileIndex, long periodBlockIndex, boolean create) {
        super(cycleSpace.getPeriodSpace(), PERIOD_INDEX, fileIndex, periodBlockIndex, create, Times.getCurrentTime());

        this.cycleSpace = cycleSpace;
    }

    @Override
    public long allocateBlocks(int blockCount) {
        return cycleSpace.allocateBlocks(blockCount);
    }

    @Override
    protected INodeSchema getRootNodeSchema() {
        ICycleSchema schema = getSpace().getSchema();
        return schema.getCyclePeriodRootNode();
    }

    @Override
    public String toString() {
        return cycleSpace.getFileName() + "[cycle]";
    }

    @Override
    protected boolean doClose(ClosePeriodBatchOperation batch, IBatchControl batchControl, boolean schemaChange) {
        return true;
    }
}
