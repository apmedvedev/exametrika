/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link FieldMetricValueSchemaConfiguration} is a aggregation metric value schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldMetricValueSchemaConfiguration extends MetricValueSchemaConfiguration {
    private final List<FieldValueSchemaConfiguration> fields;
    private final Map<String, FieldValueSchemaConfiguration> fieldsMap;

    public FieldMetricValueSchemaConfiguration(String name, List<? extends FieldValueSchemaConfiguration> fields) {
        super(name);

        Assert.notNull(fields);

        Map<String, FieldValueSchemaConfiguration> fieldsMap = new HashMap<String, FieldValueSchemaConfiguration>();
        for (FieldValueSchemaConfiguration field : fields)
            Assert.isNull(fieldsMap.put(field.getName(), field));

        this.fields = Immutables.wrap(fields);
        this.fieldsMap = fieldsMap;
    }

    public List<FieldValueSchemaConfiguration> getFields() {
        return fields;
    }

    public FieldValueSchemaConfiguration findField(String name) {
        Assert.notNull(name);

        return fieldsMap.get(name);
    }

    @Override
    public boolean isCompatible(MetricValueSchemaConfiguration c) {
        Assert.notNull(c);

        FieldMetricValueSchemaConfiguration configuration = (FieldMetricValueSchemaConfiguration) c;
        if (fields.size() > configuration.fields.size())
            return false;

        for (int i = 0; i < fields.size(); i++) {
            FieldValueSchemaConfiguration metric1 = fields.get(i);
            FieldValueSchemaConfiguration metric2 = configuration.fields.get(i);

            if (!metric1.equals(metric2))
                return false;
        }

        return true;
    }

    @Override
    public void buildBaseRepresentations(Set<String> baseRepresentations) {
        for (FieldValueSchemaConfiguration field : fields) {
            String baseRepresentation = field.getBaseRepresentation();
            if (baseRepresentation != null)
                baseRepresentations.add(baseRepresentation);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldMetricValueSchemaConfiguration))
            return false;

        FieldMetricValueSchemaConfiguration configuration = (FieldMetricValueSchemaConfiguration) o;
        return super.equals(configuration) && fields.equals(configuration.fields);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fields);
    }
}
