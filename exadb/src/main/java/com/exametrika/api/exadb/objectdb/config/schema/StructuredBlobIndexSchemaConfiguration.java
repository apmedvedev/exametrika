/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link StructuredBlobIndexSchemaConfiguration} is a configuration of B+ Tree index schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StructuredBlobIndexSchemaConfiguration extends SchemaConfiguration {
    private final String indexName;
    private final IndexType indexType;
    private final int pathIndex;
    private final boolean fixedKey;
    private final int maxKeySize;
    private final KeyNormalizerSchemaConfiguration keyNormalizer;
    private final boolean unique;
    private final boolean sorted;

    public StructuredBlobIndexSchemaConfiguration(String name, int pathIndex, IndexType indexType, boolean fixedKey,
                                                  int maxKeySize, KeyNormalizerSchemaConfiguration keyNormalizer, boolean unique, boolean sorted, String indexName) {
        this(name, name, null, pathIndex, indexType, fixedKey, maxKeySize, keyNormalizer, unique, sorted, indexName);
    }

    public StructuredBlobIndexSchemaConfiguration(String name, String alias, String description, int pathIndex, IndexType indexType,
                                                  boolean fixedKey, int maxKeySize, KeyNormalizerSchemaConfiguration keyNormalizer, boolean unique, boolean sorted,
                                                  String indexName) {
        super(name, alias, description);

        Assert.notNull(indexType);
        Assert.notNull(keyNormalizer);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(sorted || unique);
        Assert.isTrue(indexType != IndexType.HASH || !sorted);

        this.indexName = indexName;
        this.indexType = indexType;
        this.pathIndex = pathIndex;
        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.keyNormalizer = keyNormalizer;
        this.unique = unique;
        this.sorted = sorted;
    }

    public String getIndexName() {
        return indexName;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public boolean isFixedKey() {
        return fixedKey;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public KeyNormalizerSchemaConfiguration getKeyNormalizer() {
        return keyNormalizer;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isSorted() {
        return sorted;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StructuredBlobIndexSchemaConfiguration))
            return false;

        StructuredBlobIndexSchemaConfiguration configuration = (StructuredBlobIndexSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(indexName, configuration.indexName) && indexType == configuration.indexType &&
                pathIndex == configuration.pathIndex &&
                fixedKey == configuration.fixedKey && maxKeySize == configuration.maxKeySize &&
                keyNormalizer.equals(configuration.keyNormalizer) &&
                unique == configuration.unique && sorted == configuration.sorted;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(indexName, indexType, pathIndex, fixedKey, maxKeySize, keyNormalizer,
                unique, sorted);
    }
}
