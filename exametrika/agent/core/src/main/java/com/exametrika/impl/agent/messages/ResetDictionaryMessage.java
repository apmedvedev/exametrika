/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.messages;

import com.exametrika.common.messaging.IMessagePart;

/**
 * The {@link ResetDictionaryMessage} is a reset dictionary message.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ResetDictionaryMessage implements IMessagePart {
    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return "resetDictionary";
    }
}

