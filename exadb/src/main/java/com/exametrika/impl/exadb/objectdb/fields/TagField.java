/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ITagField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.IFieldSerialization;


/**
 * The {@link TagField} is a tag field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TagField implements ITagField, IFieldObject {
    private final IComplexField field;
    private List<String> value;
    private boolean modified;
    private int lastCacheSize;

    public TagField(IComplexField field) {
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
    public List<String> get() {
        if (value != null)
            return value;
        else
            return readValue();
    }

    @Override
    public void set(List<String> value) {
        Assert.checkState(!field.isReadOnly());
        get();

        if (Objects.equals(this.value, value))
            return;

        List<String> oldValue = this.value;
        this.value = value;
        setModified();

        if (oldValue != null)
            removeIndexValues(oldValue);
        if (value != null)
            addIndexValues(value);
    }

    @Override
    public void onCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
        if (get() != null)
            removeIndexValues(get());

        modified = false;
        value = null;
    }

    @Override
    public void onUnloaded() {
        value = null;
    }

    @Override
    public void flush() {
        if (!modified)
            return;

        writeValue();

        modified = false;
    }

    private List<String> readValue() {
        IFieldDeserialization fieldDeserialization = field.createDeserialization();
        if (fieldDeserialization.readBoolean()) {
            int count = fieldDeserialization.readInt();
            value = new ArrayList<String>(count);
            for (int i = 0; i < count; i++)
                value.add(fieldDeserialization.readString());
        } else
            value = null;

        updateCacheSize();

        return value;
    }

    private void writeValue() {
        IFieldSerialization fieldSerialization = field.createSerialization();

        if (value == null)
            fieldSerialization.writeBoolean(false);
        else {
            fieldSerialization.writeBoolean(true);
            fieldSerialization.writeInt(value.size());
            for (String s : value)
                fieldSerialization.writeString(s);
        }

        if (field.allowDeletion())
            fieldSerialization.removeRest();

        updateCacheSize();
    }

    private void addIndexValues(List<String> values) {
        Set<String> indexedValues = getIndexedValues(values);

        IFieldSchema schema = getSchema();
        INode node = getNode();

        for (String value : indexedValues)
            ((NodeSpace) node.getSpace()).addIndexValue(schema, value, node, true, false);
    }

    private void removeIndexValues(List<String> values) {
        Set<String> indexedValues = getIndexedValues(values);

        IFieldSchema schema = getSchema();
        INode node = getNode();

        for (String value : indexedValues)
            ((NodeSpace) node.getSpace()).removeIndexValue(schema, value, node, true, false);
    }

    private Set<String> getIndexedValues(List<String> values) {
        Set<String> indexedValues = new HashSet<String>();

        for (String value : values)
            buildIndexedValue(value, indexedValues);

        return indexedValues;
    }

    private void buildIndexedValue(String tag, Set<String> indexedValues) {
        for (int i = 0; i < tag.length(); i++) {
            char ch = tag.charAt(i);
            if (ch == '.')
                indexedValues.add(tag.substring(0, i));
        }

        indexedValues.add(tag);
    }

    private void updateCacheSize() {
        int cacheSize = 0;
        if (value != null) {
            cacheSize = CacheSizes.getArrayListCacheSize(value);
            for (int i = 0; i < value.size(); i++)
                cacheSize += CacheSizes.getStringCacheSize(value.get(i));
        }

        if (cacheSize != lastCacheSize) {
            getNode().updateCacheSize(cacheSize - lastCacheSize);
            lastCacheSize = cacheSize;
        }
    }
}
