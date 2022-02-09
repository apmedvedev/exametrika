/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link DescendingKeyNormalizerSchemaConfiguration} is a configuration of descending key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DescendingKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    private final KeyNormalizerSchemaConfiguration keyNormalizer;

    public DescendingKeyNormalizerSchemaConfiguration(KeyNormalizerSchemaConfiguration keyNormalizer) {
        Assert.notNull(keyNormalizer);

        this.keyNormalizer = keyNormalizer;
    }

    public KeyNormalizerSchemaConfiguration getKeyNormalizer() {
        return keyNormalizer;
    }

    @Override
    public IKeyNormalizer createKeyNormalizer() {
        return Indexes.createDescendingNormalizer(keyNormalizer.createKeyNormalizer());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DescendingKeyNormalizerSchemaConfiguration))
            return false;

        DescendingKeyNormalizerSchemaConfiguration configuration = (DescendingKeyNormalizerSchemaConfiguration) o;
        return keyNormalizer.equals(configuration.keyNormalizer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyNormalizer);
    }

    @Override
    public final String toString() {
        return "descending:" + keyNormalizer;
    }
}
