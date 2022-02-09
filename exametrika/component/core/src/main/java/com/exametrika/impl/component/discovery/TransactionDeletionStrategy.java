/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.discovery;

import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.TransactionComponentNode;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link TransactionDeletionStrategy} is a transaction component deletion strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class TransactionDeletionStrategy extends BaseComponentDeletionStrategy {
    private final int retentionPeriodCount;

    public TransactionDeletionStrategy(int retentionPeriodCount, IDatabaseContext context) {
        super(context);

        this.retentionPeriodCount = retentionPeriodCount;
    }

    @Override
    protected Iterable<? extends IHealthComponent> getComponents(ComponentRootNode root) {
        return root.getTransactions();
    }

    @Override
    protected boolean isActive(IHealthComponent component) {
        TransactionComponentNode transaction = (TransactionComponentNode) component;
        return transaction.incrementRetentionCounter() < retentionPeriodCount;
    }

    @Override
    protected void onActivate(IHealthComponent component) {
        TransactionComponentNode transaction = (TransactionComponentNode) component;
        transaction.resetRetentionCounter();
    }
}
