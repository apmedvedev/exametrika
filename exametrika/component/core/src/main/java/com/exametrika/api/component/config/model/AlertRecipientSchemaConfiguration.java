/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link AlertRecipientSchemaConfiguration} is an alert recipient schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertRecipientSchemaConfiguration extends Configuration {
    private final Type type;
    private final String name;
    private final String address;

    public enum Type {
        ROLE,

        USER_GROUP,

        USER,

        ADDRESS
    }

    public AlertRecipientSchemaConfiguration(String name, String address) {
        Assert.notNull(name);
        Assert.notNull(address);

        this.type = Type.ADDRESS;
        this.name = name;
        this.address = address;
    }

    public AlertRecipientSchemaConfiguration(Type type, String name) {
        Assert.notNull(type);
        Assert.isTrue(type != Type.ADDRESS);
        Assert.notNull(name);

        this.type = type;
        this.name = name;
        this.address = null;
    }

    public Type getType() {
        return type;
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
        if (!(o instanceof AlertRecipientSchemaConfiguration))
            return false;

        AlertRecipientSchemaConfiguration configuration = (AlertRecipientSchemaConfiguration) o;
        return type == configuration.type && name.equals(configuration.name) && Objects.equals(address, configuration.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, name, address);
    }

    @Override
    public String toString() {
        return "[" + type.toString().toLowerCase() + "]" + name + (address != null ? ("(" + address + ")") : "");
    }
}
