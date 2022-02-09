/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;
import com.exametrika.spi.exadb.objectdb.schema.IBlobFieldSchema;


/**
 * The {@link StreamBlobField} is a stream-oriented blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class StreamBlobField implements IField, IFieldObject {
    protected final BlobField field;

    public StreamBlobField(ISimpleField field) {
        Assert.notNull(field);

        this.field = new BlobField(field);
    }

    @Override
    public final boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public final boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public final IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public final INode getNode() {
        return field.getNode();
    }

    @Override
    public final <T> T getObject() {
        return (T) this;
    }

    @Override
    public final void setModified() {
        field.setModified();
    }

    @Override
    public final void onCreated(Object primaryKey, Object initializer) {
        Assert.isNull(primaryKey);

        field.onCreated(primaryKey, initializer);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public final void onOpened() {
        field.onOpened();
    }

    @Override
    public final void onDeleted() {
        field.onDeleted();
    }

    @Override
    public final void onUnloaded() {
        field.onUnloaded();
    }

    @Override
    public final void flush() {
        field.flush();
    }

    public final <T> T getStore() {
        IBlob blob = field.get();
        if (blob != null)
            return blob.getStore().getNode().getObject();
        else
            return null;
    }

    public final <T> void setStore(T store) {
        Assert.checkState(!field.isReadOnly());

        IBlob blob = field.get();
        if (blob != null) {
            blob.delete();
            blob = null;
        }

        if (store != null) {
            INodeObject nodeObject = (INodeObject) store;
            IBlobStoreField blobStoreField = nodeObject.getNode().getField(
                    ((IBlobFieldSchema) field.getSchema()).getStore().getIndex());

            Assert.isTrue(((IBlobFieldSchema) field.getSchema()).getStore() == blobStoreField.getSchema());

            blob = blobStoreField.createBlob();
        }

        field.set(blob);
    }

    public InputStream createInputStream() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);

        return new BlobInputStream(blob.createDeserialization());
    }

    public OutputStream createOutputStream() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!field.isReadOnly());

        return new BlobOutputStream(blob.createSerialization());
    }

    public void clear() {
        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!field.isReadOnly());

        IBlobSerialization serialization = blob.createSerialization();
        serialization.setPosition(serialization.getBeginPosition());
        serialization.removeRest();
    }

    protected abstract boolean isCompressed();

    private class BlobOutputStream extends OutputStream {
        private final boolean compressed = isCompressed();
        private IBlobSerialization serialization;

        public BlobOutputStream(IBlobSerialization serialization) {
            Assert.notNull(serialization);

            this.serialization = serialization;
        }

        @Override
        public void write(int b) throws IOException {
            Assert.supports(false);
        }

        @Override
        public void write(byte value[], int offset, int length) {
            Assert.checkState(serialization != null);

            ByteArray buffer;
            if (compressed) {
                serialization.writeInt(length);
                buffer = LZ4.compress(true, new ByteArray(value, offset, length));
            } else
                buffer = new ByteArray(value, offset, length);

            serialization.writeByteArray(buffer);
            field.getField().refresh();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            if (field.allowDeletion())
                serialization.removeRest();
            else
                serialization.updateEndPosition();

            serialization = null;
        }
    }

    private class BlobInputStream extends InputStream {
        private final boolean compressed = isCompressed();
        private IBlobDeserialization deserialization;
        private ByteArray buffer;
        private int readLength;
        private boolean eof;

        public BlobInputStream(IBlobDeserialization deserialization) {
            Assert.notNull(deserialization);

            this.deserialization = deserialization;
        }

        @Override
        public int read() throws IOException {
            Assert.supports(false);
            return 0;
        }

        @Override
        public int read(byte value[], int offset, int length) {
            Assert.checkState(deserialization != null);

            field.getField().refresh();

            if (eof)
                return -1;

            int res = 0;
            while (true) {
                if (buffer == null) {
                    if (deserialization.getPosition() != deserialization.getEndPosition()) {
                        if (compressed) {
                            int bufferLength = deserialization.readInt();
                            buffer = deserialization.readByteArray();
                            buffer = LZ4.decompress(buffer, bufferLength);
                        } else
                            buffer = deserialization.readByteArray();

                        readLength = 0;
                    } else
                        eof = true;
                }

                if (eof)
                    return res != 0 ? res : -1;

                int restLength = buffer.getLength() - readLength;
                if (length >= restLength) {
                    System.arraycopy(buffer.getBuffer(), buffer.getOffset() + readLength, value, offset, restLength);
                    length -= restLength;
                    offset += restLength;
                    res += restLength;
                    buffer = null;
                    readLength = 0;
                    if (length > 0)
                        continue;
                    else
                        return res;
                } else {
                    System.arraycopy(buffer.getBuffer(), buffer.getOffset() + readLength, value, offset, length);
                    res += length;
                    readLength += length;
                    return res;
                }
            }
        }

        @Override
        public void close() {
            deserialization = null;
        }
    }
}
