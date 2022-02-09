/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Map;
import java.util.Set;

import sun.misc.Unsafe;

import com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration.Strength;
import com.exametrika.api.exadb.index.config.schema.DescendingKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedStringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link IndexedStringFieldSchemaConfiguration} represents a configuration of schema of string indexed field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class IndexedStringFieldSchemaConfiguration extends StringFieldSchemaConfiguration {
    private final int pathIndex;
    private final IndexType indexType;
    private final boolean primary;
    private final boolean unique;
    private final boolean sorted;
    private final boolean ascending;
    private final boolean cached;
    private final CollatorSchemaConfiguration collator;
    private final boolean fullText;
    private final boolean tokenized;
    private final String indexName;
    private final String fullTextFieldName;

    public IndexedStringFieldSchemaConfiguration(String name, boolean compressed, int maxSize) {
        this(name, name, null, true, compressed, 0, maxSize, null, null, null,
                0, IndexType.BTREE, true, true, false, true, true, null, false, false, null, null);
    }

    public IndexedStringFieldSchemaConfiguration(String name, boolean required, boolean compressed,
                                                 int minSize, int maxSize, String pattern, Set<String> enumeration, String sequenceField,
                                                 int pathIndex, IndexType indexType, boolean primary, boolean unique, boolean sorted, boolean ascending,
                                                 boolean cached, CollatorSchemaConfiguration collator, boolean fullText, boolean tokenized,
                                                 String indexName, String fullTextFieldName) {
        this(name, name, null, required, compressed, minSize, maxSize, pattern, enumeration, sequenceField,
                pathIndex, indexType, primary, unique, sorted, ascending, cached, collator, fullText, tokenized,
                indexName, fullTextFieldName);
    }

    public IndexedStringFieldSchemaConfiguration(String name, String alias, String description, boolean required,
                                                 boolean compressed, int minSize, int maxSize, String pattern, Set<String> enumeration, String sequenceField,
                                                 int pathIndex, IndexType indexType,
                                                 boolean primary, boolean unique, boolean sorted, boolean ascending, boolean cached, CollatorSchemaConfiguration collator,
                                                 boolean fullText, boolean tokenized, String indexName, String fullTextFieldName) {
        super(name, alias, description, required, compressed, minSize, maxSize, pattern, enumeration, sequenceField,
                cached ? CacheSizes.HASH_MAP_ENTRY_CACHE_SIZE + Unsafe.ADDRESS_SIZE : 0);

        if (indexType != null) {
            Assert.isTrue(maxSize <= Constants.PAGE_SIZE / 64);
            Assert.notNull(indexType);
            Assert.isTrue(sorted || unique);
            Assert.isTrue(!primary || (unique && required));
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
        this.collator = collator;
        this.fullText = fullText;
        this.tokenized = tokenized;
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

    public CollatorSchemaConfiguration getCollator() {
        return collator;
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
        return tokenized;
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
        int maxKeySize = getMaxSize() * 4;

        KeyNormalizerSchemaConfiguration keyNormalizer;
        boolean fixedKey = false;
        if (collator != null)
            keyNormalizer = new CollatorKeyNormalizerSchemaConfiguration(collator.getLocale(), getCollatorStrength(collator.getStrength()));
        else if (getMinSize() == getMaxSize()) {
            keyNormalizer = new FixedStringKeyNormalizerSchemaConfiguration();
            fixedKey = true;
        } else
            keyNormalizer = new StringKeyNormalizerSchemaConfiguration();

        if (!ascending)
            keyNormalizer = new DescendingKeyNormalizerSchemaConfiguration(keyNormalizer);

        switch (indexType) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, fixedKey, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(),
                        !cached, unique, properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, fixedKey, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), sorted, unique, properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + getName(), aliasPrefix + getAlias(), getDescription(),
                        pathIndex, fixedKey, maxKeySize, true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
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

        return new com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration(fieldName,
                Enums.of(tokenized ? Option.TOKENIZED_AND_INDEXED : Option.INDEXED, Option.INDEX_DOCUMENTS, Option.OMIT_NORMS),
                collator != null ? new CollationKeyAnalyzerSchemaConfiguration(collator.getLocale(), createStrength(collator.getStrength())) :
                        new StandardAnalyzerSchemaConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexedStringFieldSchemaConfiguration))
            return false;

        IndexedStringFieldSchemaConfiguration configuration = (IndexedStringFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && sorted == configuration.sorted &&
                ascending == configuration.ascending && cached == configuration.cached &&
                Objects.equals(collator, configuration.collator) && fullText == configuration.fullText &&
                tokenized == configuration.tokenized && Objects.equals(indexName, configuration.indexName) &&
                Objects.equals(fullTextFieldName, configuration.fullTextFieldName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexedStringFieldSchemaConfiguration))
            return false;

        IndexedStringFieldSchemaConfiguration configuration = (IndexedStringFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex && Objects.equals(indexType, configuration.indexType) &&
                primary == configuration.primary && unique == configuration.unique && sorted == configuration.sorted &&
                ascending == configuration.ascending && cached == configuration.cached &&
                Objects.equals(collator, configuration.collator) && fullText == configuration.fullText &&
                tokenized == configuration.tokenized && Objects.equals(indexName, configuration.indexName) &&
                Objects.equals(fullTextFieldName, configuration.fullTextFieldName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, indexType, primary, unique, sorted,
                cached, ascending, collator, fullText, tokenized, indexName, fullTextFieldName);
    }

    private Strength getCollatorStrength(com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength strength) {
        switch (strength) {
            case PRIMARY:
                return Strength.PRIMARY;
            case SECONDARY:
                return Strength.SECONDARY;
            case TERTIARY:
                return Strength.TERTIARY;
            case QUATERNARY:
                return Strength.QUATERNARY;
            case IDENTICAL:
                return Strength.IDENTICAL;
            default:
                return Assert.error();
        }
    }

    private com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength createStrength(
            com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength strength) {
        switch (strength) {
            case PRIMARY:
                return com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength.PRIMARY;
            case SECONDARY:
                return com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength.SECONDARY;
            case TERTIARY:
                return com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength.TERTIARY;
            case QUATERNARY:
                return com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength.QUATERNARY;
            case IDENTICAL:
                return com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength.IDENTICAL;
            default:
                return Assert.error();
        }
    }
}
