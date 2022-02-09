/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.NumericSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.NumericSequenceField;
import com.exametrika.spi.exadb.jobs.ISchedulePeriod;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link NumericSequenceFieldSchema} is a numeric sequence field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NumericSequenceFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    private final ISchedulePeriod period;

    public NumericSequenceFieldSchema(NumericSequenceFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        if (configuration.getPeriod() != null)
            period = configuration.getPeriod().createPeriod();
        else
            period = null;
    }

    public ISchedulePeriod getPeriod() {
        return period;
    }

    @Override
    public NumericSequenceFieldSchemaConfiguration getConfiguration() {
        return (NumericSequenceFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new NumericSequenceField((ISimpleField) field);
    }
}
