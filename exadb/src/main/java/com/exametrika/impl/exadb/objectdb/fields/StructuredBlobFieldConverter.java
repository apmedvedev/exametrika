/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link StructuredBlobFieldConverter} is a structured blob field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StructuredBlobFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IStructuredBlobField oldField = oldFieldInstance.getObject();
        IStructuredBlobField newField = newFieldInstance.getObject();

        INodeObject oldStore = (INodeObject) oldField.getStore();
        if (oldStore == null)
            return;

        ObjectNode oldNode = (ObjectNode) oldStore.getNode();
        ObjectSpace newSpace = ((ObjectNode) newField.getNode()).getSpace();

        IObjectNode newStore = SingleReferenceFieldConverter.findOrCreateNode(oldNode, newSpace, migrationSchema);
        if (newStore == null)
            return;

        newField.setStore(newStore.getObject());

        for (Object record : oldField.getRecords())
            newField.add(record);
    }
}
