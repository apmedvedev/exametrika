/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link CollatorSchemaConfiguration} is a configuration of collator schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CollatorSchemaConfiguration extends Configuration {
    private final String locale;
    private final Strength strength;

    public enum Strength {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        QUATERNARY,
        IDENTICAL
    }

    public CollatorSchemaConfiguration(String locale, Strength strength) {
        Assert.notNull(locale);
        Assert.notNull(strength);

        this.locale = locale;
        this.strength = strength;
    }

    public String getLocale() {
        return locale;
    }

    public Strength getStrength() {
        return strength;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CollatorSchemaConfiguration))
            return false;

        CollatorSchemaConfiguration configuration = (CollatorSchemaConfiguration) o;
        return locale.equals(configuration.locale) && strength == configuration.strength;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locale, strength);
    }

    @Override
    public final String toString() {
        return locale + ":" + strength;
    }
}
