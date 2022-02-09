/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;

import com.exametrika.api.exadb.fulltext.FieldSort;
import com.exametrika.api.exadb.fulltext.ISearchResult;
import com.exametrika.api.exadb.fulltext.ISearchResultElement;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.FieldSort.Kind;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link SearchResult} is a search result.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SearchResult implements ISearchResult {
    private final int totalCount;
    private final Sort sort;
    private final List<ISearchResultElement> elements;

    public SearchResult(int totalCount, Sort sort, List<ISearchResultElement> elements) {
        Assert.notNull(elements);

        this.totalCount = totalCount;
        this.sort = sort;
        this.elements = Immutables.wrap(elements);
    }

    @Override
    public int getTotalCount() {
        return totalCount;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public List<ISearchResultElement> getTopElements() {
        return elements;
    }

    public static ISearchResult createResult(FullTextIndex index, TopDocs result, int modCount) {
        Sort sort;
        if (result instanceof TopFieldDocs)
            sort = createSort(((TopFieldDocs) result).fields);
        else
            sort = null;

        List<SearchResultElement> elements = new ArrayList<SearchResultElement>();
        for (int i = 0; i < result.scoreDocs.length; i++)
            elements.add(new SearchResultElement(index, result.scoreDocs[i].doc, modCount));

        return new SearchResult(result.totalHits, sort, (List) elements);
    }

    private static Sort createSort(SortField[] fields) {
        FieldSort[] fieldSorts = new FieldSort[fields.length];
        for (int i = 0; i < fields.length; i++)
            fieldSorts[i] = createFieldSort(fields[i]);

        return new Sort(fieldSorts);
    }

    private static FieldSort createFieldSort(SortField fieldSort) {
        if (fieldSort.getType() == Type.SCORE)
            return new FieldSort(Kind.RELEVANCE, !fieldSort.getReverse());
        else if (fieldSort.getType() == Type.DOC)
            return new FieldSort(Kind.DOCUMENT, !fieldSort.getReverse());
        else {
            String field;
            if (fieldSort.getType() == Type.STRING)
                field = fieldSort.getField();
            else
                field = fieldSort.getField().substring(0, fieldSort.getField().length() - IndexNumericField.PREFIX.length());
            return new FieldSort(field, !fieldSort.getReverse());
        }
    }
}
