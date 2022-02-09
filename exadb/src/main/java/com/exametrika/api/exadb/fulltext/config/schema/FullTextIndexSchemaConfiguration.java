/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Collections;

import com.exametrika.api.exadb.index.IIndex;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.impl.exadb.fulltext.FullTextIndex;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link FullTextIndexSchemaConfiguration} is a configuration of fulltext index schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FullTextIndexSchemaConfiguration extends IndexSchemaConfiguration {
    public FullTextIndexSchemaConfiguration(String name, int pathIndex) {
        this(name, name, null, pathIndex);
    }

    public FullTextIndexSchemaConfiguration(String name, String alias, String description, int pathIndex) {
        super(name, alias, description, pathIndex, Collections.<String, String>emptyMap());
    }

    @Override
    public String getType() {
        return "fulltext";
    }

    @Override
    public IIndex createIndex(String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        return FullTextIndex.create((IndexManager) indexManager, context.getTransactionProvider(), context.getSchemaSpace(),
                context.getTimeService(), this, context.getConfiguration().getPaths().get(getPathIndex()), filePrefix);
    }

    @Override
    public IIndex openIndex(int id, String filePrefix, IIndexManager indexManager, IDatabaseContext context) {
        return FullTextIndex.open((IndexManager) indexManager, context.getTransactionProvider(), context.getTimeService(), this,
                context.getConfiguration().getPaths().get(getPathIndex()), id, filePrefix);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FullTextIndexSchemaConfiguration))
            return false;

        FullTextIndexSchemaConfiguration configuration = (FullTextIndexSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
