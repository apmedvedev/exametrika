/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Map;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.spi.aggregator.common.meters.IExpressionContext;


/**
 * The {@link IComponentJobOperationExpressionContext} represents a component job operation expression context.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IComponentJobOperationExpressionContext extends IExpressionContext {
    /**
     * Returns base component.
     *
     * @return base component
     */
    IComponent getComponent();

    /**
     * Returns job schema.
     *
     * @return job schema
     */
    JobSchemaConfiguration getSchema();

    /**
     * Is job predefined?
     *
     * @return true if job is predefined
     */
    boolean isPredefined();

    /**
     * Executes action.
     *
     * @param name       action name
     * @param parameters action parameters
     */
    void action(String name, Map<String, ?> parameters);
}
