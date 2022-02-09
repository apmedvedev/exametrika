/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap.config;

import com.exametrika.common.utils.Assert;


/**
 * The {@link ProjectionBitmapTypeConfiguration} is a configuration of projection bitmap type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProjectionBitmapTypeConfiguration extends BitmapTypeConfiguration {
    private final KeyNormalizerConfiguration keyNormalizer;

    public ProjectionBitmapTypeConfiguration(KeyNormalizerConfiguration keyNormalizer) {
        Assert.notNull(keyNormalizer);
        this.keyNormalizer = keyNormalizer;
    }

    public KeyNormalizerConfiguration getKeyNormalizer() {
        return keyNormalizer;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProjectionBitmapTypeConfiguration))
            return false;

        ProjectionBitmapTypeConfiguration configuration = (ProjectionBitmapTypeConfiguration) o;
        return keyNormalizer.equals(configuration.keyNormalizer);
    }

    @Override
    public int hashCode() {
        return keyNormalizer.hashCode();
    }
}
