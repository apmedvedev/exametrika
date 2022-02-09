/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent;

import java.io.File;
import java.util.List;

import com.exametrika.common.services.IService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.agent.messages.ActionMessage;
import com.exametrika.spi.agent.IActionContext;

/**
 * The {@link ActionContext} is an action context.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ActionContext implements IActionContext {
    private final long actionId;
    private final Object action;
    private final AgentChannel channel;
    private boolean sent;

    public ActionContext(long actionId, Object action, AgentChannel channel) {
        Assert.notNull(action);
        Assert.notNull(channel);

        this.actionId = actionId;
        this.action = action;
        this.channel = channel;
    }

    public boolean isSent() {
        return sent;
    }

    @Override
    public Object getAction() {
        return action;
    }

    @Override
    public <T extends IService> T findService(String name) {
        return channel.getServiceRegistry().findService(name);
    }

    @Override
    public void sendResult(Object result, List<File> files) {
        Assert.checkState(!sent);
        channel.send(new ActionMessage(actionId, result), files);
        sent = true;
    }

    @Override
    public void sendError(Throwable error) {
        Assert.checkState(!sent);
        channel.send(new ActionMessage(actionId, error));
        sent = true;
    }
}
