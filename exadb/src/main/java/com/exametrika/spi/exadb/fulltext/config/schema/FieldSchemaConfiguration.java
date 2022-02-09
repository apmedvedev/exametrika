/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import java.util.Set;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.fulltext.schema.DocumentSchema;
import com.exametrika.impl.exadb.fulltext.schema.FieldSchema;


/**
 * The {@link FieldSchemaConfiguration} is a configuration of index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldSchemaConfiguration extends Configuration {
    private final String name;
    private final Set<Option> options;
    private final AnalyzerSchemaConfiguration analyzer;

    public enum Option {
        STORED,

        INDEXED,

        TOKENIZED_AND_INDEXED,

        INDEX_DOCUMENTS,

        INDEX_DOCUMENTS_AND_FREQUENCES,

        INDEX_DOCUMENTS_FREQUENCES_AND_POSITIONS,

        INDEX_DOCUMENTS_FREQUENCES_POSITIONS_AND_OFFSETS,

        STORE_TERM_VECTORS,

        STORE_TERM_VECTOR_OFFSETS,

        STORE_TERM_VECTOR_POSITIONS,

        STORE_TERM_VECTOR_PAYLOADS,

        OMIT_NORMS,
    }

    public FieldSchemaConfiguration(String name, Set<Option> options, AnalyzerSchemaConfiguration analyzer) {
        Assert.notNull(name);
        Assert.notNull(options);

        this.name = name;
        this.options = Immutables.wrap(options);
        this.analyzer = analyzer;
    }

    public String getName() {
        return name;
    }

    public Set<Option> getOptions() {
        return options;
    }

    public AnalyzerSchemaConfiguration getAnalyzer() {
        return analyzer;
    }

    public IFieldSchema createSchema(IDocumentSchema document, int index) {
        return new FieldSchema((DocumentSchema) document, this, index);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldSchemaConfiguration))
            return false;

        FieldSchemaConfiguration configuration = (FieldSchemaConfiguration) o;
        return name.equals(configuration.name) && options.equals(configuration.options) && Objects.equals(analyzer, configuration.analyzer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, options, analyzer);
    }

    @Override
    public String toString() {
        return name;
    }
}
