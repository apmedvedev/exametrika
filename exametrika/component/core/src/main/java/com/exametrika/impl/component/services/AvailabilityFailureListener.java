/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.IAgentFailureListener;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link AvailabilityFailureListener} is a availability failure listener.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class AvailabilityFailureListener implements IAgentFailureListener {
    private final HealthService healthService;
    private final IDatabaseContext context;

    public AvailabilityFailureListener(HealthService healthService, IDatabaseContext context) {
        Assert.notNull(healthService);
        Assert.notNull(context);

        this.healthService = healthService;
        this.context = context;
    }

    @Override
    public void onAgentActivated(final String agentId) {
        context.getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                healthService.onAgentActivated(agentId);
            }
        });
    }

    @Override
    public void onAgentFailed(final String agentId) {
        context.getDatabase().transaction(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                healthService.onAgentFailed(agentId);
            }
        });
    }
}