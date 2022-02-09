/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IGroupAvailabilityExpressionContext} represents a group availability expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IGroupAvailabilityExpressionContext extends IExpressionContext {
    /**
     * Returns base group.
     *
     * @return base group
     */
    IGroupComponent getGroup();

    /**
     * Returns number of available components and groups.
     *
     * @return number of available components and groups
     */
    int getAvailable();

    /**
     * Returns number of available components.
     *
     * @return number of available components
     */
    int getAvailableComponents();

    /**
     * Returns number of available groups.
     *
     * @return number of available groups
     */
    int getAvailableGroups();
}
