/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Map;

import sun.misc.Unsafe;

import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.UuidKeyNormalizerSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link IndexedUuidFieldSchemaConfiguration} represents a configuration of schema of UUID indexed field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class IndexedUuidFieldSchemaConfiguration extends UuidFieldSchemaConfiguration {
    private final int pathIndex;
    private final IndexType indexType;
    private final boolean primary;
    private final boolean unique;
    private final boolean cached;
    private final String indexName;

    public IndexedUuidFieldSchemaConfiguration(String name) {
        this(name, name, null, true, 0, IndexType.BTREE, true, true, true, null);
    }

    public IndexedUuidFieldSchemaConfiguration(String name, boolean required,
                                               int pathIndex, IndexType indexType, boolean primary, boolean unique, boolean cached, String indexName) {
        this(name, name, null, required, pathIndex, indexType, primary, unique, cached, indexName);
    }

    public IndexedUuidFieldSchemaConfiguration(String name, String alias, String description, boolean required,
                                               int pathIndex, IndexType indexType, boolean primary, boolean unique, boolean cached, String indexName) {
        super(name, alias, description, required,
                cached ? CacheSizes.HASH_MAP_ENTRY_CACHE_SIZE + Unsafe.ADDRESS_SIZE : 0);

        Assert.notNull(indexType);
        Assert.isTrue(!primary || (unique && required));
        Assert.isTrue(!cached || (indexType == IndexType.BTREE));

        this.pathIndex = pathIndex;
        this.indexType = indexType;
        this.primary = primary;
        this.unique = unique;
        this.cached = cached;
        this.indexName = indexName;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    @Override
    public boolean isPrimary() {
        return primary;
    }

    public boolean isUnique() {
        return unique;
    }

    @Override
    public boolean isCached() {
        return cached;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    @Override
    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        int maxKeySize = getSize();
        KeyNormalizerSchemaConfiguration keyNormalizer = new UuidKeyNormalizerSchemaConfiguration();

        switch (indexType) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(),
                        !cached, unique, properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), false, unique, properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
            default:
                return Assert.error();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedUuidFieldSchemaConfiguration))
            return false;

        IndexedUuidFieldSchemaConfiguration configuration = (IndexedUuidFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && cached == configuration.cached &&
                Objects.equals(indexName, configuration.indexName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexedUuidFieldSchemaConfiguration))
            return false;

        IndexedUuidFieldSchemaConfiguration configuration = (IndexedUuidFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && cached == configuration.cached &&
                Objects.equals(indexName, configuration.indexName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, indexType, primary, unique, cached, indexName);
    }
}
