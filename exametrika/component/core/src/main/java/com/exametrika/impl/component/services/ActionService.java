/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.security.ISecuredTransaction;
import com.exametrika.api.exadb.security.SecuredOperation;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.impl.component.actions.AsyncActionExecution;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;

import java.util.concurrent.Callable;


/**
 * The {@link ActionService} is a action execution service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionService extends DomainService {
    public static final String NAME = "component.ActionService";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ActionService.class);
    private IAgentActionExecutor actionExecutor;

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        actionExecutor = context.getDatabase().findParameter(IAgentActionExecutor.NAME);
        Assert.notNull(actionExecutor);
    }

    public void execute(AsyncActionExecution actionExecution) {
        Assert.notNull(actionExecution);

        if (actionExecution.isLocal())
            executeLocally(actionExecution);
        else
            executeRemotely(actionExecution);
    }

    private void executeLocally(AsyncActionExecution actionExecution) {
        final ICompletionHandler actionCompletionHandler = new ActionCompletionHandler(actionExecution);
        final Callable callable = actionExecution.createLocal();

        context.getCompartment().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object result = callable.call();
                    actionCompletionHandler.onSucceeded(result);
                } catch (Throwable e) {
                    actionCompletionHandler.onFailed(e);
                }
            }
        });
    }

    private void executeRemotely(final AsyncActionExecution actionExecution) {
        final ICompletionHandler actionCompletionHandler = new ActionCompletionHandler(actionExecution);

        context.getCompartment().execute(new Runnable() {
            @Override
            public void run() {
                actionExecutor.execute(actionExecution.getAction().getComponent().getScope().toString(), actionExecution.createRemote(),
                        actionCompletionHandler);
            }
        });
    }

    private class ActionCompletionHandler extends CompletionHandler {
        private final AsyncActionExecution actionExecution;
        private final long id;

        public ActionCompletionHandler(AsyncActionExecution actionExecution) {
            Assert.notNull(actionExecution);

            this.actionExecution = actionExecution;

            ComponentNode component = (ComponentNode) actionExecution.getAction().getComponent();
            if (actionExecution.isLoggable())
                id = component.logStart(actionExecution.getName(), actionExecution.getParameters());
            else
                id = 0;

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.actionStarted(component.getScope().toString(),
                        actionExecution.getName(), actionExecution.getParameters()));
        }

        @Override
        public void onSucceeded(final Object result) {
            if (actionExecution.getSession() != null) {
                actionExecution.getSession().transaction(new SecuredOperation() {
                    @Override
                    public void run(ISecuredTransaction transaction) {
                        txOnSucceeded(result);
                    }
                });
            } else {
                context.getDatabase().transaction(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        txOnSucceeded(result);
                    }
                });
            }
        }

        @Override
        public void onFailed(final Throwable error) {
            if (actionExecution.getSession() != null) {
                actionExecution.getSession().transaction(new SecuredOperation() {
                    @Override
                    public void run(ISecuredTransaction transaction) {
                        txOnFailed(error);
                    }
                });
            } else {
                context.getDatabase().transaction(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        txOnFailed(error);
                    }
                });
            }
        }

        private void txOnSucceeded(final Object result) {
            ComponentNode component = (ComponentNode) actionExecution.getAction().getComponent();
            if (actionExecution.isLoggable())
                component.logSuccess(id, actionExecution.getName(), actionExecution.getResult(result));

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.actionSucceeded(component.getScope().toString(),
                        actionExecution.getName(), actionExecution.getResult(result)));

            if (actionExecution.getCompletionHandler() != null)
                actionExecution.getCompletionHandler().onSucceeded(result);
        }


        private void txOnFailed(final Throwable error) {
            ComponentNode component = (ComponentNode) actionExecution.getAction().getComponent();
            if (actionExecution.isLoggable())
                component.logError(id, actionExecution.getName(), actionExecution.getError(error));

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.actionFailed(component.getScope().toString(),
                        actionExecution.getName(), actionExecution.getError(error)));

            if (actionExecution.getCompletionHandler() != null)
                actionExecution.getCompletionHandler().onFailed(error);
        }
    }

    private interface IMessages {
        @DefaultMessage("Action ''{0}'' of component ''{1}'' is started with parameters ''{2}''.")
        ILocalizedMessage actionStarted(String action, String component, Object parameters);

        @DefaultMessage("Action ''{0}'' of component ''{1}'' is succeeded with result:\n  {2}")
        ILocalizedMessage actionSucceeded(String action, String component, Object result);

        @DefaultMessage("Action ''{0}'' of component ''{1}'' is failed with error:\n  {2}")
        ILocalizedMessage actionFailed(String action, String component, Object error);
    }
}
