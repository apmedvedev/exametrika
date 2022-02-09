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
 * The {@link InstallRoleMessage} is an install role message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstallRoleMessage implements IMessagePart {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String roleName;
    private final String md5Hash;

    public InstallRoleMessage(String roleName, String md5Hash) {
        Assert.notNull(roleName);
        Assert.notNull(md5Hash);

        this.roleName = roleName;
        this.md5Hash = md5Hash;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return messages.toString(roleName, md5Hash).toString();
    }

    private interface IMessages {
        @DefaultMessage("role: {0}, md5 hash: {1}")
        ILocalizedMessage toString(String roleName, String md5Hash);
    }
}

