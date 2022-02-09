/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import java.util.List;

import com.exametrika.common.messaging.IMessagePart;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;

/**
 * The {@link RemoveNamesMessage} is a remove names message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RemoveNamesMessage implements IMessagePart {
    private final List<SerializeNameId> removedNames;

    public RemoveNamesMessage(List<SerializeNameId> removedNames) {
        Assert.notNull(removedNames);

        this.removedNames = Immutables.wrap(removedNames);
    }

    public List<SerializeNameId> getRemovedNames() {
        return removedNames;
    }

    @Override
    public int getSize() {
        return 9 * removedNames.size() + 4;
    }

    @Override
    public String toString() {
        return removedNames.toString();
    }
}

