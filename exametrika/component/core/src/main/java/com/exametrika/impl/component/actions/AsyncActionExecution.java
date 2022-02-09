/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.actions;

import java.util.Map;
import java.util.concurrent.Callable;

import com.exametrika.api.component.IAction;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.spi.component.config.model.AsyncActionSchemaConfiguration;


/**
 * The {@link AsyncActionExecution} is a base asynchronous action execution.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class AsyncActionExecution {
    private final ISession session;
    private final AsyncAction action;
    private Map<String, Object> parameters;
    private ICompletionHandler completionHandler;

    public AsyncActionExecution(ISession session, AsyncAction action, Map<String, Object> parameters, ICompletionHandler completionHandler) {
        Assert.notNull(action);
        Assert.notNull(parameters);

        this.session = session;
        this.action = action;
        this.parameters = parameters;
        this.completionHandler = completionHandler;
    }

    public ISession getSession() {
        return session;
    }

    public String getName() {
        return action.getSchema().getName();
    }

    public IAction getAction() {
        return action;
    }

    public boolean isLoggable() {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).isLoggable();
    }

    public boolean isLocal() {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).isLocal();
    }

    public <T> Callable<T> createLocal() {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).createLocal(parameters);
    }

    public Object createRemote() {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).createRemote(parameters);
    }

    public Object getParameters() {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).getParameters(parameters);
    }

    public <T> ICompletionHandler<T> getCompletionHandler() {
        return completionHandler;
    }

    public Object getResult(Object result) {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).getResult(result);
    }

    public Object getError(Throwable exception) {
        return ((AsyncActionSchemaConfiguration) action.getSchema().getConfiguration()).getError(exception);
    }
}
