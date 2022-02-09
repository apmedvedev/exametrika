/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.spi.exadb.index.config.schema.ValueConverterSchemaConfiguration;


/**
 * The {@link ByteArrayValueConverterSchemaConfiguration} is a configuration of byte array value converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ByteArrayValueConverterSchemaConfiguration extends ValueConverterSchemaConfiguration {
    @Override
    public IValueConverter createValueConverter() {
        return Indexes.createByteArrayValueConverter();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ByteArrayValueConverterSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public final String toString() {
        return "ByteArray";
    }
}
