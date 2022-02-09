/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import java.util.Map;

import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.memory.HashIndexSpace;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.ValueConverterSchemaConfiguration;


/**
 * The {@link HashIndexSchemaConfiguration} is a configuration of in-memory Hash index schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HashIndexSchemaConfiguration extends IndexSchemaConfiguration {
    private final boolean fixedKey;
    private final int maxKeySize;
    private final boolean fixedValue;
    private final int maxValueSize;
    private final KeyNormalizerSchemaConfiguration keyNormalizer;
    private final ValueConverterSchemaConfiguration valueConverter;

    public HashIndexSchemaConfiguration(String name, int pathIndex, boolean fixedKey,
                                        int maxKeySize, boolean fixedValue, int maxValueSize, KeyNormalizerSchemaConfiguration keyNormalizer,
                                        ValueConverterSchemaConfiguration valueConverter, Map<String, String> properties) {
        this(name, name, null, pathIndex, fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter,
                properties);
    }

    public HashIndexSchemaConfiguration(String name, String alias, String description, int pathIndex, boolean fixedKey,
                                        int maxKeySize, boolean fixedValue, int maxValueSize, KeyNormalizerSchemaConfiguration keyNormalizer,
                                        ValueConverterSchemaConfiguration valueConverter, Map<String, String> properties) {
        super(name, alias, description, pathIndex, properties);

        Assert.notNull(keyNormalizer);
        Assert.notNull(valueConverter);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.fixedValue = fixedValue;
        this.maxValueSize = maxValueSize;
        this.keyNormalizer = keyNormalizer;
        this.valueConverter = valueConverter;
    }

    public boolean isFixedKey() {
        return fixedKey;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public boolean isFixedValue() {
        return fixedValue;
    }

    public int getMaxValueSize() {
        return maxValueSize;
    }

    public KeyNormalizerSchemaConfiguration getKeyNormalizer() {
        return keyNormalizer;
    }

    public ValueConverterSchemaConfiguration getValueConverter() {
        return valueConverter;
    }

    @Override
    public String getType() {
        return "hash";
    }

    @Override
    public IIndex createIndex(String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        int fileIndex = context.getSchemaSpace().allocateFile(context.getTransactionProvider().getRawTransaction());
        return HashIndexSpace.create((IndexManager) indexManager, this, context.getTransactionProvider(), context.getSchemaSpace(),
                fileIndex, filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize,
                keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter(), null, getProperties());
    }

    @Override
    public IIndex openIndex(int id, String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        return HashIndexSpace.open((IndexManager) indexManager, this, context.getTransactionProvider(),
                context.getSchemaSpace(), id, filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize,
                keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter(), null, getProperties());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HashIndexSchemaConfiguration))
            return false;

        HashIndexSchemaConfiguration configuration = (HashIndexSchemaConfiguration) o;
        return super.equals(configuration) && fixedKey == configuration.fixedKey && maxKeySize == configuration.maxKeySize &&
                fixedValue == configuration.fixedValue && maxValueSize == configuration.maxValueSize &&
                keyNormalizer.equals(configuration.keyNormalizer) && valueConverter.equals(configuration.valueConverter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer,
                valueConverter);
    }
}
