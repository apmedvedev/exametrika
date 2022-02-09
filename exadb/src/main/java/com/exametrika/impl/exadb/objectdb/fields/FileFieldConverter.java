/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IFileField;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link FileFieldConverter} is a file field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class FileFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        FileField oldField = oldFieldInstance.getObject();
        FileField newField = newFieldInstance.getObject();

        newField.setInitializer(oldField.getInitializer());

        long oldFileSize = oldField.getFile().getSize();
        long k = 0;
        for (long i = 0; i < oldFileSize; ) {
            IRawPage page = oldField.getPage(k);

            ByteArray buffer = page.getReadRegion().readByteArray(0, page.getSize());
            write(newField, buffer, i);
            i += page.getSize();
            k++;
        }
    }

    private void write(IFileField field, ByteArray buffer, long fileOffset) {
        int pageSize = field.getFile().getPageSize();
        int pageOffset = (int) (fileOffset % pageSize);
        long pageIndex = fileOffset / pageSize;

        IRawWriteRegion region = field.getPage(pageIndex).getWriteRegion();
        if (pageOffset + buffer.getLength() > pageSize) {
            int length = buffer.getLength();
            byte[] buf = buffer.getBuffer();
            int offset = buffer.getOffset();
            while (length > 0) {
                if (pageOffset + length > pageSize) {
                    int l = pageSize - pageOffset;

                    region.writeByteArray(pageOffset, new ByteArray(buf, offset, l));

                    length -= l;
                    offset += l;

                    pageIndex++;
                    pageOffset = 0;

                    region = field.getPage(pageIndex).getWriteRegion();
                } else {
                    region.writeByteArray(pageOffset, new ByteArray(buf, offset, length));
                    length = 0;
                }
            }
        } else
            region.writeByteArray(pageOffset, buffer);
    }
}
