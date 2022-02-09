/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.Map;

import sun.misc.Unsafe;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.fields.LocationField;
import com.exametrika.impl.aggregator.schema.LocationFieldSchema;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link IndexedLocationFieldSchemaConfiguration} represents a configuration of schema of primitive indexed field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexedLocationFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final int pathIndex;

    public IndexedLocationFieldSchemaConfiguration(String name) {
        this(name, name, null, 0);
    }

    public IndexedLocationFieldSchemaConfiguration(String name, String alias, String description,
                                                   int pathIndex) {
        super(name, alias, description, 16, Memory.getShallowSize(LocationField.class) + Memory.getShallowSize(Location.class) +
                CacheSizes.HASH_MAP_ENTRY_CACHE_SIZE + Unsafe.ADDRESS_SIZE);

        this.pathIndex = pathIndex;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    @Override
    public boolean isPrimary() {
        return true;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public String getIndexName() {
        return "locationIndex";
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new LocationFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof IndexedLocationFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        Assert.supports(false);
        return null;
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                pathIndex, true, getSize(), true, 8, new LocationKeyNormalizerSchemaConfiguration(),
                new LongValueConverterSchemaConfiguration(), false, true, properties);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedLocationFieldSchemaConfiguration))
            return false;

        IndexedLocationFieldSchemaConfiguration configuration = (IndexedLocationFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexedLocationFieldSchemaConfiguration))
            return false;

        IndexedLocationFieldSchemaConfiguration configuration = (IndexedLocationFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex);
    }
}
