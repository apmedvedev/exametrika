/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.impl.aggregator.fields.LocationField;
import com.exametrika.impl.exadb.objectdb.schema.SimpleFieldSchema;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link LocationFieldSchema} is a location field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LocationFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    private final int indexTotalIndex;
    private int locationTotalIndex;

    public LocationFieldSchema(IndexedLocationFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset);

        this.indexTotalIndex = indexTotalIndex;
    }

    public int getLocationTotalIndex() {
        return locationTotalIndex;
    }

    public void setLocationTotalIndex(int value) {
        this.locationTotalIndex = value;
    }

    @Override
    public int getIndexTotalIndex() {
        return indexTotalIndex;
    }

    @Override
    public IndexedLocationFieldSchemaConfiguration getConfiguration() {
        return (IndexedLocationFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new LocationField((ISimpleField) field);
    }
}
