/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNode;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;
import com.exametrika.spi.exadb.objectdb.schema.INodeMigrationSchema;


/**
 * The {@link ReferenceFieldConverter} is a reference field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ReferenceFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IReferenceField<INodeObject> oldField = oldFieldInstance.getObject();
        IReferenceField newField = newFieldInstance.getObject();

        IObjectSpace oldSpace = ((IObjectNode) oldField.getNode()).getSpace();
        ObjectSpace newSpace = ((ObjectNode) newField.getNode()).getSpace();

        if (((IReferenceFieldSchema) oldFieldInstance.getSchema()).getExternalSpaceSchema() == null) {
            for (INodeObject oldNodeObject : oldField) {
                ObjectNode oldNode = (ObjectNode) oldNodeObject.getNode();

                if (oldSpace.getRootNode() == oldNode.getObject())
                    newField.add(newSpace.getRootNode());
                else {
                    INodeMigrationSchema nodeMigrationSchema = migrationSchema.getNode().getSpace().getNodes().get(oldNode.getSchema().getIndex());
                    if (nodeMigrationSchema == null)
                        continue;

                    Object key = null;
                    if (nodeMigrationSchema.getPrimaryField() != null) {
                        IFieldMigrationSchema primaryFieldMigrationSchema = nodeMigrationSchema.getPrimaryField();
                        IPrimaryFieldConverter converter = (IPrimaryFieldConverter) primaryFieldMigrationSchema.getConverter();
                        IField oldRefField = oldNode.getFieldInstance(primaryFieldMigrationSchema.getOldSchema().getIndex());
                        key = converter.convert(oldRefField, primaryFieldMigrationSchema);
                    }

                    INodeObject newNode = newSpace.findOrCreateMigratedNode(key, nodeMigrationSchema.getNewSchema());
                    if (key == null)
                        ObjectSpaceSchema.migrateNode(migrationSchema.getNode().getSpace(), nodeMigrationSchema, oldNode,
                                (ObjectNode) newNode.getNode());
                    newField.add(newNode);
                }
            }
        } else {
            for (INodeObject oldNodeObject : oldField)
                newField.add(oldNodeObject);
        }
    }
}
