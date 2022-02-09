/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import org.apache.lucene.collation.ICUCollationKeyAnalyzer;
import org.apache.lucene.util.Version;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;


/**
 * The {@link CollationKeyAnalyzerSchemaConfiguration} is a configuration of index ICU collation key analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CollationKeyAnalyzerSchemaConfiguration extends AnalyzerSchemaConfiguration {
    private final String locale;
    private final Strength strength;

    public enum Strength {
        PRIMARY,
        SECONDARY,
        TERTIARY,
        QUATERNARY,
        IDENTICAL
    }

    public CollationKeyAnalyzerSchemaConfiguration(String locale, Strength strength) {
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
    public IAnalyzer createAnalyzer() {
        Collator collator = Collator.getInstance(new ULocale(locale));
        collator.setStrength(createStrength());
        return new IndexAnalyzer(new ICUCollationKeyAnalyzer(Version.LUCENE_4_9, collator));
    }

    @Override
    public boolean isSortable() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CollationKeyAnalyzerSchemaConfiguration))
            return false;

        CollationKeyAnalyzerSchemaConfiguration configuration = (CollationKeyAnalyzerSchemaConfiguration) o;
        return locale == configuration.locale && strength == configuration.strength;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locale, strength);
    }

    private int createStrength() {
        switch (strength) {
            case IDENTICAL:
                return Collator.IDENTICAL;
            case PRIMARY:
                return Collator.PRIMARY;
            case SECONDARY:
                return Collator.SECONDARY;
            case TERTIARY:
                return Collator.TERTIARY;
            case QUATERNARY:
                return Collator.QUATERNARY;
            default:
                return Assert.error();
        }
    }
}
