/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.schema;

import java.util.Arrays;

import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.common.utils.Enums;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;

/**
 * The {@link ActionLogDocumentSchemaFactoryConfiguration} represents a configuration of schema of action log node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ActionLogDocumentSchemaFactoryConfiguration extends DocumentSchemaFactoryConfiguration {
    @Override
    public DocumentSchemaConfiguration createSchema() {
        return new DocumentSchemaConfiguration("actionLog",
                Arrays.asList(
                        new NumericFieldSchemaConfiguration("id", DataType.LONG, true, true),
                        new NumericFieldSchemaConfiguration("time", DataType.LONG, true, true),
                        new StringFieldSchemaConfiguration("component", Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS,
                                Option.OMIT_NORMS), new StandardAnalyzerSchemaConfiguration()),
                        new StringFieldSchemaConfiguration("type", Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS,
                                Option.OMIT_NORMS), new StandardAnalyzerSchemaConfiguration()),
                        new StringFieldSchemaConfiguration("state", Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS,
                                Option.OMIT_NORMS), new StandardAnalyzerSchemaConfiguration()),
                        new StringFieldSchemaConfiguration("action", Enums.of(Option.INDEXED, Option.INDEX_DOCUMENTS,
                                Option.OMIT_NORMS), new StandardAnalyzerSchemaConfiguration())
                ),
                new StandardAnalyzerSchemaConfiguration());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ActionLogDocumentSchemaFactoryConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
