/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Set;

import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link StringFieldSchemaConfiguration} is a configuration of string index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StringFieldSchemaConfiguration extends FieldSchemaConfiguration {
    public StringFieldSchemaConfiguration(String name, Set<Option> options, AnalyzerSchemaConfiguration analyzer) {
        super(name, options, analyzer);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StringFieldSchemaConfiguration))
            return false;

        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
