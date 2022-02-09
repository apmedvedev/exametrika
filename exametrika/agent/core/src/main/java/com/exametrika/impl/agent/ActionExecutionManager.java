/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.Services;
import com.exametrika.common.tasks.impl.RunnableTaskHandler;
import com.exametrika.common.tasks.impl.TaskExecutor;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.agent.messages.ActionMessage;
import com.exametrika.impl.agent.messages.ActionMessageSerializer;
import com.exametrika.spi.agent.IActionExecutor;


/**
 * The {@link ActionExecutionManager} represents an agent's action executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionExecutionManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ActionExecutionManager.class);
    private final AgentChannel channel;
    private volatile List<IActionExecutor> actionExecutors = new ArrayList<IActionExecutor>();
    private final TaskExecutor<Runnable> taskExecutor;
    private final TaskQueue<Runnable> tasks;

    public ActionExecutionManager(AgentChannel channel) {
        Assert.notNull(channel);

        this.channel = channel;
        this.tasks = new TaskQueue<Runnable>();
        taskExecutor = new TaskExecutor<Runnable>(Runtime.getRuntime().availableProcessors() * 2, tasks, new RunnableTaskHandler<Runnable>(),
                "[" + channel.getName() + "] agent channel task thread");
    }

    public void register(ISerializationRegistry registry) {
        registry.register(new ActionMessageSerializer());
    }

    public void unregister(ISerializationRegistry registry) {
        registry.unregister(ActionMessageSerializer.ID);
    }

    public void handle(final ActionMessage message) {
        for (final IActionExecutor actionExecutor : actionExecutors) {
            if (!actionExecutor.supports(message.getAction()))
                continue;

            tasks.offer(new Runnable() {
                @Override
                public void run() {
                    execute(message, actionExecutor);
                }
            });
            return;
        }

        if (logger.isLogEnabled(LogLevel.WARNING))
            logger.log(LogLevel.WARNING, messages.executorNotFound(toString(message.getAction())));
    }

    public void start() {
        List<IActionExecutor> actionExecutors = Services.loadProviders(IActionExecutor.class);
        for (IActionExecutor actionExecutor : actionExecutors)
            actionExecutor.start();

        this.actionExecutors = actionExecutors;

        taskExecutor.start();
    }

    public void stop() {
        taskExecutor.stop();

        for (IActionExecutor actionExecutor : actionExecutors)
            actionExecutor.stop();

        this.actionExecutors = new ArrayList<IActionExecutor>();
    }

    private void execute(ActionMessage message, IActionExecutor actionExecutor) {
        ActionContext context = new ActionContext(message.getActionId(), message.getAction(), channel);

        try {
            actionExecutor.execute(context);

            if (!context.isSent())
                context.sendResult(null, null);

            if (logger.isLogEnabled(LogLevel.DEBUG))
                logger.log(LogLevel.DEBUG, messages.actionSucceeded(toString(context.getAction())));
        } catch (Throwable e) {
            if (!context.isSent())
                context.sendError(e);

            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, messages.actionFailed(toString(context.getAction())));

            if (logger.isLogEnabled(LogLevel.ERROR))
                logger.log(LogLevel.ERROR, e);
        }
    }

    private String toString(Object action) {
        return action.getClass().getSimpleName() + ":" + action.toString();
    }

    private interface IMessages {
        @DefaultMessage("Executor is not found for action ''{0}''.")
        ILocalizedMessage executorNotFound(Object action);

        @DefaultMessage("Action ''{0}'' is succeeded.")
        ILocalizedMessage actionSucceeded(Object action);

        @DefaultMessage("Action ''{0}'' is failed.")
        ILocalizedMessage actionFailed(Object action);
    }
}
