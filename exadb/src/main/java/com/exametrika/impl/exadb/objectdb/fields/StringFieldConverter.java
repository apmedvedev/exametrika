/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link StringFieldConverter} is a string field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StringFieldConverter implements IPrimaryFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IStringField oldField = oldFieldInstance.getObject();
        IStringField newField = newFieldInstance.getObject();

        newField.set(oldField.get());
    }

    @Override
    public Object convert(IField oldFieldInstance, IFieldMigrationSchema migrationSchema) {
        IPrimaryField oldField = oldFieldInstance.getObject();
        return oldField.getKey();
    }
}
