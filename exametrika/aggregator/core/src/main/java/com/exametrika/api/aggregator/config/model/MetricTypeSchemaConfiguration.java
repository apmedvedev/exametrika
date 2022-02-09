/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.values.config.MetricValueSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link MetricTypeSchemaConfiguration} is a aggregation metric type schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MetricTypeSchemaConfiguration extends Configuration {
    private final String name;
    private final MetricValueSchemaConfiguration fields;
    private final List<MetricRepresentationSchemaConfiguration> representations;
    private final Map<String, MetricRepresentationSchemaConfiguration> representationsMap;

    public MetricTypeSchemaConfiguration(String name, MetricValueSchemaConfiguration fields,
                                         List<? extends MetricRepresentationSchemaConfiguration> representations) {
        Assert.notNull(name);
        Assert.notNull(fields);

        this.name = name;
        this.fields = fields;

        Map<String, MetricRepresentationSchemaConfiguration> representationsMap = new HashMap<String, MetricRepresentationSchemaConfiguration>();
        for (MetricRepresentationSchemaConfiguration field : representations)
            Assert.isNull(representationsMap.put(field.getName(), field));

        this.representations = Immutables.wrap(representations);
        this.representationsMap = representationsMap;
    }

    public String getName() {
        return name;
    }

    public MetricValueSchemaConfiguration getFields() {
        return fields;
    }

    public List<MetricRepresentationSchemaConfiguration> getRepresentations() {
        return representations;
    }

    public MetricRepresentationSchemaConfiguration findRepresentation(String name) {
        Assert.notNull(name);

        return representationsMap.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MetricTypeSchemaConfiguration))
            return false;

        MetricTypeSchemaConfiguration configuration = (MetricTypeSchemaConfiguration) o;
        return name.equals(configuration.name) && fields.equals(configuration.fields) &&
                representations.equals(configuration.representations);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, fields, representations);
    }

    @Override
    public String toString() {
        return name;
    }
}
