/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.INumericSequenceField;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.schema.PrimitiveFieldSchema;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFullTextField;
import com.exametrika.spi.exadb.objectdb.fields.IPrimaryField;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link PrimitiveField} is a primitive inline node field, which contained in node's field table.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class PrimitiveField implements IPrimitiveField, IPrimaryField, IFullTextField, IFieldObject {
    protected final ISimpleField field;
    protected boolean modified;

    public static PrimitiveField createFieldInstance(ISimpleField field, DataType dataType) {
        switch (dataType) {
            case BYTE:
                return new ByteField(field);
            case SHORT:
                return new ShortField(field);
            case CHAR:
                return new CharField(field);
            case INT:
                return new IntField(field);
            case LONG:
                return new LongField(field);
            case BOOLEAN:
                return new BooleanField(field);
            case FLOAT:
                return new FloatField(field);
            case DOUBLE:
                return new DoubleField(field);
            default:
                return Assert.error();
        }
    }

    @Override
    public boolean isModified() {
        return modified;
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
    public IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public INode getNode() {
        return field.getNode();
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
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public byte getByte() {
        return ((Number) get()).byteValue();
    }

    @Override
    public short getShort() {
        return ((Number) get()).shortValue();
    }

    @Override
    public char getChar() {
        return (Character) get();
    }

    @Override
    public int getInt() {
        return ((Number) get()).intValue();
    }

    @Override
    public long getLong() {
        return ((Number) get()).longValue();
    }

    @Override
    public boolean getBoolean() {
        return (Boolean) get();
    }

    @Override
    public double getDouble() {
        return ((Number) get()).doubleValue();
    }

    @Override
    public float getFloat() {
        return ((Number) get()).floatValue();
    }

    @Override
    public void setByte(byte value) {
        set(value);
    }

    @Override
    public void setShort(short value) {
        set(value);
    }

    @Override
    public void setChar(char value) {
        set(value);
    }

    @Override
    public void setInt(int value) {
        set(value);
    }

    @Override
    public void setLong(long value) {
        set(value);
    }

    @Override
    public void setBoolean(boolean value) {
        set(value);
    }

    @Override
    public void setFloat(float value) {
        set(value);
    }

    @Override
    public void setDouble(double value) {
        set(value);
    }

    protected PrimitiveField(ISimpleField field) {
        Assert.notNull(field);

        this.field = field;
    }

    public static class ByteField extends PrimitiveField implements INumericField {
        private byte value;

        protected ByteField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readByte(0);
        }

        @Override
        public Object get() {
            return getByte();
        }

        @Override
        public void set(Object value) {
            setByte(((Number) value).byteValue());
        }

        @Override
        public byte getByte() {
            return value;
        }

        @Override
        public void setByte(byte value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            byte oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).byteValue();
                field.getWriteRegion().writeByte(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setByte((byte) 0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getByte(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeByte(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getByte(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getByte();
        }

        @Override
        public Object getFullTextValue() {
            return (int) getByte();
        }
    }

    public static class ShortField extends PrimitiveField implements INumericField {
        private short value;

        protected ShortField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readShort(0);
        }

        @Override
        public Object get() {
            return getShort();
        }

        @Override
        public void set(Object value) {
            setShort(((Number) value).shortValue());
        }

        @Override
        public short getShort() {
            return value;
        }

        @Override
        public void setShort(short value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            short oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).shortValue();
                field.getWriteRegion().writeShort(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setShort((short) 0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getShort(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeShort(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getShort(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getShort();
        }

        @Override
        public Object getFullTextValue() {
            return (int) getShort();
        }
    }

    public static class CharField extends PrimitiveField {
        private char value;

        protected CharField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readChar(0);
        }

        @Override
        public Object get() {
            return getChar();
        }

        @Override
        public void set(Object value) {
            setChar((Character) value);
        }

        @Override
        public char getChar() {
            return value;
        }

        @Override
        public void setChar(char value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            char oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isCached())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            IFieldSchema schema = field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary) {
                value = (Character) primaryKey;
                field.getWriteRegion().writeChar(0, value);

                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setChar((char) 0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getChar(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeChar(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getChar(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getChar();
        }

        @Override
        public Object getFullTextValue() {
            return (int) getChar();
        }
    }

    public static class IntField extends PrimitiveField implements INumericField {
        private int value;

        protected IntField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readInt(0);
        }

        @Override
        public Object get() {
            return getInt();
        }

        @Override
        public void set(Object value) {
            setInt(((Number) value).intValue());
        }

        @Override
        public int getInt() {
            return value;
        }

        @Override
        public void setInt(int value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            int oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).intValue();
                field.getWriteRegion().writeInt(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setInt(0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getInt(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeInt(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getInt(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getInt();
        }

        @Override
        public Object getFullTextValue() {
            return getInt();
        }
    }

    public static class LongField extends PrimitiveField implements INumericField {
        private long value;

        protected LongField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readLong(0);
        }

        @Override
        public Object get() {
            return getLong();
        }

        @Override
        public void set(Object value) {
            setLong(((Number) value).longValue());
        }

        @Override
        public long getLong() {
            return value;
        }

        @Override
        public void setLong(long value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            long oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).longValue();
                field.getWriteRegion().writeLong(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setLong(0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getLong(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeLong(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getLong(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getLong();
        }

        @Override
        public Object getFullTextValue() {
            return getLong();
        }
    }

    public static class BooleanField extends PrimitiveField {
        private boolean value;

        protected BooleanField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readByte(0) != 0;
        }

        @Override
        public Object get() {
            return getBoolean();
        }

        @Override
        public void set(Object value) {
            setBoolean((Boolean) value);
        }

        @Override
        public boolean getBoolean() {
            return value;
        }

        @Override
        public void setBoolean(boolean value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            boolean oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            IFieldSchema schema = field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary) {
                value = (Boolean) primaryKey;
                field.getWriteRegion().writeByte(0, value ? (byte) 1 : 0);

                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setBoolean(false);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getBoolean(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeByte(0, value ? (byte) 1 : 0);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getBoolean(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getBoolean();
        }

        @Override
        public Object getFullTextValue() {
            return getBoolean() ? 1 : 0;
        }
    }

    public static class FloatField extends PrimitiveField implements INumericField {
        private float value;

        protected FloatField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readFloat(0);
        }

        @Override
        public Object get() {
            return getFloat();
        }

        @Override
        public void set(Object value) {
            setFloat(((Number) value).floatValue());
        }

        @Override
        public float getFloat() {
            return value;
        }

        @Override
        public void setFloat(float value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            float oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).floatValue();
                field.getWriteRegion().writeFloat(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setFloat(0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getFloat(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeFloat(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getFloat(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getFloat();
        }

        @Override
        public Object getFullTextValue() {
            return getFloat();
        }
    }

    public static class DoubleField extends PrimitiveField implements INumericField {
        private double value;

        protected DoubleField(ISimpleField field) {
            super(field);

            value = field.getReadRegion().readDouble(0);
        }

        @Override
        public Object get() {
            return getDouble();
        }

        @Override
        public void set(Object value) {
            setDouble(((Number) value).doubleValue());
        }

        @Override
        public double getDouble() {
            return value;
        }

        @Override
        public void setDouble(double value) {
            Assert.checkState(!isReadOnly());

            if (this.value == value)
                return;

            double oldValue = this.value;
            this.value = value;
            setModified();

            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            INode node = getNode();
            if (configuration.isIndexed())
                ((NodeSpace) node.getSpace()).updateIndexValue(schema, oldValue, value, node);
        }

        @Override
        public void onCreated(Object primaryKey, Object initializer) {
            PrimitiveFieldSchema schema = (PrimitiveFieldSchema) field.getSchema();
            boolean primary = schema.getConfiguration().isPrimary();

            if (primary || schema.getSequenceField() != null) {
                Node node = (Node) getNode();

                if (primaryKey == null && schema.getSequenceField() != null) {
                    INumericSequenceField sequenceField = ((INodeObject) node.getRootNode()).getNode().getField(schema.getSequenceField().getIndex());
                    primaryKey = sequenceField.getNext();
                }

                Assert.notNull(primaryKey);

                value = ((Number) primaryKey).doubleValue();
                field.getWriteRegion().writeDouble(0, value);

                if (schema.getConfiguration().isIndexed())
                    node.getSpace().addIndexValue(schema, value, node, true, true);

                modified = false;
            } else
                setDouble(0);
        }

        @Override
        public void onOpened() {
            IFieldSchema schema = getSchema();
            FieldSchemaConfiguration configuration = schema.getConfiguration();
            if (configuration.isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).addIndexValue(schema, getDouble(), node, false, true);
            }
        }

        @Override
        public void onUnloaded() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isCached()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, false, true);
            }
        }

        @Override
        public void flush() {
            if (!modified)
                return;

            field.getWriteRegion().writeDouble(0, value);
            modified = false;
        }

        @Override
        public void onDeleted() {
            IFieldSchema schema = getSchema();
            if (schema.getConfiguration().isIndexed()) {
                INode node = getNode();
                ((NodeSpace) node.getSpace()).removeIndexValue(schema, getDouble(), node, true, true);
            }

            modified = false;
        }

        @Override
        public Object getKey() {
            return getDouble();
        }

        @Override
        public Object getFullTextValue() {
            return getDouble();
        }
    }
}
