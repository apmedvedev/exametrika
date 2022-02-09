/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;


/**
 * The {@link CollatorKeyNormalizerSchemaConfiguration} is a configuration of collator key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CollatorKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    private final String locale;
    private final Strength strength;

    public enum Strength {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        QUATERNARY,
        IDENTICAL
    }

    public CollatorKeyNormalizerSchemaConfiguration(String locale, Strength strength) {
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
    public IKeyNormalizer createKeyNormalizer() {
        Collator collator = Collator.getInstance(new ULocale(locale));
        collator.setStrength(getCollatorStrength());

        return Indexes.createCollationKeyNormalizer(collator);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CollatorKeyNormalizerSchemaConfiguration))
            return false;

        CollatorKeyNormalizerSchemaConfiguration configuration = (CollatorKeyNormalizerSchemaConfiguration) o;
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

    private int getCollatorStrength() {
        switch (strength) {
            case PRIMARY:
                return Collator.PRIMARY;
            case SECONDARY:
                return Collator.SECONDARY;
            case TERTIARY:
                return Collator.TERTIARY;
            case QUATERNARY:
                return Collator.QUATERNARY;
            case IDENTICAL:
                return Collator.IDENTICAL;
            default:
                return Assert.error();
        }
    }
}
