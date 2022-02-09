/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link SchemaCache} is a cache of database schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SchemaCache {
    private final DatabaseSchema currentSchema;
    private final List<DatabaseSchema> schemas;

    public SchemaCache(List<DatabaseSchema> schemas) {
        Assert.notNull(schemas);

        if (!schemas.isEmpty())
            currentSchema = schemas.get(schemas.size() - 1);
        else
            currentSchema = null;

        this.schemas = Immutables.wrap(schemas);
    }

    public DatabaseSchema getCurrentSchema() {
        return currentSchema;
    }

    public DatabaseSchema findSchema(long time) {
        int pos = Collections.binarySearch(schemas, time, new PeriodComparator());
        if (pos < 0)
            pos = -(pos + 2);

        if (pos != -1)
            return schemas.get(pos);
        else
            return null;
    }

    public List<DatabaseSchema> getSchemas() {
        return schemas;
    }

    public SchemaCache addSchema(DatabaseSchema schema) {
        Assert.notNull(schema);

        List<DatabaseSchema> schemas = new ArrayList<DatabaseSchema>(this.schemas);
        schemas.add(schema);

        return new SchemaCache(schemas);
    }

    private static class PeriodComparator implements Comparator {
        @Override
        public int compare(Object o1, Object o2) {
            long t1 = ((IDatabaseSchema) o1).getCreationTime();
            long t2 = (Long) o2;
            return (t1 < t2) ? -1 : ((t1 == t2) ? 0 : 1);
        }
    }
}
