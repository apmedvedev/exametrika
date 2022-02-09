/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link FieldMetricRepresentationSchemaConfiguration} is a aggregation metric value representation schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldMetricRepresentationSchemaConfiguration extends MetricRepresentationSchemaConfiguration {
    private final List<FieldRepresentationSchemaConfiguration> fields;
    private final Map<String, FieldRepresentationSchemaConfiguration> fieldsMap;

    public FieldMetricRepresentationSchemaConfiguration(String name) {
        this(name, Collections.<FieldRepresentationSchemaConfiguration>emptyList());
    }

    public FieldMetricRepresentationSchemaConfiguration(String name,
                                                        List<? extends FieldRepresentationSchemaConfiguration> fields) {
        super(name);

        Assert.notNull(fields);

        Map<String, FieldRepresentationSchemaConfiguration> fieldsMap = new HashMap<String, FieldRepresentationSchemaConfiguration>();
        for (FieldRepresentationSchemaConfiguration field : fields)
            Assert.isNull(fieldsMap.put(field.getName(), field));

        this.fields = Immutables.wrap(fields);
        this.fieldsMap = fieldsMap;
    }

    public List<FieldRepresentationSchemaConfiguration> getFields() {
        return fields;
    }

    public FieldRepresentationSchemaConfiguration findField(String name) {
        Assert.notNull(name);

        return fieldsMap.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldMetricRepresentationSchemaConfiguration))
            return false;

        FieldMetricRepresentationSchemaConfiguration configuration = (FieldMetricRepresentationSchemaConfiguration) o;
        return super.equals(configuration) && fields.equals(configuration.fields);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fields);
    }
}
