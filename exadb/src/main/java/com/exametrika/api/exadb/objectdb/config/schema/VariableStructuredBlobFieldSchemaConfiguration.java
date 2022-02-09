/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.VariableStructuredBlobField;
import com.exametrika.impl.exadb.objectdb.schema.VariableStructuredBlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link VariableStructuredBlobFieldSchemaConfiguration} represents a configuration of schema of variable
 * structured blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class VariableStructuredBlobFieldSchemaConfiguration extends BlobFieldSchemaConfiguration {
    private final boolean compressed;
    private final Set<String> allowedClasses;
    private final boolean fixedRecord;
    private final int fixedRecordSize;

    public VariableStructuredBlobFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName) {
        this(name, name, null, blobStoreNodeType, blobStoreFieldName, false, true, null, false, 0);
    }

    public VariableStructuredBlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType,
                                                          String blobStoreFieldName, boolean required, boolean compressed, Set<String> allowedClasses, boolean fixedRecord,
                                                          int fixedRecordSize) {
        super(name, alias, description, blobStoreNodeType, blobStoreFieldName, required, 8,
                Memory.getShallowSize(VariableStructuredBlobField.class));

        if (fixedRecord) {
            Assert.isTrue(!compressed);
            Assert.notNull(allowedClasses);
            Assert.isTrue(allowedClasses.size() == 1);
        }

        this.compressed = compressed;
        this.allowedClasses = allowedClasses;
        this.fixedRecord = fixedRecord;
        this.fixedRecordSize = fixedRecordSize;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }

    public boolean getFixedRecord() {
        return fixedRecord;
    }

    public int getFixedRecordSize() {
        return fixedRecordSize;
    }

    public boolean hasSerializationRegistry() {
        return true;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new VariableStructuredBlobFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof VariableStructuredBlobFieldSchemaConfiguration;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof VariableStructuredBlobFieldSchemaConfiguration))
            return false;

        VariableStructuredBlobFieldSchemaConfiguration configuration = (VariableStructuredBlobFieldSchemaConfiguration) o;
        return super.equals(configuration) && compressed == configuration.compressed &&
                Objects.equals(allowedClasses, configuration.allowedClasses) && fixedRecord == configuration.fixedRecord &&
                fixedRecordSize == configuration.fixedRecordSize;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof VariableStructuredBlobFieldSchemaConfiguration))
            return false;

        VariableStructuredBlobFieldSchemaConfiguration configuration = (VariableStructuredBlobFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed &&
                fixedRecord == configuration.fixedRecord && fixedRecordSize == configuration.fixedRecordSize;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(compressed, allowedClasses, fixedRecord, fixedRecordSize);
    }
}
