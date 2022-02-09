/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.impl.exadb.objectdb.fields.ComputedField;
import com.exametrika.impl.exadb.objectdb.fields.NoneFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.ComputedFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link ComputedFieldSchemaConfiguration} represents a configuration of schema of computed field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComputedFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final String expression;

    public ComputedFieldSchemaConfiguration(String name, String expression) {
        this(name, name, null, expression);
    }

    public ComputedFieldSchemaConfiguration(String name, String alias, String description, String expression) {
        super(name, alias, description, 0, Memory.getShallowSize(ComputedField.class));

        Assert.notNull(expression);

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new ComputedFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof ComputedFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new NoneFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComputedFieldSchemaConfiguration))
            return false;

        ComputedFieldSchemaConfiguration configuration = (ComputedFieldSchemaConfiguration) o;
        return super.equals(configuration) && expression.equals(configuration.expression);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ComputedFieldSchemaConfiguration))
            return false;

        ComputedFieldSchemaConfiguration configuration = (ComputedFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + expression.hashCode();
    }
}
