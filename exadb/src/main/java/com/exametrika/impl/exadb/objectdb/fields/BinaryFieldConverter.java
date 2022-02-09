/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.fields.IBinaryField;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link BinaryFieldConverter} is a binary field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BinaryFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IBinaryField oldField = oldFieldInstance.getObject();
        IBinaryField newField = newFieldInstance.getObject();

        INodeObject oldStore = oldField.getStore();
        if (oldStore == null)
            return;

        ObjectNode oldNode = (ObjectNode) oldStore.getNode();
        ObjectSpace newSpace = ((ObjectNode) newField.getNode()).getSpace();

        IObjectNode newStore = SingleReferenceFieldConverter.findOrCreateNode(oldNode, newSpace, migrationSchema);
        if (newStore == null)
            return;

        newField.setStore(newStore.getObject());

        InputStream in = null;
        OutputStream out = null;
        try {
            in = oldField.createInputStream();
            out = newField.createOutputStream();
            IOs.copy(in, out);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            IOs.close(in);
            IOs.close(out);
        }
    }
}
