/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link SingleReferenceField} is a single reference field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SingleReferenceField implements ISingleReferenceField, IFieldObject {
    public static final int HEADER_SIZE = 12;// refId(long) + deletionCount(int)
    private static final int REF_ID_OFFSET = 0;
    private static final int DELETION_COUNT_OFFSET = 8;
    private final ISimpleField field;
    private Node reference;
    private int deletionCount;
    private boolean modified;

    public SingleReferenceField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public IReferenceFieldSchema getSchema() {
        return (IReferenceFieldSchema) field.getSchema();
    }

    @Override
    public Node getNode() {
        return (Node) field.getNode();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        modified = true;
        field.setModified();
    }

    @Override
    public Object get() {
        if (reference != null && !reference.isStale() && reference.getDeletionCount() == deletionCount)
            return reference.getObject();

        return readReference();
    }

    @Override
    public void set(Object value) {
        Node node;
        if (value != null) {
            Assert.isTrue(value instanceof INodeObject);
            node = (Node) ((INodeObject) value).getNode();
        } else
            node = null;

        set(node, true);
    }

    public void set(Node node, boolean correctBidirectional) {
        Assert.checkState(!field.isReadOnly());
        get();

        if (reference == node)
            return;

        IReferenceFieldSchema schema = getSchema();
        if (correctBidirectional && reference != null && schema.getFieldReference() != null) {
            IReferenceFieldSchema fieldReference = schema.getFieldReference();
            Assert.isTrue(fieldReference.getParent() == reference.getSchema());
            boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

            IField field = reference.getField(fieldReference.getIndex());
            if (single)
                ((SingleReferenceField) field).set(null, false);
            else
                ((ReferenceField) field).remove(getNode(), false);
        }

        if (node != null) {
            Assert.isTrue(node.isCached());
            Assert.isTrue(!node.isDeleted());
            Assert.isTrue(!node.isStale());
            Assert.isTrue(((Node) field.getNode()).canReference(schema, node));

            if (schema.getFieldReference() != null) {
                if (correctBidirectional) {
                    IReferenceFieldSchema fieldReference = schema.getFieldReference();
                    Assert.isTrue(fieldReference.getParent() == node.getSchema());
                    boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

                    IField field = node.getField(fieldReference.getIndex());
                    if (single)
                        ((SingleReferenceField) field).set(getNode(), false);
                    else
                        ((ReferenceField) field).add(getNode(), 0, false);
                }
            } else {
                Set<INodeSchema> nodeReferences = schema.getNodeReferences();
                Assert.isTrue(nodeReferences == null || nodeReferences.contains(node.getSchema()));
            }

            reference = node;
            deletionCount = node.getDeletionCount();
        } else {
            reference = null;
            deletionCount = 0;
        }

        setModified();
    }


    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
        if (((SingleReferenceFieldSchemaConfiguration) getSchema().getConfiguration()).isOwning()) {
            get();
            if (reference != null)
                reference.delete();
        }
        reference = null;
        deletionCount = 0;
        modified = false;
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        long refId;
        if (reference != null)
            refId = reference.getRefId();
        else
            refId = 0;

        IRawWriteRegion region = field.getWriteRegion();
        region.writeLong(REF_ID_OFFSET, refId);
        region.writeInt(DELETION_COUNT_OFFSET, deletionCount);
        modified = false;
    }

    private Object readReference() {
        if (modified) {
            if (reference == null)
                return reference;

            if (reference.isStale())
                reference = open(reference.getRefId());

            return reference;
        }

        if (reference == null || reference.isStale()) {
            IRawReadRegion region = field.getReadRegion();
            long refId = region.readLong(REF_ID_OFFSET);
            deletionCount = region.readInt(DELETION_COUNT_OFFSET);
            if (refId == 0)
                return null;

            reference = open(refId);
        }

        if (reference == null || reference.getDeletionCount() != deletionCount) {
            if (!field.isReadOnly()) {
                field.getWriteRegion().writeLong(REF_ID_OFFSET, 0);
                reference = null;
                deletionCount = 0;
            }

            return null;
        }

        return reference.getObject();
    }

    private Node open(long refId) {
        IReferenceFieldSchema schema = getSchema();
        IObjectSpaceSchema externalSpaceSchema = schema.getExternalSpaceSchema();
        if (externalSpaceSchema == null) {
            Node node = (Node) field.getNode();
            return node.open(refId);
        } else {
            INodeObject nodeObject = (INodeObject) externalSpaceSchema.getSpace().findNodeById(refId);
            if (nodeObject != null)
                return (Node) nodeObject.getNode();
            else
                return null;
        }
    }
}
