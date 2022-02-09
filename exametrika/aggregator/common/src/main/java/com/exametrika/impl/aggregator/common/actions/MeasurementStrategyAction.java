/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.actions;

import java.io.Serializable;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;

/**
 * The {@link MeasurementStrategyAction} is a measurement strategy control action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementStrategyAction implements Serializable {
    private static final IMessages messages = Messages.get(IMessages.class);
    private String strategyName;
    private boolean allowed;

    public MeasurementStrategyAction(String strategyName, boolean allowed) {
        Assert.notNull(strategyName);

        this.strategyName = strategyName;
        this.allowed = allowed;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public boolean isAllowed() {
        return allowed;
    }

    @Override
    public String toString() {
        return messages.toString(strategyName, allowed).toString();
    }

    private interface IMessages {
        @DefaultMessage("strategy: {0}, allowed: {1}")
        ILocalizedMessage toString(String strategyName, boolean allowed);
    }
}

