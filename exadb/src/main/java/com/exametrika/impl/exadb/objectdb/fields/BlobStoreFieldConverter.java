/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link BlobStoreFieldConverter} is a blob store field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobStoreFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
    }
}
