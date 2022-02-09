/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.List;

import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodNodeSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.schema.NodeSchema;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;


/**
 * The {@link PeriodNodeSchema} represents a schema of aggregation node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class PeriodNodeSchema extends NodeSchema implements IPeriodNodeSchema {
    private IPeriodNodeSchema prevPeriodNode;
    private IPeriodNodeSchema nextPeriodNode;

    public PeriodNodeSchema(PeriodNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                            IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        ICycleSchema cycleSchema = getParent();
        IPeriodSpaceSchema spaceSchema = cycleSchema.getParent();

        if (cycleSchema.getIndex() > 0) {
            ICycleSchema prevCycleSchema = spaceSchema.getCycles().get(cycleSchema.getIndex() - 1);
            prevPeriodNode = prevCycleSchema.findNode(getConfiguration().getName());
        } else
            prevPeriodNode = null;

        if (cycleSchema.getIndex() < spaceSchema.getCycles().size() - 1) {
            ICycleSchema nextCycleSchema = spaceSchema.getCycles().get(cycleSchema.getIndex() + 1);
            nextPeriodNode = nextCycleSchema.findNode(getConfiguration().getName());
        } else
            nextPeriodNode = null;
    }

    @Override
    public PeriodNodeSchemaConfiguration getConfiguration() {
        return (PeriodNodeSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public ICycleSchema getParent() {
        return (ICycleSchema) super.getParent();
    }

    @Override
    public IPeriodNodeSchema getPreviousPeriodNode() {
        return prevPeriodNode;
    }

    @Override
    public IPeriodNodeSchema getNextPeriodNode() {
        return nextPeriodNode;
    }
}
