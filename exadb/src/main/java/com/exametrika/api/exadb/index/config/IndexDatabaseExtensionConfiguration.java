/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config;

import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;


/**
 * The {@link IndexDatabaseExtensionConfiguration} is a configuration of index.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexDatabaseExtensionConfiguration extends DatabaseExtensionConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.index-1.0";
    public static final String NAME = "index";
    private final long maxIndexIdlePeriod;
    private final FullTextIndexConfiguration fullTextIndex;

    public IndexDatabaseExtensionConfiguration(long maxIndexIdlePeriod, FullTextIndexConfiguration fullTextIndex) {
        super(NAME);

        Assert.notNull(fullTextIndex);

        this.maxIndexIdlePeriod = maxIndexIdlePeriod;
        this.fullTextIndex = fullTextIndex;
    }

    public long getIndexIdlePeriod() {
        return maxIndexIdlePeriod;
    }

    public FullTextIndexConfiguration getFullTextIndex() {
        return fullTextIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexDatabaseExtensionConfiguration))
            return false;

        IndexDatabaseExtensionConfiguration configuration = (IndexDatabaseExtensionConfiguration) o;
        return super.equals(configuration) && maxIndexIdlePeriod == configuration.maxIndexIdlePeriod &&
                fullTextIndex.equals(configuration.fullTextIndex);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(maxIndexIdlePeriod, fullTextIndex);
    }
}
