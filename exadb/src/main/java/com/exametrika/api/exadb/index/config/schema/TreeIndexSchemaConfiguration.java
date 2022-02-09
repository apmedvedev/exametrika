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
import com.exametrika.impl.exadb.index.memory.TreeIndexSpace;
import com.exametrika.impl.exadb.index.memory.TreeNonUniqueIndex;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.ValueConverterSchemaConfiguration;


/**
 * The {@link TreeIndexSchemaConfiguration} is a configuration of in-memory Tree index schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TreeIndexSchemaConfiguration extends IndexSchemaConfiguration {
    private final boolean fixedKey;
    private final int maxKeySize;
    private final boolean fixedValue;
    private final int maxValueSize;
    private final KeyNormalizerSchemaConfiguration keyNormalizer;
    private final ValueConverterSchemaConfiguration valueConverter;
    private final boolean sorted;
    private final boolean unique;

    public TreeIndexSchemaConfiguration(String name, int pathIndex, boolean fixedKey,
                                        int maxKeySize, boolean fixedValue, int maxValueSize, KeyNormalizerSchemaConfiguration keyNormalizer,
                                        ValueConverterSchemaConfiguration valueConverter, boolean sorted, boolean unique, Map<String, String> properties) {
        this(name, name, null, pathIndex, fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter,
                sorted, unique, properties);
    }

    public TreeIndexSchemaConfiguration(String name, String alias, String description, int pathIndex, boolean fixedKey,
                                        int maxKeySize, boolean fixedValue, int maxValueSize, KeyNormalizerSchemaConfiguration keyNormalizer,
                                        ValueConverterSchemaConfiguration valueConverter, boolean sorted, boolean unique, Map<String, String> properties) {
        super(name, alias, description, pathIndex, properties);

        Assert.notNull(keyNormalizer);
        Assert.notNull(valueConverter);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(unique || fixedValue);

        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.fixedValue = fixedValue;
        this.maxValueSize = maxValueSize;
        this.keyNormalizer = keyNormalizer;
        this.valueConverter = valueConverter;
        this.sorted = sorted;
        this.unique = unique;
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

    public boolean isSorted() {
        return sorted;
    }

    public boolean isUnique() {
        return unique;
    }

    @Override
    public String getType() {
        return "tree";
    }

    @Override
    public IIndex createIndex(String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        int fileIndex = context.getSchemaSpace().allocateFile(context.getTransactionProvider().getRawTransaction());
        if (unique)
            return TreeIndexSpace.create((IndexManager) indexManager, this, context.getTransactionProvider(), context.getSchemaSpace(),
                    fileIndex, filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize,
                    keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter(), true, null, getProperties());
        else {
            int maxKeySize = fixedKey ? (this.maxKeySize + maxValueSize) : ((this.maxKeySize + 1) * 2 + maxValueSize);

            TreeIndexSpace treeIndex = TreeIndexSpace.create((IndexManager) indexManager, this, context.getTransactionProvider(),
                    context.getSchemaSpace(), fileIndex, filePrefix, fixedKey, maxKeySize, true, 0,
                    new ByteArrayKeyNormalizerSchemaConfiguration().createKeyNormalizer(),
                    new ByteArrayValueConverterSchemaConfiguration().createValueConverter(), true, null, getProperties());
            return new TreeNonUniqueIndex((IndexManager) indexManager, this, treeIndex, fixedKey,
                    this.maxKeySize, maxValueSize, keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter());
        }
    }

    @Override
    public IIndex openIndex(int id, String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        if (unique)
            return TreeIndexSpace.open((IndexManager) indexManager, this, context.getTransactionProvider(),
                    context.getSchemaSpace(), id, filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize,
                    keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter(), null, getProperties(), true);
        else {
            int maxKeySize = fixedKey ? (this.maxKeySize + maxValueSize) : ((this.maxKeySize + 1) * 2 + maxValueSize);

            TreeIndexSpace treeIndex = TreeIndexSpace.open((IndexManager) indexManager, this, context.getTransactionProvider(),
                    context.getSchemaSpace(), id, filePrefix, fixedKey, maxKeySize, true, 0,
                    new ByteArrayKeyNormalizerSchemaConfiguration().createKeyNormalizer(),
                    new ByteArrayValueConverterSchemaConfiguration().createValueConverter(), null, getProperties(), true);
            return new TreeNonUniqueIndex((IndexManager) indexManager, this, treeIndex, fixedKey,
                    this.maxKeySize, maxValueSize, keyNormalizer.createKeyNormalizer(), valueConverter.createValueConverter());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TreeIndexSchemaConfiguration))
            return false;

        TreeIndexSchemaConfiguration configuration = (TreeIndexSchemaConfiguration) o;
        return super.equals(configuration) && fixedKey == configuration.fixedKey && maxKeySize == configuration.maxKeySize &&
                fixedValue == configuration.fixedValue && maxValueSize == configuration.maxValueSize &&
                keyNormalizer.equals(configuration.keyNormalizer) && valueConverter.equals(configuration.valueConverter) &&
                sorted == configuration.sorted && unique == configuration.unique;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer,
                valueConverter, sorted, unique);
    }
}
