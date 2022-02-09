/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaBuilder;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link NumericFieldSchemaBuilder} is a builder of configuration of numeric field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NumericFieldSchemaBuilder extends FieldSchemaBuilder {
    private final DocumentSchemaBuilder parent;
    private final String name;
    private DataType type = DataType.INT;
    private boolean stored;
    private boolean indexed;
    private int precisionStep = 4;

    public NumericFieldSchemaBuilder(DocumentSchemaBuilder parent, String name) {
        Assert.notNull(parent);
        Assert.notNull(name);

        this.parent = parent;
        this.name = name;
    }

    public NumericFieldSchemaBuilder stored() {
        stored = true;
        return this;
    }

    public NumericFieldSchemaBuilder indexed() {
        indexed = true;
        return this;
    }

    public NumericFieldSchemaBuilder type(DataType type) {
        Assert.notNull(type);

        this.type = type;
        return this;
    }

    public NumericFieldSchemaBuilder precisionStep(int value) {
        precisionStep = value;
        return this;
    }

    public DocumentSchemaBuilder end() {
        return parent;
    }

    @Override
    public FieldSchemaConfiguration toConfiguration() {
        return new NumericFieldSchemaConfiguration(name, type, stored, indexed, precisionStep);
    }
}
