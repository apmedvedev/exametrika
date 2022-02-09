/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.messages;

import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link SynchronizeRolesMessage} is an synchronize roles message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SynchronizeRolesMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Set<String> roles;

    public SynchronizeRolesMessage(Set<String> roles) {
        Assert.notNull(roles);

        this.roles = Immutables.wrap(roles);
    }

    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(roles).toString();
    }

    private interface IMessages {
        @DefaultMessage("roles: {0}")
        ILocalizedMessage toString(Set<String> roles);
    }
}

