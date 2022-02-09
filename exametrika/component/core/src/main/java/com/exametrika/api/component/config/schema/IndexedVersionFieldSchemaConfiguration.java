/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.Map;

import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.component.fields.IndexedVersionField;
import com.exametrika.impl.component.fields.IndexedVersionFieldConverter;
import com.exametrika.impl.component.schema.IndexedVersionFieldSchema;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link IndexedVersionFieldSchemaConfiguration} represents a configuration of schema of indexed version field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexedVersionFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    public IndexedVersionFieldSchemaConfiguration(String name) {
        super(name, name, null, 0, Memory.getShallowSize(IndexedVersionField.class));
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public boolean isSorted() {
        return true;
    }

    @Override
    public String getIndexName() {
        return "versionIndex";
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new IndexedVersionFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof IndexedVersionFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new IndexedVersionFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                0, true, 16, true, 8, new VersionTimeKeyNormalizerSchemaConfiguration(),
                new LongValueConverterSchemaConfiguration(), true, true, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedVersionFieldSchemaConfiguration))
            return false;

        IndexedVersionFieldSchemaConfiguration configuration = (IndexedVersionFieldSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexedVersionFieldSchemaConfiguration))
            return false;

        IndexedVersionFieldSchemaConfiguration configuration = (IndexedVersionFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
