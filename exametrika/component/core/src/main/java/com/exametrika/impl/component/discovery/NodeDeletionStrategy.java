/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.services.HealthService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link NodeDeletionStrategy} is a node component deletion strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class NodeDeletionStrategy extends BaseComponentDeletionStrategy {
    private HealthService healthService;

    public NodeDeletionStrategy(IDatabaseContext context) {
        super(context);
    }

    @Override
    protected Iterable<? extends IHealthComponent> getComponents(ComponentRootNode root) {
        return root.getNodes();
    }

    @Override
    protected boolean isActive(IHealthComponent component) {
        if (healthService == null)
            healthService = context.getTransactionProvider().getTransaction().findDomainService(HealthService.NAME);

        return healthService.isAgentActive(component);
    }
}
