/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.schema;

import java.util.List;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;


/**
 * The {@link IAggregationFieldSchema} represents a schema for aggregation field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAggregationFieldSchema extends IFieldSchema {
    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    IAggregationNodeSchema getParent();

    /**
     * Does field contain single log metric type?
     *
     * @return true if field contains single log metric type
     */
    boolean isLogMetric();

    /**
     * Returns index of metadata field.
     *
     * @return index of metadata field or -1 if field does not have corresponding metadata field
     */
    int getMetadataFieldIndex();

    /**
     * Returns serializer of record values.
     *
     * @return serializer of record values
     */
    IComponentValueSerializer getSerializer();

    /**
     * Returns list of supported record representations schemas.
     *
     * @return list of supported record representations schemas
     */
    List<IComponentRepresentationSchema> getRepresentations();

    /**
     * Finds record representation schema by name.
     *
     * @param name representation name
     * @return representation schema or null if schema is not found
     */
    IComponentRepresentationSchema findRepresentation(String name);

    /**
     * Returns list of base representations schemas.
     *
     * @return list of base representations schemas or null if base representations are not used
     */
    List<IComponentRepresentationSchema> getBaseRepresentations();

    /**
     * Returns representation for rules.
     *
     * @return representation for rules or null if rules are not used
     */
    IComponentRepresentationSchema getRuleRepresentation();
}
