/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;
import java.util.Map;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.BlobSpace;
import com.exametrika.impl.exadb.objectdb.fields.BlobStoreField;
import com.exametrika.impl.exadb.objectdb.fields.BlobStoreFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.BlobStoreFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link BlobStoreFieldSchemaConfiguration} represents a configuration of schema of blob store field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class BlobStoreFieldSchemaConfiguration extends FileFieldSchemaConfiguration {
    private boolean allowDeletion;

    public BlobStoreFieldSchemaConfiguration(String name) {
        this(name, name, null, 0, Long.MAX_VALUE, null, PageType.NORMAL, false, Collections.<String, String>emptyMap(), true);
    }

    public BlobStoreFieldSchemaConfiguration(String name, String alias, String description, int pathIndex, long maxFileSize,
                                             String directory, PageType pageType, boolean preload, Map<String, String> properties, boolean allowDeletion) {
        super(name, alias, description, pathIndex, align(maxFileSize, pageType), directory,
                pageType, preload, properties, Memory.getShallowSize(BlobStoreField.class) + Memory.getShallowSize(BlobSpace.class));

        this.allowDeletion = allowDeletion;
    }

    public boolean isAllowDeletion() {
        return allowDeletion;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new BlobStoreFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof BlobStoreFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new BlobStoreFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BlobStoreFieldSchemaConfiguration))
            return false;

        BlobStoreFieldSchemaConfiguration configuration = (BlobStoreFieldSchemaConfiguration) o;
        return super.equals(o) && allowDeletion == configuration.allowDeletion;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BlobStoreFieldSchemaConfiguration))
            return false;

        BlobStoreFieldSchemaConfiguration configuration = (BlobStoreFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowDeletion);
    }

    private static long align(long maxFileSize, PageType pageType) {
        if (maxFileSize == Long.MAX_VALUE || maxFileSize == 0)
            return maxFileSize;

        int pageSize;
        switch (pageType) {
            case SMALL:
                pageSize = Constants.SMALL_PAGE_SIZE;
                break;
            case SMALL_MEDIUM:
                pageSize = Constants.SMALL_MEDIUM_PAGE_SIZE;
                break;
            case MEDIUM:
                pageSize = Constants.MEDIUM_PAGE_SIZE;
                break;
            case LARGE_MEDIUM:
                pageSize = Constants.LARGE_MEDIUM_PAGE_SIZE;
                break;
            case LARGE:
                pageSize = Constants.LARGE_PAGE_SIZE;
                break;
            case EXTRA_LARGE:
                pageSize = Constants.EXTRA_LARGE_PAGE_SIZE;
                break;
            default:
                pageSize = Constants.NORMAL_PAGE_SIZE;
        }
        Assert.isTrue(Numbers.isPowerOfTwo(pageSize) && pageSize >= RawPageTypeConfiguration.MIN_PAGE_SIZE);

        long blockCount = maxFileSize / (BlobSpace.BLOCK_ELEMENT_COUNT * 64 * pageSize);
        if ((maxFileSize % (BlobSpace.BLOCK_ELEMENT_COUNT * 64 * pageSize)) != 0)
            blockCount++;

        return blockCount * BlobSpace.BLOCK_ELEMENT_COUNT * 64 * pageSize;
    }
}
