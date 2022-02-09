/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.component.config.schema.BehaviorTypeNodeSchemaConfiguration;
import com.exametrika.api.component.nodes.IBehaviorType;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.BehaviorType;
import com.exametrika.spi.aggregator.IBehaviorTypeProvider;
import com.exametrika.spi.exadb.core.DomainService;


/**
 * The {@link BehaviorTypeService} is a behavior type service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BehaviorTypeService extends DomainService implements IBehaviorTypeProvider {
    private IObjectSpaceSchema spaceSchema;
    private INodeSchema nodeSchema;

    @Override
    public boolean containsBehaviorType(int typeId) {
        ensureSpace();

        return spaceSchema.getSpace().containsNode(typeId, nodeSchema);
    }

    @Override
    public BehaviorType findBehaviorType(int typeId) {
        ensureSpace();

        IBehaviorType node = spaceSchema.getSpace().findNode(typeId, nodeSchema);
        if (node != null)
            return new BehaviorType(node.getName(), node.getMetadata(), node.getTags());
        else
            return null;
    }

    @Override
    public void addBehaviorType(int typeId, BehaviorType behaviorType) {
        Assert.notNull(behaviorType);

        ensureSpace();

        IBehaviorType node = spaceSchema.getSpace().findOrCreateNode(typeId, nodeSchema);
        node.setName(behaviorType.getName());
        node.setMetadata(behaviorType.getMetadata());
        node.setTags(behaviorType.getLabels());
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
        nodeSchema = null;
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("component");
            Assert.notNull(spaceSchema);

            nodeSchema = spaceSchema.getSpace().getSchema().findNode(BehaviorTypeNodeSchemaConfiguration.NAME);
            Assert.notNull(nodeSchema);
        }
    }
}
