/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link ReferenceField} is a reference field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ReferenceField implements IReferenceField, IFieldObject {
    private static final int REFERENCE_CACHE_SIZE = Memory.getShallowSize(Reference.class);
    private static final int ELEMENT_HEADER_SIZE = 16;// refId(long) + deletionCount(int) + flags(int)
    private final IComplexField field;
    private Set<Reference> readReferences;
    private Set<Reference> addedReferences;
    private Set<Reference> removedReferences;
    private int modCount;
    private long currentAreaBlockIndex;
    private int currentAreaOffset;
    private int lastCacheSize;
    private boolean fullyRead;

    public ReferenceField(IComplexField field) {
        Assert.notNull(field);

        this.field = field;
        field.setAutoRemoveUnusedAreas();
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
    public <T> T get() {
        return (T) this;
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        field.setModified();
    }

    @Override
    public IReferenceIterator<Object> iterator() {
        return new ReferenceIterator(++modCount);
    }

    @Override
    public void add(Object value) {
        add(value, 0);
    }

    @Override
    public void add(Object value, int flags) {
        Assert.isTrue(value instanceof INodeObject);

        Node node = (Node) ((INodeObject) value).getNode();
        add(node, flags, true);
    }

    public void add(Node node, int flags, boolean correctBidirectional) {
        Assert.checkState(!field.isReadOnly());

        IReferenceFieldSchema schema = getSchema();

        Assert.isTrue(node.isCached());
        Assert.isTrue(!node.isDeleted());
        Assert.isTrue(!node.isStale());
        Assert.isTrue(((Node) field.getNode()).canReference(schema, node));

        field.refresh();

        if (((ReferenceFieldSchemaConfiguration) schema.getConfiguration()).isStableOrder())
            readFully();

        if (correctBidirectional && schema.getFieldReference() != null) {
            IReferenceFieldSchema fieldReference = schema.getFieldReference();
            Assert.isTrue(fieldReference.getParent() == node.getSchema());
            boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

            IField field = node.getField(fieldReference.getIndex());
            if (single)
                ((SingleReferenceField) field).set(getNode(), false);
            else
                ((ReferenceField) field).add(getNode(), flags, false);
        } else {
            Set<INodeSchema> nodeReferences = schema.getNodeReferences();
            Assert.isTrue(nodeReferences == null || nodeReferences.contains(node.getSchema()));
        }

        if (addedReferences == null)
            addedReferences = new LinkedHashSet<Reference>();

        Reference reference = new Reference(node, node.getDeletionCount(), flags);
        addedReferences.add(reference);

        if (removedReferences != null)
            removedReferences.remove(reference);

        modCount++;
        setModified();
    }

    @Override
    public void remove(Object value) {
        Assert.isTrue(value instanceof INodeObject);

        Node node = (Node) ((INodeObject) value).getNode();
        remove(node, true);
    }

    public void remove(Node node, boolean correctBidirectional) {
        Assert.supports(field.allowDeletion());
        Assert.checkState(!field.isReadOnly());

        IReferenceFieldSchema schema = getSchema();

        Assert.isTrue(node.isCached());
        Assert.isTrue(!node.isDeleted());
        Assert.isTrue(!node.isStale());
        Assert.isTrue(((Node) field.getNode()).canReference(schema, node));

        if (correctBidirectional && schema.getFieldReference() != null) {
            IReferenceFieldSchema fieldReference = schema.getFieldReference();
            Assert.isTrue(fieldReference.getParent() == node.getSchema());
            boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

            IField field = node.getField(fieldReference.getIndex());
            if (single)
                ((SingleReferenceField) field).set(null, false);
            else
                ((ReferenceField) field).remove(getNode(), false);
        }

        field.refresh();

        if (removedReferences == null)
            removedReferences = new HashSet<Reference>();

        Reference reference = new Reference(node, node.getDeletionCount(), 0);
        removedReferences.add(reference);

        if (addedReferences != null)
            addedReferences.remove(reference);
        if (readReferences != null)
            readReferences.remove(reference);

        modCount++;
        setModified();
    }

    @Override
    public void clear() {
        Assert.supports(field.allowDeletion());

        IReferenceFieldSchema schema = getSchema();
        if (schema.getFieldReference() != null) {
            IReferenceFieldSchema fieldReference = schema.getFieldReference();
            boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

            for (Object reference : this) {
                Node node = (Node) ((INodeObject) reference).getNode();
                Assert.isTrue(fieldReference.getParent() == node.getSchema());
                IField field = node.getField(fieldReference.getIndex());
                if (single)
                    ((SingleReferenceField) field).set(null, false);
                else
                    ((ReferenceField) field).remove(getNode(), false);
            }
        }

        IFieldSerialization serialization = field.createSerialization();
        int count = Constants.COMPLEX_FIELD_AREA_DATA_SIZE / ELEMENT_HEADER_SIZE;
        for (int i = 0; i < count; i++) {
            serialization.writeLong(0);
            serialization.writeInt(0);
            serialization.writeInt(0);
        }

        serialization.removeRest();
        addedReferences = null;
        removedReferences = null;
        readReferences = null;
        modCount++;
        currentAreaBlockIndex = -1;
        currentAreaOffset = 0;

        setModified();
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);
        fullyRead = true;
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
        if (((ReferenceFieldSchemaConfiguration) getSchema().getConfiguration()).isOwning()) {
            for (Object reference : this)
                ((INodeObject) reference).getNode().delete();
        }

        readReferences = null;
        addedReferences = null;
        removedReferences = null;
        currentAreaBlockIndex = -1;
        currentAreaOffset = 0;
    }

    @Override
    public void onUnloaded() {
        readReferences = null;
    }

    @Override
    public void flush() {
        if (addedReferences == null && removedReferences == null)
            return;

        Iterator<Reference> itAdded = null;
        if (addedReferences != null) {
            if (removedReferences != null)
                addedReferences.removeAll(removedReferences);
            itAdded = addedReferences.iterator();
        }

        IFieldSerialization serialization = field.createSerialization();
        if (!allowDeletion())
            serialization.setPosition(serialization.getLastAreaId(), 0);

        while (serialization.hasNext(ELEMENT_HEADER_SIZE)) {
            IFieldSerialization markedSerialization = serialization.clone();
            long refId = serialization.readLong();
            int deletionCount = serialization.readInt();
            int flags = serialization.readInt();
            if (refId == 0) {
                addReference(markedSerialization, itAdded, true);
                continue;
            }

            if (removedReferences != null && removedReferences.contains(new Reference(refId, deletionCount, flags))) {
                if (!addReference(markedSerialization, itAdded, false)) {
                    markedSerialization.writeLong(0);
                    serialization.decrementCurrentAreaUsageCount();
                }
            }
        }

        while (addReference(serialization, itAdded, true))
            ;

        addedReferences = null;
        removedReferences = null;
        modCount++;

        updateCacheSize();
    }

    private boolean addReference(IFieldSerialization serialization, Iterator<Reference> it, boolean incrementUsageCount) {
        if (it == null || !it.hasNext())
            return false;

        Reference reference = it.next();
        serialization.writeLong(reference.refId);
        serialization.writeInt(reference.deletionCount);
        serialization.writeInt(reference.flags);

        if (incrementUsageCount)
            serialization.incrementCurrentAreaUsageCount();

        if (readReferences == null)
            readReferences = new LinkedHashSet<Reference>();

        readReferences.add(reference);
        return true;
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

    private void readFully() {
        if (fullyRead)
            return;

        for (Iterator<Object> it = iterator(); it.hasNext(); )
            it.next();
    }

    private void updateCacheSize() {
        int cacheSize = 0;
        if (readReferences != null)
            cacheSize = CacheSizes.getLinkedHashSetCacheSize(readReferences) + REFERENCE_CACHE_SIZE * readReferences.size();

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }

    private class ReferenceIterator implements IReferenceIterator<Object> {
        private IFieldDeserialization deserialization;
        private Iterator<Reference> readIterator;
        private Iterator<Reference> addedIterator;
        private final int modCount;
        private Reference currentReference;
        private Reference nextReference;

        public ReferenceIterator(int modCount) {
            this.modCount = modCount;
            this.readIterator = readReferences != null ? readReferences.iterator() : null;
            this.addedIterator = addedReferences != null ? addedReferences.iterator() : null;
            this.nextReference = findNext();
        }

        @Override
        public boolean hasNext() {
            return nextReference != null;
        }

        @Override
        public Object next() {
            Assert.isTrue(nextReference != null);

            if (modCount != ReferenceField.this.modCount)
                throw new ConcurrentModificationException();

            currentReference = nextReference;
            nextReference = findNext();

            if (currentReference.node.isStale())
                currentReference.node = open(currentReference.refId);

            return currentReference.node.getObject();
        }

        @Override
        public void remove() {
            Assert.supports(allowDeletion());
            Assert.checkState(!isReadOnly());
            Assert.isTrue(currentReference != null);
            if (modCount != ReferenceField.this.modCount)
                throw new ConcurrentModificationException();

            IReferenceFieldSchema schema = getSchema();
            if (schema.getFieldReference() != null) {
                IReferenceFieldSchema fieldReference = schema.getFieldReference();
                Assert.isTrue(fieldReference.getParent() == currentReference.node.getSchema());
                boolean single = fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration;

                IField field = currentReference.node.getField(fieldReference.getIndex());
                if (single)
                    ((SingleReferenceField) field).set(null, false);
                else
                    ((ReferenceField) field).remove(getNode(), false);
            }

            if (removedReferences == null)
                removedReferences = new HashSet<Reference>();

            removedReferences.add(currentReference);
            setModified();
        }

        @Override
        public int getFlags() {
            Assert.notNull(currentReference);
            return currentReference.flags;
        }

        private Reference findNext() {
            Node node = ((Node) field.getNode());

            Reference reference = null;
            while (readIterator != null) {
                if (!readIterator.hasNext()) {
                    readIterator = null;
                    break;
                }

                reference = readIterator.next();
                if (removedReferences != null && removedReferences.contains(reference)) {
                    readIterator.remove();
                    continue;
                }

                node.refresh();

                if (reference.node.isStale())
                    reference.node = open(reference.refId);
                if (reference.node != null && reference.node.getDeletionCount() == reference.deletionCount)
                    return reference;
                else {
                    readIterator.remove();

                    if (!isReadOnly()) {
                        if (removedReferences == null)
                            removedReferences = new HashSet<Reference>();

                        removedReferences.add(reference);
                        setModified();
                    }
                }
            }

            while (addedIterator != null) {
                if (!addedIterator.hasNext()) {
                    addedIterator = null;
                    break;
                }

                reference = addedIterator.next();
                if (removedReferences != null && removedReferences.contains(reference)) {
                    addedIterator.remove();
                    continue;
                }

                node.refresh();

                if (reference.node.isStale())
                    reference.node = open(reference.refId);
                if (reference.node != null && reference.node.getDeletionCount() == reference.deletionCount)
                    return reference;
                else
                    addedIterator.remove();
            }

            if (fullyRead || currentAreaBlockIndex == -1)
                return null;

            if (deserialization == null) {
                deserialization = field.createDeserialization();
                deserialization.setPosition(currentAreaBlockIndex, currentAreaOffset);
            }

            while (deserialization.hasNext(ELEMENT_HEADER_SIZE)) {
                node.refresh();

                long refId = deserialization.readLong();
                int deletionCount = deserialization.readInt();
                int flags = deserialization.readInt();

                currentAreaBlockIndex = deserialization.getAreaId();
                currentAreaOffset = deserialization.getAreaOffset();

                if (refId == 0)
                    continue;

                reference = new Reference(refId, deletionCount, flags);
                reference.node = open(refId);
                if (removedReferences != null && removedReferences.contains(reference))
                    continue;

                if (readReferences != null && readReferences.contains(reference))
                    continue;

                if (reference.node != null && reference.node.getDeletionCount() == reference.deletionCount) {
                    if (readReferences == null)
                        readReferences = new LinkedHashSet<Reference>();

                    readReferences.add(reference);

                    updateCacheSize();
                    return reference;
                } else if (!isReadOnly()) {
                    if (removedReferences == null)
                        removedReferences = new HashSet<Reference>();

                    removedReferences.add(reference);
                    setModified();
                }
            }

            currentAreaBlockIndex = -1;
            fullyRead = true;

            return null;
        }
    }

    private static class Reference {
        private final long refId;
        private final int deletionCount;
        private final int flags;
        private Node node;

        public Reference(Node node, int deletionCount, int flags) {
            this.refId = node.getRefId();
            this.deletionCount = deletionCount;
            this.flags = flags;
            this.node = node;
        }

        public Reference(long refId, int deletionCount, int flags) {
            this.refId = refId;
            this.deletionCount = deletionCount;
            this.flags = flags;
            this.node = null;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Reference))
                return false;

            Reference reference = (Reference) o;
            return refId == reference.refId && deletionCount == reference.deletionCount;
        }

        @Override
        public int hashCode() {
            return 31 * (int) (refId ^ (refId >>> 32)) + deletionCount;
        }
    }
}
