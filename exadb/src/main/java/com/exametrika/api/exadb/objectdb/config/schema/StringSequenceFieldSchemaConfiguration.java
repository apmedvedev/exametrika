/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.schema.StringSequenceFieldSchema;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link StringSequenceFieldSchemaConfiguration} represents a configuration of schema of string sequence field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StringSequenceFieldSchemaConfiguration extends NumericSequenceFieldSchemaConfiguration {
    private final String prefix;
    private final String suffix;
    private final String numberFormat;

    public StringSequenceFieldSchemaConfiguration(String name,
                                                  String prefix, String suffix, String numberFormat, long initialValue, int step,
                                                  SchedulePeriodSchemaConfiguration period) {
        this(name, name, null, prefix, suffix, numberFormat, initialValue, step, period);
    }

    public StringSequenceFieldSchemaConfiguration(String name, String alias, String description,
                                                  String prefix, String suffix, String numberFormat, long initialValue, int step,
                                                  SchedulePeriodSchemaConfiguration period) {
        super(name, alias, description, initialValue, step, period);

        if (prefix == null)
            prefix = "";
        if (suffix == null)
            suffix = "";

        this.prefix = prefix;
        this.suffix = suffix;
        this.numberFormat = numberFormat;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getNumberFormat() {
        return numberFormat;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new StringSequenceFieldSchema(this, index, offset);
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StringSequenceFieldSchemaConfiguration))
            return false;

        StringSequenceFieldSchemaConfiguration configuration = (StringSequenceFieldSchemaConfiguration) o;
        return super.equals(configuration) && prefix.equals(configuration.prefix) && suffix.equals(configuration.suffix) &&
                Objects.equals(numberFormat, configuration.numberFormat);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StringSequenceFieldSchemaConfiguration))
            return false;

        StringSequenceFieldSchemaConfiguration configuration = (StringSequenceFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(prefix, suffix, numberFormat);
    }
}
