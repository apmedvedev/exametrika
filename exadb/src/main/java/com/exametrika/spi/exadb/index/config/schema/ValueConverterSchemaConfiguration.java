/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.common.config.Configuration;

/**
 * The {@link ValueConverterSchemaConfiguration} represents a configuration of value converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ValueConverterSchemaConfiguration extends Configuration {
    public abstract <V> IValueConverter<V> createValueConverter();
}
