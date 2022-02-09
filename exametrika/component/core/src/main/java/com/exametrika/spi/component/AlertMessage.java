/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.List;

import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;

/**
 * The {@link AlertMessage} is an alert message.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class AlertMessage {
    private final Type type;
    private final AlertChannelSchemaConfiguration schema;
    private final List<AlertRecipient> recipients;
    private final String text;

    public enum Type {
        ON,
        OFF,
        STATUS
    }

    public AlertMessage(Type type, AlertChannelSchemaConfiguration schema, List<AlertRecipient> recipients, String text) {
        Assert.notNull(type);
        Assert.notNull(schema);
        Assert.notNull(recipients);
        Assert.isTrue(!recipients.isEmpty());
        Assert.notNull(text);

        this.type = type;
        this.schema = schema;
        this.recipients = Immutables.wrap(recipients);
        this.text = text;
    }

    public Type getType() {
        return type;
    }

    public AlertChannelSchemaConfiguration getSchema() {
        return schema;
    }

    public List<AlertRecipient> getRecipients() {
        return recipients;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return type.toString().toLowerCase() + ":" + recipients + " - " + text;
    }
}