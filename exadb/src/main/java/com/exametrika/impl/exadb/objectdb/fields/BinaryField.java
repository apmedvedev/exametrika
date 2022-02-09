/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBinaryField;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.objectdb.schema.BinaryFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link BinaryField} is a binary blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BinaryField extends StreamBlobField implements IBinaryField {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final byte MAGIC = 0x71;

    public BinaryField(ISimpleField field) {
        super(field);
    }

    @Override
    public <T> T get() {
        return (T) createOutputStream();
    }

    @Override
    public OutputStream createOutputStream() {
        return new BufferedOutputStream(super.createOutputStream());
    }

    @Override
    public <T> T read() {
        field.getField().refresh();

        IBlob blob = field.get();
        Assert.checkState(blob != null);

        IBlobDeserialization fieldDeserialization = blob.createDeserialization();
        fieldDeserialization.setPosition(blob.getBeginPosition());

        if (fieldDeserialization.getPosition() == fieldDeserialization.getEndPosition())
            return null;

        if (fieldDeserialization.readByte() != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fieldDeserialization.getBlob()));

        BinaryFieldSchemaConfiguration configuration = ((BinaryFieldSchemaConfiguration) getSchema().getConfiguration());

        ByteArray buffer;
        if (!configuration.isCompressed())
            buffer = fieldDeserialization.readByteArray();
        else {
            int decompressedLength = fieldDeserialization.readInt();
            ByteArray compressedBuffer = fieldDeserialization.readByteArray();
            buffer = LZ4.decompress(compressedBuffer, decompressedLength);
        }

        ISerializationRegistry registry = ((BinaryFieldSchema) field.getSchema()).getSerializationRegistry();
        ByteInputStream stream = new ByteInputStream(buffer.getBuffer(), buffer.getOffset(), buffer.getLength());
        Deserialization deserialization = new Deserialization(registry, stream);
        return deserialization.readObject();
    }

    @Override
    public <T> void write(T value) {
        field.getField().refresh();

        IBlob blob = field.get();
        Assert.checkState(blob != null);
        Assert.checkState(!isReadOnly());

        IBlobSerialization fieldSerialization = blob.createSerialization();
        fieldSerialization.setPosition(fieldSerialization.getBeginPosition());

        if (blob.getStore().allowDeletion())
            fieldSerialization.removeRest();

        fieldSerialization.writeByte(MAGIC);

        BinaryFieldSchemaConfiguration configuration = ((BinaryFieldSchemaConfiguration) getSchema().getConfiguration());

        ByteOutputStream stream = new ByteOutputStream();
        ISerializationRegistry registry = ((BinaryFieldSchema) field.getSchema()).getSerializationRegistry();
        Serialization serialization = new Serialization(registry, true, stream);
        serialization.writeObject(value);

        ByteArray buffer = new ByteArray(stream.getBuffer(), 0, stream.getLength());

        if (!configuration.isCompressed())
            fieldSerialization.writeByteArray(buffer);
        else {
            ByteArray compressedBuffer = LZ4.compress(true, buffer);

            fieldSerialization.writeInt(buffer.getLength());
            fieldSerialization.writeByteArray(compressedBuffer);

            buffer = compressedBuffer;
        }

        fieldSerialization.updateEndPosition();
        setModified();
    }

    @Override
    protected boolean isCompressed() {
        return ((BinaryFieldSchemaConfiguration) getSchema().getConfiguration()).isCompressed();
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of blob ''{0}''.")
        ILocalizedMessage invalidFormat(IBlob blob);
    }
}
