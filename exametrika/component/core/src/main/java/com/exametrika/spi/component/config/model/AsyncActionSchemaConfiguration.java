/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.component.actions.AsyncAction;

/**
 * The {@link AsyncActionSchemaConfiguration} represents a configuration of schema of component asynchronous action.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AsyncActionSchemaConfiguration extends ActionSchemaConfiguration {
    private final Map<String, AsyncActionParameterDefinitionSchemaConfiguration> parameterDefinitions;

    public AsyncActionSchemaConfiguration(String name) {
        super(name);

        parameterDefinitions = buildParameterDefinitions();
    }

    @Override
    public <T extends IAction> T createAction(IComponent component, IActionSchema schema) {
        return (T) new AsyncAction(component, schema);
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    public boolean isLoggable() {
        return true;
    }

    public boolean isLocal() {
        return true;
    }

    public Map<String, AsyncActionParameterDefinitionSchemaConfiguration> getParameterDefinitions() {
        return parameterDefinitions;
    }

    public abstract Callable createLocal(Map<String, Object> parameters);

    public abstract Object createRemote(Map<String, Object> parameters);

    public Object getParameters(Map<String, Object> parameters) {
        return JsonUtils.toJson(parameters);
    }

    public Object getResult(Object result) {
        if (result != null)
            return result.toString();
        else
            return null;
    }

    public Object getError(Throwable exception) {
        if (exception != null) {
            Json json = Json.object();
            Meters.buildExceptionStackTrace(exception, 100, 512, json, true);
            return json.toObject();
        } else
            return null;
    }

    protected Map<String, AsyncActionParameterDefinitionSchemaConfiguration> buildParameterDefinitions() {
        return Collections.<String, AsyncActionParameterDefinitionSchemaConfiguration>emptyMap();
    }
}
