/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.index.NodeFullTextIndex;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.fulltext.IndexFilter;


/**
 * The {@link StructuredBlobFullTextIndex} implements {@link INodeFullTextIndex}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StructuredBlobFullTextIndex extends NodeFullTextIndex {
    private final StructuredBlobField field;
    private final NodeSpace space;

    public StructuredBlobFullTextIndex(IDatabaseContext context, IFullTextIndex index, int id, NodeSpace space, StructuredBlobField field) {
        super(context, (IFullTextDocumentSpace) space, index, id);

        Assert.notNull(space);
        Assert.notNull(field);

        this.space = space;
        this.field = field;
    }

    @Override
    protected <T> T getValue(IDocument document) {
        long id = ((INumericField) document.getFields().get(0)).get().longValue();
        return (T) new Pair(id, field.get(id));
    }

    @Override
    public long getId(Object value) {
        Assert.supports(false);
        return 0;
    }

    @Override
    protected IFilter getFilter(IDocumentSchema schema) {
        return new IndexFilter(new TermFilter(new Term(StructuredBlobField.FIELD_ID_FIELD_NAME, field.getFieldId())));
    }

    @Override
    protected IDocument createDocument(Object value) {
        Assert.supports(false);
        return null;
    }

    @Override
    protected IFullTextIndex refreshIndex(int id) {
        return space.getFullTextIndex().getIndex();
    }
}
