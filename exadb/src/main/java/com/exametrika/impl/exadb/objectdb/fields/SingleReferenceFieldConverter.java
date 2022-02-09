/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.impl.exadb.objectdb.Node;
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
 * The {@link SingleReferenceFieldConverter} is a single reference field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SingleReferenceFieldConverter implements IFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        ISingleReferenceField<INodeObject> oldField = oldFieldInstance.getObject();
        ISingleReferenceField newField = newFieldInstance.getObject();

        if (oldField.get() == null)
            return;

        if (((IReferenceFieldSchema) oldFieldInstance.getSchema()).getExternalSpaceSchema() == null) {
            ObjectNode oldNode = (ObjectNode) oldField.get().getNode();
            ObjectSpace newSpace = ((ObjectNode) newField.getNode()).getSpace();
            IObjectNode newReference = findOrCreateNode(oldNode, newSpace, migrationSchema);
            if (newReference != null)
                newField.set(newReference.getObject());
        } else
            newField.set(oldField.get());
    }

    public static IObjectNode findOrCreateNode(ObjectNode oldNode, ObjectSpace newSpace, IFieldMigrationSchema migrationSchema) {
        IObjectSpace oldSpace = oldNode.getSpace();

        if (oldSpace.getRootNode() == oldNode.getObject())
            return (IObjectNode) ((INodeObject) newSpace.getRootNode()).getNode();
        else {
            INodeMigrationSchema nodeMigrationSchema = migrationSchema.getNode().getSpace().getNodes().get(oldNode.getSchema().getIndex());
            if (nodeMigrationSchema == null)
                return null;

            Object key = null;
            if (nodeMigrationSchema.getPrimaryField() != null) {
                IFieldMigrationSchema primaryFieldMigrationSchema = nodeMigrationSchema.getPrimaryField();
                IPrimaryFieldConverter converter = (IPrimaryFieldConverter) primaryFieldMigrationSchema.getConverter();
                IField oldRefField = ((Node) oldNode).getFieldInstance(primaryFieldMigrationSchema.getOldSchema().getIndex());
                key = converter.convert(oldRefField, primaryFieldMigrationSchema);
            }

            ObjectNode newNode = (ObjectNode) ((INodeObject) newSpace.findOrCreateMigratedNode(key, nodeMigrationSchema.getNewSchema())).getNode();
            if (key == null)
                ObjectSpaceSchema.migrateNode(migrationSchema.getNode().getSpace(), nodeMigrationSchema, oldNode, newNode);

            return newNode;
        }
    }
}
