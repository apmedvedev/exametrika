/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link ActionMessage} is an action message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final long actionId;
    private final Object action;

    public ActionMessage(long actionId, Object action) {
        this.actionId = actionId;
        this.action = action;
    }

    public long getActionId() {
        return actionId;
    }

    public Object getAction() {
        return action;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(actionId, action != null ? action.getClass().getSimpleName() : "null", action).toString();
    }

    private interface IMessages {
        @DefaultMessage("action-id: {0}, {1}: {2}")
        ILocalizedMessage toString(long actionId, String actionType, Object action);
    }
}

