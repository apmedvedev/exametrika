/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;
import java.util.Map;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.FileField;
import com.exametrika.impl.exadb.objectdb.fields.FileFieldConverter;
import com.exametrika.impl.exadb.objectdb.fields.FileFieldInitializer;
import com.exametrika.impl.exadb.objectdb.schema.FileFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link FileFieldSchemaConfiguration} represents a configuration of schema of file-based field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class FileFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final int pathIndex;
    private final long maxFileSize;
    private final String directory;
    private final PageType pageType;
    private final boolean preload;
    private final Map<String, String> properties;

    public enum PageType {
        SMALL,
        NORMAL,
        SMALL_MEDIUM,
        MEDIUM,
        LARGE_MEDIUM,
        LARGE,
        EXTRA_LARGE
    }

    public FileFieldSchemaConfiguration(String name) {
        this(name, name, null, 0, 0, null, PageType.NORMAL, false, Collections.<String, String>emptyMap());
    }

    public FileFieldSchemaConfiguration(String name, String alias, String description, int pathIndex, long maxFileSize,
                                        String directory, PageType pageType, boolean preload, Map<String, String> properties) {
        this(name, alias, description, pathIndex, maxFileSize, directory, pageType, preload, properties, 0);
    }

    public FileFieldSchemaConfiguration(String name, String alias, String description, int pathIndex, long maxFileSize,
                                        String directory, PageType pageType, boolean preload, Map<String, String> properties, int cacheSize) {
        super(name, alias, description, FileField.HEADER_SIZE, cacheSize + Memory.getShallowSize(FileField.class));

        Assert.isTrue(directory == null || directory.length() <= FileField.MAX_DIRECTORY_NAME_LENGTH);
        Assert.notNull(pageType);
        Assert.notNull(properties);

        this.pathIndex = pathIndex;
        this.maxFileSize = maxFileSize;
        this.directory = directory;
        this.pageType = pageType;
        this.preload = preload;
        this.properties = Immutables.wrap(properties);
    }

    public final int getPathIndex() {
        return pathIndex;
    }

    public final long getMaxFileSize() {
        return maxFileSize;
    }

    public final String getDirectory() {
        return directory;
    }

    public final PageType getPageType() {
        return pageType;
    }

    public final boolean isPreload() {
        return preload;
    }

    public final Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new FileFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof FileFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new FileFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return new FileFieldInitializer();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileFieldSchemaConfiguration))
            return false;

        FileFieldSchemaConfiguration configuration = (FileFieldSchemaConfiguration) o;
        return super.equals(configuration) && pathIndex == configuration.pathIndex &&
                maxFileSize == configuration.maxFileSize && Objects.equals(directory, configuration.directory) &&
                pageType == configuration.pageType && preload == configuration.preload && properties.equals(configuration.properties);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof FileFieldSchemaConfiguration))
            return false;

        FileFieldSchemaConfiguration configuration = (FileFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex &&
                maxFileSize == configuration.maxFileSize && Objects.equals(directory, configuration.directory) &&
                pageType == configuration.pageType && preload == configuration.preload && properties.equals(configuration.properties);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(pathIndex, maxFileSize, directory, pageType, preload, properties);
    }
}
