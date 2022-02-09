/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.Map;
import java.util.concurrent.Callable;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.impl.aggregator.common.actions.MeasurementStrategyAction;
import com.exametrika.spi.component.config.model.AsyncActionParameterDefinitionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AsyncActionSchemaConfiguration;


/**
 * The {@link MeasurementStrategyActionSchemaConfiguration} is a measurement strategy action schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class MeasurementStrategyActionSchemaConfiguration extends AsyncActionSchemaConfiguration {
    public MeasurementStrategyActionSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public Callable createLocal(Map<String, Object> parameters) {
        Assert.supports(false);
        return null;
    }

    @Override
    public Object createRemote(Map<String, Object> parameters) {
        return new MeasurementStrategyAction((String) parameters.get("measurementStrategyName"), (Boolean) parameters.get("allowed"));
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeasurementStrategyActionSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getName() + "(measurementStrategyName:string,allowed:boolean):void";
    }

    @Override
    protected Map<String, AsyncActionParameterDefinitionSchemaConfiguration> buildParameterDefinitions() {
        return new MapBuilder<String, AsyncActionParameterDefinitionSchemaConfiguration>()
                .put("measurementStrategyName", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, null, null))
                .put("allowed", new AsyncActionParameterDefinitionSchemaConfiguration(true, null, null, null))
                .toMap();
    }
}
