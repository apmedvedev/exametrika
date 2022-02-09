/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.exametrika.api.exadb.objectdb.config.schema.StringSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.StringSequenceField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link StringSequenceFieldSchema} is a string field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StringSequenceFieldSchema extends NumericSequenceFieldSchema implements IFieldSchema {
    private final NumberFormat numberFormat;

    public StringSequenceFieldSchema(StringSequenceFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        if (configuration.getNumberFormat() != null)
            numberFormat = new DecimalFormat(configuration.getNumberFormat());
        else
            numberFormat = null;
    }

    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    @Override
    public StringSequenceFieldSchemaConfiguration getConfiguration() {
        return (StringSequenceFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new StringSequenceField((ISimpleField) field);
    }
}
