/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;


/**
 * The {@link StandardAnalyzerSchemaConfiguration} is a configuration of index standard analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardAnalyzerSchemaConfiguration extends AnalyzerSchemaConfiguration {
    @Override
    public IAnalyzer createAnalyzer() {
        return new IndexAnalyzer(new StandardAnalyzer(Version.LUCENE_4_9));
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StandardAnalyzerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
