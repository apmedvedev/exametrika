/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.index;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.NumericRangeFilter;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeFullTextIndex;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.PeriodNode;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.fulltext.IndexNumericField;
import com.exametrika.impl.exadb.objectdb.index.NodeFullTextIndex;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IndexFilter;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFullTextField;


/**
 * The {@link PeriodNodeFullTextIndex} implements {@link INodeFullTextIndex}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodNodeFullTextIndex extends NodeFullTextIndex {
    private final PeriodSpace space;

    public PeriodNodeFullTextIndex(IDatabaseContext context, IFullTextIndex index, int id, PeriodSpace space) {
        super(context, space, index, id);

        Assert.notNull(space);

        this.space = space;
    }

    @Override
    protected <T> T getValue(IDocument document) {
        long id = ((INumericField) document.getFields().get(0)).get().longValue();
        int periodIndex = ((INumericField) document.getFields().get(1)).get().intValue();
        IPeriod period = space.getPeriod(periodIndex);
        return period.findNodeById(id);
    }

    @Override
    public long getId(Object value) {
        INode node = (INode) value;
        return node.getId();
    }

    @Override
    protected IFilter getFilter(IDocumentSchema schema) {
        if (!space.getRawTransaction().isReadOnly() || space.getSchema().getConfiguration().isNonAggregating())
            return new IndexFilter(new TermFilter(new Term(NodeSpaceSchema.DOCUMENT_TYPE_FIELD_NAME,
                    schema.getConfiguration().getDocumentType())));
        else {
            BooleanFilter filter = new BooleanFilter();

            filter.add(new TermFilter(new Term(NodeSpaceSchema.DOCUMENT_TYPE_FIELD_NAME,
                    schema.getConfiguration().getDocumentType())), Occur.MUST);
            filter.add(NumericRangeFilter.newIntRange(CycleSchema.PERIOD_FIELD_NAME + IndexNumericField.PREFIX, -1,
                    space.getPeriodsCount(), true, false), Occur.MUST);

            return new IndexFilter(filter);
        }
    }

    @Override
    protected IDocument createDocument(Object value) {
        INode node = (INode) value;
        IDocumentSchema schema = node.getSchema().getFullTextSchema();
        List<Object> values = new ArrayList<Object>();
        values.add(node.getId());
        values.add(((PeriodNode) node).getPeriod().getPeriodIndex());
        values.add(schema.getConfiguration().getDocumentType());
        for (int i = 0; i < node.getFieldCount(); i++) {
            IField field = node.getField(i);
            if (field.getSchema().getConfiguration().isFullTextIndexed())
                values.add(((IFullTextField) field.getObject()).getFullTextValue());
        }
        return schema.createDocument(values);
    }
}
