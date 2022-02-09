/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.aggregator.IRuleService;
import com.exametrika.spi.exadb.core.DomainService;


/**
 * The {@link RuleService} is a rule service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RuleService extends DomainService implements IRuleService {
    private INodeIndex<Long, IComponent> componentIndex;

    @Override
    public IRuleExecutor findRuleExecutor(long scopeId) {
        ensureSpace();

        IComponent component = componentIndex.find(scopeId);
        if (component == null || component.getCurrentVersion().isDeleted())
            return null;
        else
            return (IRuleExecutor) component;
    }

    @Override
    public void clearCaches() {
        componentIndex = null;
    }

    private void ensureSpace() {
        if (componentIndex == null) {
            IObjectSpaceSchema spaceSchema = schema.getParent().findSpace("component");
            Assert.notNull(spaceSchema);

            componentIndex = spaceSchema.getSpace().findIndex("componentIndex");
            Assert.checkState(componentIndex != null);
        }
    }
}
