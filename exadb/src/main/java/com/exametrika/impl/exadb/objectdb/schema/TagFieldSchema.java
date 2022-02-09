/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.TagFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.exadb.objectdb.fields.TagField;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link TagFieldSchema} is a tag field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TagFieldSchema extends ComplexFieldSchema implements IFieldSchema {
    private final int indexTotalIndex;

    public TagFieldSchema(TagFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset);

        this.indexTotalIndex = indexTotalIndex;
    }

    @Override
    public int getIndexTotalIndex() {
        return indexTotalIndex;
    }

    @Override
    public TagFieldSchemaConfiguration getConfiguration() {
        return (TagFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new TagField((IComplexField) field);
    }
}
