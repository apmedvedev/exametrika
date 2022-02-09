/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.spi.aggregator.IMeasurementExpressionContext;


/**
 * The {@link ISimpleAlertExpressionContext} represents a simple alert expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ISimpleAlertExpressionContext extends IMeasurementExpressionContext {
    /**
     * Returns base component.
     *
     * @return base component
     */
    IComponent getComponent();

    /**
     * Returns measurement.
     *
     * @return measurement
     */
    IAggregationNode getMeasurement();
}
