/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.messaging;

import java.util.UUID;

import com.exametrika.common.messaging.IAddress;
import com.exametrika.common.messaging.impl.transports.UnicastAddress;


/**
 * The {@link TestAddress} a mock implementation of {@link IAddress}.
 *
 * @author Medvedev-A
 */
public class TestAddress extends UnicastAddress {
    public TestAddress(UUID id, String name) {
        super(id, name);
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public String getConnection(int transportId) {
        return getName();
    }
}
