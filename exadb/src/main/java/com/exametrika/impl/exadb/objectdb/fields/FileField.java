/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.File;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.fields.IFileField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.IRawDataFile;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawPageData;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link FileField} is a file-based field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class FileField implements IFileField, IFieldObject {
    public static final int MAX_DIRECTORY_NAME_LENGTH = 16;
    public static final int HEADER_SIZE = 20 + 2 * MAX_DIRECTORY_NAME_LENGTH;// fileIndex(int) + pathIndex(int) + maxFileSize(long) +
    public static final int FILE_INDEX_OFFSET = 0; // directoryNameLength(int) + directoryName(MAX_DIRECTORY_NAME)
    public static final int PATH_INDEX_OFFSET = 4;
    public static final int MAX_FILE_SIZE_OFFSET = 8;
    public static final int DIRECTORY_NAME_LENGTH_OFFSET = 16;
    public static final int DIRECTORY_NAME_OFFSET = 20;
    private final ISimpleField field;
    private int fileIndex;
    private IRawDataFile file;
    private final String cacheType;

    public FileField(ISimpleField field, String cacheType) {
        Assert.notNull(field);
        Assert.notNull(cacheType);

        this.field = field;
        this.cacheType = cacheType;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public FileFieldInitializer getInitializer() {
        IRawReadRegion region = field.getReadRegion();
        FileFieldInitializer initializer = new FileFieldInitializer();
        initializer.setPathIndex(region.readInt(PATH_INDEX_OFFSET));
        initializer.setMaxFileSize(region.readLong(MAX_FILE_SIZE_OFFSET));
        int length = region.readInt(DIRECTORY_NAME_LENGTH_OFFSET);
        if (length > 0)
            initializer.setDirectory(region.readString(DIRECTORY_NAME_OFFSET, length));
        return initializer;
    }

    public void setInitializer(FileFieldInitializer initializer) {
        setInitializer(initializer, true);
    }

    @Override
    public boolean isReadOnly() {
        return field.isReadOnly();
    }

    @Override
    public boolean allowDeletion() {
        return field.allowDeletion();
    }

    @Override
    public IFieldSchema getSchema() {
        return field.getSchema();
    }

    @Override
    public INode getNode() {
        return field.getNode();
    }

    @Override
    public <T> T get() {
        return (T) getFile();
    }

    @Override
    public <T> T getObject() {
        return (T) this;
    }

    @Override
    public void setModified() {
        field.setModified();
    }

    @Override
    public IRawDataFile getFile() {
        return new DataFileProxy(file);
    }

    @Override
    public IRawPage getPage(long pageIndex) {
        return new PageProxy(((Node) field.getNode()).getPage(fileIndex, pageIndex));
    }

    @Override
    public void onCreated(Object primaryKey, Object initalizer) {
        Assert.isNull(primaryKey);

        FileFieldInitializer fieldInitializer = (FileFieldInitializer) initalizer;
        fileIndex = ((Node) field.getNode()).allocateFile();
        IRawWriteRegion region = field.getWriteRegion();
        region.writeInt(FILE_INDEX_OFFSET, fileIndex);
        setInitializer(fieldInitializer, false);
    }

    @Override
    public void onAfterCreated(Object primaryKey, Object initializer) {
    }

    @Override
    public void onOpened() {
        IRawReadRegion region = field.getReadRegion();
        fileIndex = region.readInt(FILE_INDEX_OFFSET);
        FileFieldInitializer initializer = getInitializer();

        bindFile(initializer.getPathIndex(), initializer.getMaxFileSize(), initializer.getDirectory(), false);
    }

    @Override
    public void onDeleted() {
        file.delete();
        file = null;
        fileIndex = 0;
    }

    @Override
    public void onUnloaded() {
    }

    @Override
    public void flush() {
    }

    private void setInitializer(FileFieldInitializer initializer, boolean unbind) {
        IRawWriteRegion region = field.getWriteRegion();
        region.writeInt(PATH_INDEX_OFFSET, initializer.getPathIndex());
        region.writeLong(MAX_FILE_SIZE_OFFSET, initializer.getMaxFileSize());
        if (initializer.getDirectory() != null) {
            region.writeInt(DIRECTORY_NAME_LENGTH_OFFSET, initializer.getDirectory().length());
            region.writeString(DIRECTORY_NAME_OFFSET, initializer.getDirectory());
        } else
            region.writeInt(DIRECTORY_NAME_LENGTH_OFFSET, 0);

        bindFile(initializer.getPathIndex(), initializer.getMaxFileSize(), initializer.getDirectory(), unbind);
    }

    private void bindFile(int pathIndex, long maxFileSize, String directory, boolean unbind) {
        FileFieldSchemaConfiguration configuration = (FileFieldSchemaConfiguration) field.getSchema().getConfiguration();
        if (pathIndex == -1)
            pathIndex = configuration.getPathIndex();
        if (maxFileSize == 0)
            maxFileSize = configuration.getMaxFileSize();
        if (directory == null)
            directory = configuration.getDirectory();

        Node node = ((Node) field.getNode());
        String name = Spaces.getSpaceFileName(node.getSpaceFilesPath() + (directory != null ? (File.separator + directory) : ""), fileIndex);
        IRawTransaction transaction = node.getRawTransaction();
        Assert.checkState(!transaction.isCompleted());

        if (unbind)
            transaction.unbindFile(fileIndex);

        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(name);
        bindInfo.setMaxFileSize(maxFileSize);
        bindInfo.setPageTypeIndex(getPageTypeIndex(configuration.getPageType()));
        if (configuration.isPreload())
            bindInfo.setFlags(RawBindInfo.PRELOAD);

        NodeSpaceSchema spaceSchema = ((NodeSpaceSchema) field.getSchema().getParent().getParent());
        Pair<String, String> pair = spaceSchema.getContext().getCacheCategorizationStrategy(
        ).categorize(new MapBuilder<String, String>(configuration.getProperties())
                .put("type", cacheType)
                .put("spaceName", spaceSchema.getConfiguration().getName())
                .put("domainName", spaceSchema.getParent().getConfiguration().getName())
                .put("name", spaceSchema.getParent().getConfiguration().getName() + "." + spaceSchema.getConfiguration().getName())
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        file = transaction.bindFile(fileIndex, bindInfo);
    }

    private int getPageTypeIndex(PageType pageType) {
        switch (pageType) {
            case SMALL:
                return Constants.SMALL_PAGE_TYPE;
            case SMALL_MEDIUM:
                return Constants.SMALL_MEDIUM_PAGE_TYPE;
            case MEDIUM:
                return Constants.MEDIUM_PAGE_TYPE;
            case LARGE_MEDIUM:
                return Constants.LARGE_MEDIUM_PAGE_TYPE;
            case LARGE:
                return Constants.LARGE_PAGE_TYPE;
            case EXTRA_LARGE:
                return Constants.EXTRA_LARGE_PAGE_TYPE;
            default:
                return Constants.NORMAL_PAGE_TYPE;
        }
    }

    private class DataFileProxy implements IRawDataFile {
        private final IRawDataFile file;

        public DataFileProxy(IRawDataFile file) {
            Assert.notNull(file);

            this.file = file;
        }

        @Override
        public boolean isReadOnly() {
            return FileField.this.isReadOnly();
        }

        @Override
        public boolean isStale() {
            return file.isStale();
        }

        @Override
        public boolean isDeleted() {
            return file.isDeleted();
        }

        @Override
        public int getPageSize() {
            return file.getPageSize();
        }

        @Override
        public long getSize() {
            return file.getSize();
        }

        @Override
        public int getIndex() {
            return file.getIndex();
        }

        @Override
        public String getPath() {
            return file.getPath();
        }


        @Override
        public ReadMode getReadMode() {
            return file.getReadMode();
        }

        @Override
        public void setReadMode(ReadMode readMode) {
            file.setReadMode(readMode);
        }

        @Override
        public String getCategoryType() {
            return file.getCategoryType();
        }

        @Override
        public String getCategory() {
            return file.getCategory();
        }

        @Override
        public void setCategory(String categoryType, String category) {
            file.setCategory(categoryType, category);
        }

        @Override
        public void prefetch(long startPageIndex, long endPageIndex) {
            file.prefetch(startPageIndex, endPageIndex);
        }

        @Override
        public void truncate(long newSize) {
            Assert.checkState(!isReadOnly());

            file.truncate(newSize);
        }

        @Override
        public void delete() {
            Assert.supports(false);
        }
    }

    private class PageProxy implements IRawPage {
        private final IRawPage page;

        public PageProxy(IRawPage page) {
            Assert.notNull(page);

            this.page = page;
        }

        @Override
        public boolean isStale() {
            return page.isStale();
        }

        @Override
        public int getSize() {
            return page.getSize();
        }

        @Override
        public long getIndex() {
            return page.getIndex();
        }

        @Override
        public IRawDataFile getFile() {
            return new DataFileProxy(page.getFile());
        }

        @Override
        public boolean isReadOnly() {
            return FileField.this.isReadOnly();
        }

        @Override
        public IRawReadRegion getReadRegion() {
            return page.getReadRegion();
        }

        @Override
        public IRawWriteRegion getWriteRegion() {
            Assert.checkState(!isReadOnly());
            return page.getWriteRegion();
        }

        @Override
        public IRawPageData getData() {
            return page.getData();
        }

        @Override
        public void setData(IRawPageData data) {
            page.setData(data);
        }
    }
}
