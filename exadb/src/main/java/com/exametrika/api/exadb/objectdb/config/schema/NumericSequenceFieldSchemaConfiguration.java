/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.NumericSequenceField;
import com.exametrika.impl.exadb.objectdb.fields.NumericSequenceFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.NumericSequenceFieldSchema;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link NumericSequenceFieldSchemaConfiguration} represents a configuration of schema of numeric sequence field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NumericSequenceFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final long initialValue;
    private final int step;
    private final SchedulePeriodSchemaConfiguration period;

    public NumericSequenceFieldSchemaConfiguration(String name, long initialValue, int step, SchedulePeriodSchemaConfiguration period) {
        this(name, name, null, initialValue, step, period);
    }

    public NumericSequenceFieldSchemaConfiguration(String name, String alias, String description,
                                                   long initialValue, int step, SchedulePeriodSchemaConfiguration period) {
        super(name, alias, description, 16, Memory.getShallowSize(NumericSequenceField.class));

        this.initialValue = initialValue;
        this.step = step;
        this.period = period;
    }

    public long getInitialValue() {
        return initialValue;
    }

    public int getStep() {
        return step;
    }

    public SchedulePeriodSchemaConfiguration getPeriod() {
        return period;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new NumericSequenceFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof NumericSequenceFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new NumericSequenceFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NumericSequenceFieldSchemaConfiguration))
            return false;

        NumericSequenceFieldSchemaConfiguration configuration = (NumericSequenceFieldSchemaConfiguration) o;
        return super.equals(configuration) && initialValue == configuration.initialValue && step == configuration.step &&
                Objects.equals(period, configuration.period);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof NumericSequenceFieldSchemaConfiguration))
            return false;

        NumericSequenceFieldSchemaConfiguration configuration = (NumericSequenceFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(initialValue, step, period);
    }
}
