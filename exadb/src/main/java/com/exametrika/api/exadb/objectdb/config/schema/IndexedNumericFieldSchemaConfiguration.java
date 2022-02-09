/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Map;
import java.util.Set;

import sun.misc.Unsafe;

import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.DescendingKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link IndexedNumericFieldSchemaConfiguration} represents a configuration of schema of primitive indexed field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class IndexedNumericFieldSchemaConfiguration extends NumericFieldSchemaConfiguration {
    private final int pathIndex;
    private final IndexType indexType;
    private final boolean primary;
    private final boolean unique;
    private final boolean sorted;
    private final boolean ascending;
    private final boolean cached;
    private final boolean fullText;
    private final String indexName;
    private final String fullTextFieldName;

    public IndexedNumericFieldSchemaConfiguration(String name, DataType dataType) {
        this(name, name, null, dataType, null, null, null, null, 0, IndexType.BTREE, true, true, false, true, true, false, null, null);
    }

    public IndexedNumericFieldSchemaConfiguration(String name, DataType dataType,
                                                  Number min, Number max, Set<? extends Number> enumeration, String sequenceField,
                                                  int pathIndex, IndexType indexType, boolean primary, boolean unique, boolean sorted, boolean ascending,
                                                  boolean cached, boolean fullText, String indexName, String fullTextFieldName) {
        this(name, name, null, dataType, min, max, enumeration, sequenceField, pathIndex, indexType, primary,
                unique, sorted, ascending, cached, fullText, indexName, fullTextFieldName);
    }

    public IndexedNumericFieldSchemaConfiguration(String name, String alias, String description, DataType dataType,
                                                  Number min, Number max, Set<? extends Number> enumeration, String sequenceField,
                                                  int pathIndex, IndexType indexType, boolean primary, boolean unique, boolean sorted,
                                                  boolean ascending, boolean cached, boolean fullText, String indexName, String fullTextFieldName) {
        super(name, alias, description, dataType, min, max, enumeration, sequenceField,
                cached ? CacheSizes.HASH_MAP_ENTRY_CACHE_SIZE + Unsafe.ADDRESS_SIZE : 0);

        if (indexType != null) {
            Assert.isTrue(sorted || unique);
            Assert.isTrue(!primary || unique);
            Assert.isTrue(indexType != IndexType.HASH || !sorted);
            Assert.isTrue(!cached || (indexType == IndexType.BTREE && !sorted));
        } else {
            Assert.isTrue(!primary && !cached && !sorted);
            Assert.isTrue(fullText);
        }

        this.pathIndex = pathIndex;
        this.indexType = indexType;
        this.primary = primary;
        this.unique = unique;
        this.sorted = sorted;
        this.ascending = ascending;
        this.cached = cached;
        this.fullText = fullText;
        this.indexName = indexName;
        this.fullTextFieldName = fullTextFieldName;
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
    public boolean isSorted() {
        return sorted;
    }

    public boolean isAscending() {
        return ascending;
    }

    @Override
    public boolean isCached() {
        return cached;
    }

    @Override
    public boolean isIndexed() {
        return indexType != null;
    }

    @Override
    public boolean isFullTextIndexed() {
        return fullText;
    }

    @Override
    public boolean isTokenized() {
        return false;
    }

    @Override
    public String getIndexName() {
        return indexName;
    }

    public String getFullTextFieldName() {
        return fullTextFieldName;
    }

    @Override
    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        int maxKeySize = getSize();
        KeyNormalizerSchemaConfiguration keyNormalizer = createKeyNormalizer();

        switch (indexType) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(),
                        !cached, unique, properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), sorted, unique, properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, true, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
            default:
                return Assert.error();
        }
    }

    @Override
    public com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration createFullTextSchemaConfiguration(String nodeName) {
        String fieldName;
        if (fullTextFieldName != null)
            fieldName = fullTextFieldName;
        else
            fieldName = getName();

        return new com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration(fieldName,
                getFullTextDataType(), false, true);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedNumericFieldSchemaConfiguration))
            return false;

        IndexedNumericFieldSchemaConfiguration configuration = (IndexedNumericFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && sorted == configuration.sorted &&
                ascending == configuration.ascending && cached == configuration.cached && fullText == configuration.fullText &&
                Objects.equals(indexName, configuration.indexName) &&
                Objects.equals(fullTextFieldName, configuration.fullTextFieldName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexedNumericFieldSchemaConfiguration))
            return false;

        IndexedNumericFieldSchemaConfiguration configuration = (IndexedNumericFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && sorted == configuration.sorted &&
                ascending == configuration.ascending && cached == configuration.cached && fullText == configuration.fullText &&
                Objects.equals(indexName, configuration.indexName) &&
                Objects.equals(fullTextFieldName, configuration.fullTextFieldName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, indexType, primary, unique, sorted,
                ascending, cached, fullText, indexName, fullTextFieldName);
    }

    private KeyNormalizerSchemaConfiguration createKeyNormalizer() {
        NumericKeyNormalizerSchemaConfiguration.DataType dataType;
        switch (getDataType()) {
            case BYTE:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.BYTE;
                break;
            case SHORT:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.SHORT;
                break;
            case INT:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.INT;
                break;
            case LONG:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.LONG;
                break;
            case FLOAT:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.FLOAT;
                break;
            case DOUBLE:
                dataType = NumericKeyNormalizerSchemaConfiguration.DataType.DOUBLE;
                break;
            default:
                return Assert.error();
        }

        KeyNormalizerSchemaConfiguration keyNormalizer = new NumericKeyNormalizerSchemaConfiguration(dataType);
        if (!ascending)
            keyNormalizer = new DescendingKeyNormalizerSchemaConfiguration(keyNormalizer);

        return keyNormalizer;
    }

    private com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType getFullTextDataType() {
        com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType dataType;
        switch (getDataType()) {
            case BYTE:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.INT;
                break;
            case SHORT:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.INT;
                break;
            case INT:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.INT;
                break;
            case LONG:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.LONG;
                break;
            case FLOAT:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.FLOAT;
                break;
            case DOUBLE:
                dataType = com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType.DOUBLE;
                break;
            default:
                return Assert.error();
        }

        return dataType;
    }
}
