/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import com.exametrika.common.utils.Assert;

/**
 * The {@link AlertRecipient} is an alert recipient.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertRecipient {
    private final String name;
    private final String address;

    public AlertRecipient(String name, String address) {
        Assert.notNull(name);
        Assert.notNull(address);

        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AlertRecipient))
            return false;

        AlertRecipient recipient = (AlertRecipient) o;
        return name.equals(recipient.name) && address.equals(recipient.address);
    }

    @Override
    public int hashCode() {
        return 31 * name.hashCode() + address.hashCode();
    }

    @Override
    public String toString() {
        return name + "(" + address + ")";
    }
}