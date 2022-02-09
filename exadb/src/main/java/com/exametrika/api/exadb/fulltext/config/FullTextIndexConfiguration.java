/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;


/**
 * The {@link FullTextIndexConfiguration} is a configuration of full text index.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FullTextIndexConfiguration extends Configuration {
    private final long writerCommitPeriod;
    private final long searcherUpdatePeriod;
    private final long indexDeleteDelay;
    private final long bufferSizePerIndex;

    public FullTextIndexConfiguration() {
        writerCommitPeriod = 60000;
        searcherUpdatePeriod = 3000;
        indexDeleteDelay = 60000;
        bufferSizePerIndex = 16000000;
    }

    public FullTextIndexConfiguration(long writerCommitPeriod, long searcherUpdatePeriod, long indexDeleteDelay,
                                      long bufferSizePerIndex) {
        this.writerCommitPeriod = writerCommitPeriod;
        this.searcherUpdatePeriod = searcherUpdatePeriod;
        this.indexDeleteDelay = indexDeleteDelay;
        this.bufferSizePerIndex = bufferSizePerIndex;
    }

    public long getWriterCommitPeriod() {
        return writerCommitPeriod;
    }

    public long getSearcherUpdatePeriod() {
        return searcherUpdatePeriod;
    }

    public long getIndexDeleteDelay() {
        return indexDeleteDelay;
    }

    public long getBufferSizePerIndex() {
        return bufferSizePerIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FullTextIndexConfiguration))
            return false;

        FullTextIndexConfiguration configuration = (FullTextIndexConfiguration) o;
        return writerCommitPeriod == configuration.writerCommitPeriod && searcherUpdatePeriod == configuration.searcherUpdatePeriod &&
                indexDeleteDelay == configuration.indexDeleteDelay && bufferSizePerIndex == configuration.bufferSizePerIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(writerCommitPeriod, searcherUpdatePeriod, indexDeleteDelay, bufferSizePerIndex);
    }
}
