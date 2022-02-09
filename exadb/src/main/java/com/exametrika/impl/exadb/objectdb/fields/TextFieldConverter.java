/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.fields.ITextField;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link TextFieldConverter} is a text field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TextFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        ITextField oldField = oldFieldInstance.getObject();
        ITextField newField = newFieldInstance.getObject();

        INodeObject oldStore = oldField.getStore();
        if (oldStore == null)
            return;

        ObjectNode oldNode = (ObjectNode) oldStore.getNode();
        ObjectSpace newSpace = ((ObjectNode) newField.getNode()).getSpace();

        IObjectNode newStore = SingleReferenceFieldConverter.findOrCreateNode(oldNode, newSpace, migrationSchema);
        if (newStore == null)
            return;

        newField.setStore(newStore.getObject());

        Reader in = null;
        Writer out = null;
        try {
            in = oldField.createReader();
            out = newField.createWriter();
            IOs.copy(in, out);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            IOs.close(in);
            IOs.close(out);
        }
    }
}
