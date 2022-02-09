/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap.config;

import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link BitmapIndexFieldSchemaConfiguration} is a configuration of bitmap index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BitmapIndexFieldSchemaConfiguration extends Configuration {
    private final String name;
    private final Set<Option> options;
    private final KeyNormalizerConfiguration keyNormalizer;
    private final BitmapTypeConfiguration bitmapType;
    private final ConditionConfiguration condition;

    public enum Option {
        UNIQUE,

        SORTED,

        REQUIRED,

        COMPUTED
    }

    public BitmapIndexFieldSchemaConfiguration(String name, Set<Option> options, KeyNormalizerConfiguration keyNormalizer,
                                               BitmapTypeConfiguration bitmapType, ConditionConfiguration condition) {
        Assert.notNull(name);
        Assert.notNull(options);
        Assert.notNull(keyNormalizer);
        Assert.notNull(bitmapType);
        Assert.isTrue(options.contains(Option.COMPUTED) == (condition != null));

        this.name = name;
        this.options = Immutables.wrap(options);
        this.keyNormalizer = keyNormalizer;
        this.bitmapType = bitmapType;
        this.condition = condition;
    }

    public String getName() {
        return name;
    }

    public Set<Option> getOptions() {
        return options;
    }

    public KeyNormalizerConfiguration getKeyNormalizer() {
        return keyNormalizer;
    }

    public BitmapTypeConfiguration getBitmapType() {
        return bitmapType;
    }

    public ConditionConfiguration getCondition() {
        return condition;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BitmapIndexFieldSchemaConfiguration))
            return false;

        BitmapIndexFieldSchemaConfiguration configuration = (BitmapIndexFieldSchemaConfiguration) o;
        return name.equals(configuration.name) && options.equals(configuration.options) &&
                keyNormalizer.equals(configuration.keyNormalizer) && bitmapType.equals(configuration.bitmapType) &&
                Objects.equals(condition, configuration.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, options, keyNormalizer, bitmapType, condition);
    }

    @Override
    public String toString() {
        return name;
    }
}
