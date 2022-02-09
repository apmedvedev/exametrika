/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import com.exametrika.api.component.config.schema.IndexedVersionFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.component.fields.IndexedVersionField;
import com.exametrika.impl.exadb.objectdb.schema.SimpleFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link IndexedVersionFieldSchema} is a indexed version field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IndexedVersionFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    private final int indexTotalIndex;

    public IndexedVersionFieldSchema(IndexedVersionFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset);

        this.indexTotalIndex = indexTotalIndex;
    }

    @Override
    public int getIndexTotalIndex() {
        return indexTotalIndex;
    }

    @Override
    public IndexedVersionFieldSchemaConfiguration getConfiguration() {
        return (IndexedVersionFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new IndexedVersionField((ISimpleField) field);
    }
}
