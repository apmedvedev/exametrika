/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext;

import org.apache.lucene.analysis.Analyzer;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexAnalyzer} is an index analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexAnalyzer implements IAnalyzer {
    private final Analyzer analyzer;

    public IndexAnalyzer(Analyzer analyzer) {
        Assert.notNull(analyzer);

        this.analyzer = analyzer;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }
}
