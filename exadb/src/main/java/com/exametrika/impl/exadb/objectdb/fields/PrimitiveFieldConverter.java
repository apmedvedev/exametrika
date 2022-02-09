/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryFieldConverter;
import com.exametrika.spi.exadb.objectdb.schema.IFieldMigrationSchema;


/**
 * The {@link PrimitiveFieldConverter} is a primitive field converter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class PrimitiveFieldConverter implements IPrimaryFieldConverter {
    @Override
    public void convert(IField oldFieldInstance, IField newFieldInstance, IFieldMigrationSchema migrationSchema) {
        IPrimitiveField oldField = oldFieldInstance.getObject();
        IPrimitiveField newField = newFieldInstance.getObject();
        DataType oldDataType = ((PrimitiveFieldSchemaConfiguration) oldFieldInstance.getSchema().getConfiguration()).getDataType();
        DataType newDataType = ((PrimitiveFieldSchemaConfiguration) newFieldInstance.getSchema().getConfiguration()).getDataType();

        switch (oldDataType) {
            case BYTE: {
                byte value = oldField.getByte();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte(value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort(value);
                        break;
                    case INT:
                        newField.setInt(value);
                        break;
                    case LONG:
                        newField.setLong(value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case CHAR: {
                char value = oldField.getChar();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar(value);
                        break;
                    case SHORT:
                        newField.setShort((short) value);
                        break;
                    case INT:
                        newField.setInt(value);
                        break;
                    case LONG:
                        newField.setLong(value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case SHORT: {
                short value = oldField.getShort();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort(value);
                        break;
                    case INT:
                        newField.setInt(value);
                        break;
                    case LONG:
                        newField.setLong(value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case INT: {
                int value = oldField.getInt();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort((short) value);
                        break;
                    case INT:
                        newField.setInt(value);
                        break;
                    case LONG:
                        newField.setLong(value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case LONG: {
                long value = oldField.getLong();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort((short) value);
                        break;
                    case INT:
                        newField.setInt((int) value);
                        break;
                    case LONG:
                        newField.setLong(value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case BOOLEAN: {
                boolean value = oldField.getBoolean();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte(value ? (byte) 1 : 0);
                        break;
                    case CHAR:
                        newField.setChar(value ? (char) 1 : 0);
                        break;
                    case SHORT:
                        newField.setShort(value ? (short) 1 : 0);
                        break;
                    case INT:
                        newField.setInt(value ? 1 : 0);
                        break;
                    case LONG:
                        newField.setLong(value ? 1 : 0);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value);
                        break;
                    case FLOAT:
                        newField.setFloat(value ? 1 : 0);
                    case DOUBLE:
                        newField.setDouble(value ? 1 : 0);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case FLOAT: {
                float value = oldField.getFloat();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort((short) value);
                        break;
                    case INT:
                        newField.setInt((int) value);
                        break;
                    case LONG:
                        newField.setLong((long) value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat(value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            case DOUBLE: {
                double value = oldField.getDouble();
                switch (newDataType) {
                    case BYTE:
                        newField.setByte((byte) value);
                        break;
                    case CHAR:
                        newField.setChar((char) value);
                        break;
                    case SHORT:
                        newField.setShort((short) value);
                        break;
                    case INT:
                        newField.setInt((int) value);
                        break;
                    case LONG:
                        newField.setLong((long) value);
                        break;
                    case BOOLEAN:
                        newField.setBoolean(value != 0);
                        break;
                    case FLOAT:
                        newField.setFloat((float) value);
                    case DOUBLE:
                        newField.setDouble(value);
                        break;
                    default:
                        Assert.error();
                }
            }
            break;
            default:
                Assert.error();
        }
    }

    @Override
    public Object convert(IField oldFieldInstance, IFieldMigrationSchema migrationSchema) {
        IPrimitiveField oldField = oldFieldInstance.getObject();
        DataType oldDataType = ((PrimitiveFieldSchemaConfiguration) oldFieldInstance.getSchema().getConfiguration()).getDataType();
        DataType newDataType = ((PrimitiveFieldSchemaConfiguration) migrationSchema.getNewSchema().getConfiguration()).getDataType();

        switch (oldDataType) {
            case BYTE: {
                byte value = oldField.getByte();
                switch (newDataType) {
                    case BYTE:
                        return value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case CHAR: {
                char value = oldField.getChar();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case SHORT: {
                short value = oldField.getShort();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case INT: {
                int value = oldField.getInt();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case LONG: {
                long value = oldField.getLong();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case BOOLEAN: {
                boolean value = oldField.getBoolean();
                switch (newDataType) {
                    case BYTE:
                        return value ? (byte) 1 : (byte) 0;
                    case CHAR:
                        return value ? (char) 1 : (char) 0;
                    case SHORT:
                        return value ? (short) 1 : (short) 0;
                    case INT:
                        return value ? (int) 1 : (int) 0;
                    case LONG:
                        return value ? (long) 1 : (long) 0;
                    case BOOLEAN:
                        return value;
                    case FLOAT:
                        return value ? (float) 1 : (float) 0;
                    case DOUBLE:
                        return value ? (double) 1 : (double) 0;
                    default:
                        return Assert.error();
                }
            }
            case FLOAT: {
                float value = oldField.getFloat();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            case DOUBLE: {
                double value = oldField.getDouble();
                switch (newDataType) {
                    case BYTE:
                        return (byte) value;
                    case CHAR:
                        return (char) value;
                    case SHORT:
                        return (short) value;
                    case INT:
                        return (int) value;
                    case LONG:
                        return (long) value;
                    case BOOLEAN:
                        return value != 0;
                    case FLOAT:
                        return (float) value;
                    case DOUBLE:
                        return (double) value;
                    default:
                        return Assert.error();
                }
            }
            default:
                return Assert.error();
        }
    }
}
