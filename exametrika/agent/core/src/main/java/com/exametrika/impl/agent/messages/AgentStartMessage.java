/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;

/**
 * The {@link AgentStartMessage} is a message sent by agent after establishing connection with server.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AgentStartMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String component;
    private final String configHash;
    private final String modulesHash;

    public AgentStartMessage(String component, String configHash, String modulesHash) {
        Assert.notNull(component);

        this.component = component;
        this.configHash = configHash;
        this.modulesHash = modulesHash;
    }

    public String getComponent() {
        return component;
    }

    public String getConfigHash() {
        return configHash;
    }

    public String getModulesHash() {
        return modulesHash;
    }

    @Override
    public int getSize() {
        return component.length() * 2 + (configHash != null ? configHash.length() * 2 : 0) +
                (modulesHash != null ? modulesHash.length() * 2 : 0);
    }

    @Override
    public String toString() {
        return messages.toString(component, configHash, modulesHash).toString();
    }

    private interface IMessages {
        @DefaultMessage("component: {0}, config hash: {1}, modules hash: {2}")
        ILocalizedMessage toString(String component, String configHash, String modulesHash);
    }
}

