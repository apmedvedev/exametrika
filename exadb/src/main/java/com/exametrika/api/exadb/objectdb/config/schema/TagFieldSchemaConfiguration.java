/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Map;

import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.TagField;
import com.exametrika.impl.exadb.objectdb.fields.TagFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.TagFieldSchema;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link TagFieldSchemaConfiguration} represents a configuration of schema of tag field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class TagFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final int maxSize;
    private final int pathIndex;
    private final IndexType indexType;
    private final String indexName;

    public TagFieldSchemaConfiguration(String name) {
        this(name, name, null, 128, 0, IndexType.BTREE, "exaTags");
    }

    public TagFieldSchemaConfiguration(String name, int maxSize, int pathIndex, IndexType indexType, String indexName) {
        this(name, name, null, maxSize, pathIndex, indexType, indexName);
    }

    public TagFieldSchemaConfiguration(String name, String alias, String description, int maxSize,
                                       int pathIndex, IndexType indexType, String indexName) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(TagField.class));

        Assert.isTrue(maxSize <= Constants.PAGE_SIZE / 64);
        Assert.notNull(indexType);

        this.maxSize = maxSize;
        this.pathIndex = pathIndex;
        this.indexType = indexType;
        this.indexName = indexName;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    @Override
    public String getIndexName() {
        return indexName;
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
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new TagFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof TagFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new TagFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        int maxKeySize = 2 * maxSize;
        KeyNormalizerSchemaConfiguration keyNormalizer = new StringKeyNormalizerSchemaConfiguration();

        switch (indexType) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, false, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(),
                        true, false, properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, false, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), true, false, properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, false, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
            default:
                return Assert.error();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TagFieldSchemaConfiguration))
            return false;

        TagFieldSchemaConfiguration configuration = (TagFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                maxSize == configuration.maxSize && Objects.equals(indexName, configuration.indexName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof TagFieldSchemaConfiguration))
            return false;

        TagFieldSchemaConfiguration configuration = (TagFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                maxSize >= configuration.maxSize && Objects.equals(indexName, configuration.indexName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(maxSize, pathIndex, indexType, indexName);
    }
}
