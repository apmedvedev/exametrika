/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.messaging;

import java.util.List;

/**
 * The {@link IMessageListPart} is a part of message containing list of messages.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IMessageListPart extends IMessagePart {
    /**
     * Returns list of messages.
     *
     * @return list of messages
     */
    List<IMessage> getMessages();
}
