/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;

/**
 * The {@link FieldSchemaConfiguration} represents a configuration of schema of field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldSchemaConfiguration extends SchemaConfiguration {
    private final int size;
    private final int cacheSize;

    public FieldSchemaConfiguration(String name, String alias, String description, int size, int cacheSize) {
        super(name, alias, description);

        this.size = size;
        this.cacheSize = cacheSize;
    }

    public boolean isPrimary() {
        return false;
    }

    public boolean isCached() {
        return false;
    }

    public final int getSize() {
        return size;
    }

    public final int getCacheSize() {
        return cacheSize;
    }

    public abstract IFieldSchema createSchema(int index, int offset, int indexTotalIndex);

    public abstract Object createInitializer();

    public abstract boolean isCompatible(FieldSchemaConfiguration newConfiguration);

    public abstract IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration);

    public boolean isIndexed() {
        return false;
    }

    public String getIndexName() {
        return null;
    }

    public boolean isSorted() {
        return false;
    }

    public boolean isFullTextIndexed() {
        return false;
    }

    public boolean hasFullTextIndex() {
        return isFullTextIndexed();
    }

    public boolean isTokenized() {
        return false;
    }

    public List<FieldSchemaConfiguration> getAdditionalFields() {
        return null;
    }

    public IndexSchemaConfiguration createIndexSchemaConfiguration(String namePrefix, String aliasPrefix,
                                                                   Map<String, String> properties) {
        Assert.supports(false);
        return null;
    }

    public com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration createFullTextSchemaConfiguration(String nodeName) {
        Assert.supports(false);
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldSchemaConfiguration))
            return false;

        FieldSchemaConfiguration configuration = (FieldSchemaConfiguration) o;
        return super.equals(configuration) && size == configuration.size;
    }

    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        return getName().equals(newSchema.getName()) && size == newSchema.size;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(size);
    }
}
