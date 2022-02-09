/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.util.Version;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;


/**
 * The {@link SimpleAnalyzerSchemaConfiguration} is a configuration of index simple analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleAnalyzerSchemaConfiguration extends AnalyzerSchemaConfiguration {
    @Override
    public IAnalyzer createAnalyzer() {
        return new IndexAnalyzer(new SimpleAnalyzer(Version.LUCENE_4_9));
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleAnalyzerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
