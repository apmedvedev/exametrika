/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermFilter;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IndexFilter;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFullTextField;


/**
 * The {@link ObjectNodeFullTextIndex} implements {@link INodeFullTextIndex}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectNodeFullTextIndex extends NodeFullTextIndex {
    private final ObjectSpace space;

    public ObjectNodeFullTextIndex(IDatabaseContext context, IFullTextIndex index, int id, ObjectSpace space) {
        super(context, space, index, id);

        Assert.notNull(space);

        this.space = space;
    }

    @Override
    protected <T> T getValue(IDocument document) {
        long id = ((INumericField) document.getFields().get(0)).get().longValue();
        return space.findNodeById(id);
    }

    @Override
    public long getId(Object value) {
        INode node = (INode) value;
        return node.getId();
    }

    @Override
    protected IFilter getFilter(IDocumentSchema schema) {
        return new IndexFilter(new TermFilter(new Term(NodeSpaceSchema.DOCUMENT_TYPE_FIELD_NAME,
                schema.getConfiguration().getDocumentType())));
    }

    @Override
    protected IDocument createDocument(Object value) {
        INode node = (INode) value;
        IDocumentSchema schema = node.getSchema().getFullTextSchema();
        List<Object> values = new ArrayList<Object>();
        values.add(node.getId());
        values.add(schema.getConfiguration().getDocumentType());
        for (int i = 0; i < node.getFieldCount(); i++) {
            IField field = node.getField(i);
            if (field.getSchema().getConfiguration().isFullTextIndexed())
                values.add(((IFullTextField) field.getObject()).getFullTextValue());
        }
        return schema.createDocument(values);
    }
}
