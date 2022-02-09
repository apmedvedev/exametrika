/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.impl.component.fields.VersionTimeKeyNormalizer;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link VersionTimeKeyNormalizerSchemaConfiguration} is a configuration of version time key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionTimeKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    @Override
    public IKeyNormalizer createKeyNormalizer() {
        return new VersionTimeKeyNormalizer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VersionTimeKeyNormalizerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public final String toString() {
        return "versionTime";
    }
}
