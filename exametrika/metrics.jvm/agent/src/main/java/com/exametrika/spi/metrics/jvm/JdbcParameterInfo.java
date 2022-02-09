/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import com.exametrika.common.utils.Assert;


/**
 * The {@link JdbcParameterInfo} represents an information about JDBC parameter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JdbcParameterInfo {
    private final String name;
    private final Object value;

    public JdbcParameterInfo(String name, Object value) {
        Assert.notNull(name);

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + "=" + ((value instanceof String) ? quote((String) value) : value);
    }

    private String quote(String value) {
        StringBuilder builder = new StringBuilder();
        builder.append('\'');

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'')
                builder.append('\'');

            builder.append(c);
        }

        builder.append('\'');

        return builder.toString();
    }
}
