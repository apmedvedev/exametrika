/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link SynchronizeRolesResponseMessage} is a synchronize roles response message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SynchronizeRolesResponseMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Map<String, String> rolesHashes;

    public SynchronizeRolesResponseMessage(Map<String, String> rolesHashes) {
        Assert.notNull(rolesHashes);

        this.rolesHashes = Immutables.wrap(rolesHashes);
    }

    public Map<String, String> getRolesHashes() {
        return rolesHashes;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(rolesHashes).toString();
    }

    private interface IMessages {
        @DefaultMessage("roles hashes: {0}")
        ILocalizedMessage toString(Map<String, String> rolesHashes);
    }
}

