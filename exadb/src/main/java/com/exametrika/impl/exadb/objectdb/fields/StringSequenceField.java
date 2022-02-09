/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.config.schema.StringSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStringSequenceField;
import com.exametrika.impl.exadb.objectdb.schema.StringSequenceFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link StringSequenceField} is a string field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StringSequenceField extends NumericSequenceField implements IStringSequenceField {
    public StringSequenceField(ISimpleField field) {
        super(field);
    }

    @Override
    public String getNextString() {
        StringSequenceFieldSchema schema = (StringSequenceFieldSchema) getSchema();
        StringSequenceFieldSchemaConfiguration configuration = schema.getConfiguration();

        long value = getNext();
        String str;
        if (schema.getNumberFormat() != null)
            str = schema.getNumberFormat().format(value);
        else
            str = Long.toString(value);

        return configuration.getPrefix() + str + configuration.getSuffix();
    }
}
