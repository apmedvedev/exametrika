/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.nio.socket;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Set;


/**
 * The {@link ITcpSelector} is a TCP socket channel selector.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpSelector extends Closeable {
    boolean isOpen();

    Set<SelectionKey> keys();

    Set<SelectionKey> selectedKeys();

    int selectNow() throws IOException;

    int select(long timeout) throws IOException;

    int select() throws IOException;

    void wakeup();
}
