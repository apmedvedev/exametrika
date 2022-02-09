/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.common.config.Configuration;

/**
 * The {@link KeyNormalizerSchemaConfiguration} represents a configuration of key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class KeyNormalizerSchemaConfiguration extends Configuration {
    public abstract <K> IKeyNormalizer<K> createKeyNormalizer();
}
