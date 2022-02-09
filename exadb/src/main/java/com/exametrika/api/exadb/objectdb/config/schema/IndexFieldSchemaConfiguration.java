/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.objectdb.fields.IndexField;
import com.exametrika.impl.exadb.objectdb.fields.IndexFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.IndexFieldSchema;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link IndexFieldSchemaConfiguration} represents a configuration of schema of index field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final IndexSchemaConfiguration index;

    public IndexFieldSchemaConfiguration(String name, IndexSchemaConfiguration index) {
        this(name, name, null, index);
    }

    public IndexFieldSchemaConfiguration(String name, String alias, String description, IndexSchemaConfiguration index) {
        super(name, alias, description, 4, Memory.getShallowSize(IndexField.class));

        Assert.notNull(index);

        this.index = index;
    }

    public IndexSchemaConfiguration getIndex() {
        return index;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new IndexFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return false;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new IndexFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof IndexFieldSchemaConfiguration))
            return false;

        IndexFieldSchemaConfiguration configuration = (IndexFieldSchemaConfiguration) o;
        return super.equals(o) && index.equals(configuration.index);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof IndexFieldSchemaConfiguration))
            return false;

        IndexFieldSchemaConfiguration configuration = (IndexFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && index.equals(configuration.index);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + index.hashCode();
    }
}
