/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import java.util.List;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.fulltext.IFullTextIndexControl;


/**
 * The {@link NodeFullTextIndex} implements {@link INodeFullTextIndex}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeFullTextIndex implements INodeFullTextIndex {
    private final IDatabaseContext context;
    private final int id;
    private IFullTextIndex index;
    private final IFullTextDocumentSpace space;

    public NodeFullTextIndex(IDatabaseContext context, IFullTextDocumentSpace space, IFullTextIndex index, int id) {
        Assert.notNull(context);
        Assert.notNull(space);

        this.context = context;
        this.space = space;
        this.index = index;
        this.id = id;

        if (index == null)
            this.index = refreshIndex(id);
    }

    public IFullTextIndex getIndex() {
        if (!index.isStale())
            return index;
        else {
            index = refreshIndex(id);
            return index;
        }
    }

    public void reindex() {
        ((IFullTextIndexControl) getIndex()).reindex();
    }

    @Override
    public INodeSearchResult search(IQuery query, int count) {
        Assert.notNull(query);

        IFilter filter = getFilter(query.getSchema());
        if (filter != null)
            return new NodeSearchResult(this, getIndex().search(query, filter, count));
        else
            return new NodeSearchResult(this, getIndex().search(query, count));
    }

    @Override
    public INodeSearchResult search(IQuery query, Sort sort, int count) {
        Assert.notNull(query);

        IFilter filter = getFilter(query.getSchema());
        if (filter != null)
            return new NodeSearchResult(this, getIndex().search(query, filter, sort, count));
        else
            return new NodeSearchResult(this, getIndex().search(query, sort, count));
    }

    public void add(Object node) {
        getIndex().add(createDocument(node));
    }

    public void update(Object value) {
        getIndex().update(NodeSpaceSchema.NODE_ID_FIELD_NAME, Long.toString(getId(value)), createDocument(value));
    }

    public void remove(Object value) {
        getIndex().remove(NodeSpaceSchema.NODE_ID_FIELD_NAME, Long.toString(getId(value)));
    }

    public void remove(long id) {
        getIndex().remove(NodeSpaceSchema.NODE_ID_FIELD_NAME, Long.toString(id));
    }

    public void unload() {
        index.unload();
    }

    public List<String> beginSnapshot() {
        return getIndex().beginSnapshot();
    }

    public void endSnapshot() {
        getIndex().endSnapshot();
    }

    protected abstract <T> T getValue(IDocument document);

    protected abstract IFilter getFilter(IDocumentSchema schema);

    protected abstract IDocument createDocument(Object value);

    protected abstract long getId(Object value);

    protected IFullTextIndex refreshIndex(int id) {
        IIndexManager indexManager = context.findTransactionExtension(IIndexManager.NAME);
        IFullTextIndex index = indexManager.getIndex(id);
        ((IFullTextIndexControl) index).setDocumentSpace(space);
        return index;
    }
}
