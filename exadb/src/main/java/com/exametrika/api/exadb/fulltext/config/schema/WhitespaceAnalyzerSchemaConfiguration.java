/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.util.Version;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;


/**
 * The {@link WhitespaceAnalyzerSchemaConfiguration} is a configuration of index whitespace analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class WhitespaceAnalyzerSchemaConfiguration extends AnalyzerSchemaConfiguration {
    @Override
    public IAnalyzer createAnalyzer() {
        return new IndexAnalyzer(new WhitespaceAnalyzer(Version.LUCENE_4_9));
    }

    @Override
    public boolean isSortable() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof WhitespaceAnalyzerSchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
