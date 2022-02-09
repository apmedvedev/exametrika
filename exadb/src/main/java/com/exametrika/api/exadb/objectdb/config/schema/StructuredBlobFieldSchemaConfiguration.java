/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.StructuredBlobField;
import com.exametrika.impl.exadb.objectdb.fields.StructuredBlobFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link StructuredBlobFieldSchemaConfiguration} represents a configuration of schema of structured blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StructuredBlobFieldSchemaConfiguration extends BlobFieldSchemaConfiguration {
    private final boolean compressed;
    private final Set<String> allowedClasses;
    private final boolean fixedRecord;
    private final int fixedRecordSize;
    private final List<StructuredBlobIndexSchemaConfiguration> indexes;
    private final boolean fullTextIndex;
    private final RecordIndexerSchemaConfiguration recordIndexer;

    public StructuredBlobFieldSchemaConfiguration(String name, String blobStoreNodeType, String blobStoreFieldName) {
        this(name, name, null, blobStoreNodeType, blobStoreFieldName, false, true, null, false, 0,
                Collections.<StructuredBlobIndexSchemaConfiguration>emptyList(), false, null);
    }

    public StructuredBlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreNodeType, String blobStoreFieldName,
                                                  boolean required, boolean compressed, Set<String> allowedClasses, boolean fixedRecord,
                                                  int fixedRecordSize, List<StructuredBlobIndexSchemaConfiguration> indexes,
                                                  boolean fullTextIndex, RecordIndexerSchemaConfiguration recordIndexer) {
        super(name, alias, description, blobStoreNodeType, blobStoreFieldName, required, 8 + indexes.size() * 4,
                Memory.getShallowSize(StructuredBlobField.class));

        Assert.notNull(indexes);
        Assert.isTrue((!indexes.isEmpty() || fullTextIndex) == (recordIndexer != null));

        if (fixedRecord) {
            Assert.isTrue(!compressed);
            Assert.notNull(allowedClasses);
            Assert.isTrue(allowedClasses.size() == 1);
        }

        this.compressed = compressed;
        this.allowedClasses = allowedClasses;
        this.fixedRecord = fixedRecord;
        this.fixedRecordSize = fixedRecordSize;
        this.indexes = Immutables.wrap(indexes);
        this.fullTextIndex = fullTextIndex;
        this.recordIndexer = recordIndexer;
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

    public List<StructuredBlobIndexSchemaConfiguration> getIndexes() {
        return indexes;
    }

    public boolean isFullTextIndex() {
        return fullTextIndex;
    }

    public RecordIndexerSchemaConfiguration getRecordIndexer() {
        return recordIndexer;
    }

    public boolean hasSerializationRegistry() {
        return true;
    }

    @Override
    public boolean hasFullTextIndex() {
        return fullTextIndex;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new StructuredBlobFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof StructuredBlobFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new StructuredBlobFieldConverter();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StructuredBlobFieldSchemaConfiguration))
            return false;

        StructuredBlobFieldSchemaConfiguration configuration = (StructuredBlobFieldSchemaConfiguration) o;
        return super.equals(configuration) && compressed == configuration.compressed &&
                Objects.equals(allowedClasses, configuration.allowedClasses) && fixedRecord == configuration.fixedRecord &&
                fixedRecordSize == configuration.fixedRecordSize && indexes.equals(configuration.indexes) &&
                fullTextIndex == configuration.fullTextIndex && Objects.equals(recordIndexer, configuration.recordIndexer);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StructuredBlobFieldSchemaConfiguration))
            return false;

        StructuredBlobFieldSchemaConfiguration configuration = (StructuredBlobFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed &&
                indexes.equals(configuration.indexes) && fixedRecord == configuration.fixedRecord &&
                fixedRecordSize == configuration.fixedRecordSize &&
                fullTextIndex == configuration.fullTextIndex && Objects.equals(recordIndexer, configuration.recordIndexer);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(compressed, allowedClasses, fixedRecord, fixedRecordSize, indexes,
                fullTextIndex, recordIndexer);
    }
}
