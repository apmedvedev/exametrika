/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link PlatformUpdateMessage} is a base platform update message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformUpdateMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String configuration;
    private final String configHash;
    private final String modulesHash;

    public PlatformUpdateMessage(String configuration, String configHash, String modulesHash) {
        Assert.isTrue((configuration == null) == (configHash == null));
        this.configuration = configuration;
        this.configHash = configHash;
        this.modulesHash = modulesHash;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getConfigHash() {
        return configHash;
    }

    public String getModulesHash() {
        return modulesHash;
    }

    @Override
    public int getSize() {
        return (configuration != null ? configuration.length() * 2 : 0) +
                (configHash != null ? configHash.length() * 2 : 0) + (modulesHash != null ? modulesHash.length() * 2 : 0);
    }

    @Override
    public final String toString() {
        return messages.toString(configuration != null ? Strings.indent(configuration, 4) : null, configHash, modulesHash).toString();
    }

    private interface IMessages {
        @DefaultMessage("config hash: {1}, modules hash: {2}, configuration:\n{0}")
        ILocalizedMessage toString(String configuration, String configHash, String modulesHash);
    }
}

