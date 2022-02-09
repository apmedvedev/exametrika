/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link ActionResponseMessage} is an action response message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionResponseMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String nodeName;
    private final String actionName;
    private final Throwable error;

    public ActionResponseMessage(String nodeName, String actionName, Throwable error) {
        Assert.notNull(nodeName);
        Assert.notNull(actionName);

        this.nodeName = nodeName;
        this.actionName = actionName;
        this.error = error;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getActionName() {
        return actionName;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(nodeName, actionName, error).toString();
    }

    private interface IMessages {
        @DefaultMessage("node: {0}, action: {1}, error: {2}")
        ILocalizedMessage toString(String nodeName, String actionName, Throwable error);
    }
}

