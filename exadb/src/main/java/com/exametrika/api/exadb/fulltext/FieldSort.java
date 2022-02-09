/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link FieldSort} is a field sort criterion.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FieldSort {
    private final Kind kind;
    private final String field;
    private final boolean ascending;

    public enum Kind {
        RELEVANCE,

        DOCUMENT,

        FIELD
    }

    ;

    public FieldSort(Kind kind) {
        this(kind, true);
    }

    public FieldSort(Kind kind, boolean ascending) {
        Assert.notNull(kind);
        Assert.isTrue(kind == Kind.RELEVANCE || kind == Kind.DOCUMENT);

        this.kind = kind;
        this.ascending = ascending;

        if (kind == Kind.RELEVANCE)
            this.field = "score";
        else
            this.field = "document";
    }

    public FieldSort(String field) {
        this(field, true);
    }

    public FieldSort(String field, boolean ascending) {
        Assert.notNull(field);

        this.kind = Kind.FIELD;
        this.field = field;
        this.ascending = ascending;
    }

    public Kind getKind() {
        return kind;
    }

    public String getField() {
        return field;
    }

    public boolean isAscending() {
        return ascending;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FieldSort))
            return false;

        FieldSort sort = (FieldSort) o;
        return kind == sort.kind && field.equals(sort.field) && ascending == sort.ascending;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(kind, field, ascending);
    }

    @Override
    public String toString() {
        return field;
    }
}
