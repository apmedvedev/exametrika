/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.component.IAsyncAction;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.services.ActionService;
import com.exametrika.spi.component.config.model.AsyncActionParameterDefinitionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AsyncActionSchemaConfiguration;


/**
 * The {@link AsyncAction} is a base asynchronous action.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class AsyncAction extends Action implements IAsyncAction {
    public AsyncAction(IComponent component, IActionSchema schema) {
        super(component, schema);
    }

    @Override
    public <T> void execute(Map<String, ?> parameters, ICompletionHandler<T> completionHandler) {
        IPermission permission = schema.getExecutePermission();
        permission.beginCheck(this);

        Map<String, Object> fullParameters = fillParameters(((AsyncActionSchemaConfiguration) schema.getConfiguration()).getParameterDefinitions(), parameters);
        getActionService().execute(new AsyncActionExecution(getSession(), this, fullParameters, completionHandler));

        permission.endCheck();
    }

    @Override
    protected void doExecute(Map<String, ?> parameters) {
        Map<String, Object> fullParameters = fillParameters(((AsyncActionSchemaConfiguration) schema.getConfiguration()).getParameterDefinitions(), parameters);
        getActionService().execute(new AsyncActionExecution(getSession(), this, fullParameters, null));
    }

    protected Map<String, Object> fillParameters(Map<String, AsyncActionParameterDefinitionSchemaConfiguration> parameterDefinitions, Map<String, ?> actualParameters) {
        Map<String, Object> fullParameters = new LinkedHashMap<String, Object>(actualParameters);
        IComponentVersion version = getComponent().getCurrentVersion();
        Assert.checkState(!version.isDeleted());

        JsonObject properties = version.getProperties();
        JsonObject options = version.getOptions();
        for (Map.Entry<String, AsyncActionParameterDefinitionSchemaConfiguration> entry : parameterDefinitions.entrySet()) {
            if (fullParameters.containsKey(entry.getKey()))
                continue;

            AsyncActionParameterDefinitionSchemaConfiguration parameterDefinition = entry.getValue();
            Object value = null;
            if (options != null && parameterDefinition.optionsName != null)
                value = options.get(parameterDefinition.optionsName, null);

            if (value == null && properties != null && parameterDefinition.propertiesName != null)
                value = properties.get(parameterDefinition.propertiesName, null);

            if (value == null)
                value = parameterDefinition.defaultValue;

            if (parameterDefinition.required)
                Assert.notNull(value);

            fullParameters.put(entry.getKey(), value);
        }

        return fullParameters;
    }

    protected final ActionService getActionService() {
        return ((ComponentNode) getComponent()).getNode().getTransaction().findDomainService(ActionService.NAME);
    }

    protected final ISession getSession() {
        ISecurityService securityService = ((ComponentNode) getComponent()).getNode().getTransaction().findDomainService(ISecurityService.NAME);
        return securityService.getSession();
    }
}
