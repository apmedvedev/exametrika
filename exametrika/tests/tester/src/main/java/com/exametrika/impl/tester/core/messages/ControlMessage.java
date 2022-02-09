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
 * The {@link ControlMessage} is a control message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ControlMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String nodeName;
    private final Type type;

    public enum Type {
        START,
        STOP,
        STOP_FAILED,
        COLLECT_RESULTS,
        RUN,
        RUN_STOPPED_RESPONSE
    }

    public ControlMessage(String nodeName, Type type) {
        Assert.notNull(nodeName);
        Assert.notNull(type);

        this.nodeName = nodeName;
        this.type = type;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(nodeName, type).toString();
    }

    private interface IMessages {
        @DefaultMessage("node: {0}, type: {1}")
        ILocalizedMessage toString(String nodeName, Type type);
    }
}

