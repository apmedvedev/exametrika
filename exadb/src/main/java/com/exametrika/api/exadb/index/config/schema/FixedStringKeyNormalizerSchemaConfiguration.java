/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link FixedStringKeyNormalizerSchemaConfiguration} is a configuration of fixed string key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FixedStringKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    @Override
    public IKeyNormalizer createKeyNormalizer() {
        return Indexes.createFixedStringKeyNormalizer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FixedStringKeyNormalizerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public final String toString() {
        return "string";
    }
}
