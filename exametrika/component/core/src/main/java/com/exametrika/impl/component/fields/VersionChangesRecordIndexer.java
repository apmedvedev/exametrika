/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.fields;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.IVersionChangeRecord;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link VersionChangesRecordIndexer} is a version changes record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class VersionChangesRecordIndexer implements IRecordIndexer {
    private IRecordIndexProvider indexProvider;

    public VersionChangesRecordIndexer(IRecordIndexProvider indexProvider) {
        Assert.notNull(indexProvider);

        this.indexProvider = indexProvider;
    }

    @Override
    public void addRecord(Object r, long id) {
        IVersionChangeRecord record = (IVersionChangeRecord) r;
        indexProvider.add(0, record.getTime(), id);
    }

    @Override
    public void removeRecord(Object r) {
        IVersionChangeRecord record = (IVersionChangeRecord) r;
        indexProvider.remove(0, record.getTime());
    }

    @Override
    public void reindex(Object record, long id) {
        Assert.supports(false);
    }
}
