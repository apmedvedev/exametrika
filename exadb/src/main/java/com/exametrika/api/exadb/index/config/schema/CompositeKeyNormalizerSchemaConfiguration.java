/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link CompositeKeyNormalizerSchemaConfiguration} is a configuration of composite key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    private final List<KeyNormalizerSchemaConfiguration> keyNormalizers;

    public CompositeKeyNormalizerSchemaConfiguration(List<? extends KeyNormalizerSchemaConfiguration> keyNormalizers) {
        Assert.notNull(keyNormalizers);

        this.keyNormalizers = Immutables.wrap(keyNormalizers);
    }

    public List<KeyNormalizerSchemaConfiguration> getKeyNormalizers() {
        return keyNormalizers;
    }

    @Override
    public IKeyNormalizer createKeyNormalizer() {
        List<IKeyNormalizer> keyNormalizers = new ArrayList<IKeyNormalizer>();
        for (KeyNormalizerSchemaConfiguration keyNormalizer : this.keyNormalizers)
            keyNormalizers.add(keyNormalizer.createKeyNormalizer());

        return Indexes.createCompositeNormalizer(keyNormalizers);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeKeyNormalizerSchemaConfiguration))
            return false;

        CompositeKeyNormalizerSchemaConfiguration configuration = (CompositeKeyNormalizerSchemaConfiguration) o;
        return keyNormalizers.equals(configuration.keyNormalizers);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyNormalizers);
    }

    @Override
    public final String toString() {
        return keyNormalizers.toString();
    }
}
