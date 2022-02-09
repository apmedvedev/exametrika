/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaBuilder;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;


/**
 * The {@link StringFieldSchemaBuilder} is a builder of configuration of string or text field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StringFieldSchemaBuilder extends FieldSchemaBuilder {
    private final DocumentSchemaBuilder parent;
    private final String name;
    private final Set<Option> options = Enums.noneOf(Option.class);
    private final boolean string;
    private AnalyzerSchemaConfiguration analyzer;

    public StringFieldSchemaBuilder(DocumentSchemaBuilder parent, String name, boolean string) {
        Assert.notNull(parent);
        Assert.notNull(name);

        this.parent = parent;
        this.name = name;
        this.string = string;
    }

    public StringFieldSchemaBuilder indexed() {
        options.add(Option.INDEXED);
        return this;
    }

    public StringFieldSchemaBuilder stored() {
        options.add(Option.STORED);
        return this;
    }

    public StringFieldSchemaBuilder tokenized() {
        options.add(Option.TOKENIZED_AND_INDEXED);
        return this;
    }

    public StringFieldSchemaBuilder indexDocuments() {
        options.add(Option.INDEX_DOCUMENTS);
        return this;
    }

    public StringFieldSchemaBuilder indexDocumentsAndFrequences() {
        options.add(Option.INDEX_DOCUMENTS_AND_FREQUENCES);
        return this;
    }

    public StringFieldSchemaBuilder indexDocumentsFrequencesAndPositions() {
        options.add(Option.INDEX_DOCUMENTS_FREQUENCES_AND_POSITIONS);
        return this;
    }

    public StringFieldSchemaBuilder indexDocumentsFrequencesPositionsAndOffsets() {
        options.add(Option.INDEX_DOCUMENTS_FREQUENCES_POSITIONS_AND_OFFSETS);
        return this;
    }

    public StringFieldSchemaBuilder storeTermVectors() {
        options.add(Option.STORE_TERM_VECTORS);
        return this;
    }

    public StringFieldSchemaBuilder storeTermVectorOffsets() {
        options.add(Option.STORE_TERM_VECTOR_OFFSETS);
        return this;
    }

    public StringFieldSchemaBuilder storeTermVectorPositions() {
        options.add(Option.STORE_TERM_VECTOR_POSITIONS);
        return this;
    }

    public StringFieldSchemaBuilder storeTermVectorPayloads() {
        options.add(Option.STORE_TERM_VECTOR_PAYLOADS);
        return this;
    }

    public StringFieldSchemaBuilder omitNorms() {
        options.add(Option.OMIT_NORMS);
        return this;
    }

    public StringFieldSchemaBuilder analyzer(AnalyzerSchemaConfiguration analyzer) {
        Assert.notNull(analyzer);

        this.analyzer = analyzer;
        return this;
    }

    public DocumentSchemaBuilder end() {
        return parent;
    }

    @Override
    public FieldSchemaConfiguration toConfiguration() {
        if (string)
            return new StringFieldSchemaConfiguration(name, Enums.copyOf(options), analyzer);
        else
            return new TextFieldSchemaConfiguration(name, Enums.copyOf(options), analyzer);
    }
}
