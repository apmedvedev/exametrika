/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link JsonFieldConverter} is a JSON field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IJsonField oldField = oldFieldInstance.getObject();
        IJsonField newField = newFieldInstance.getObject();

        newField.set((IJsonCollection) oldField.get());
    }
}
