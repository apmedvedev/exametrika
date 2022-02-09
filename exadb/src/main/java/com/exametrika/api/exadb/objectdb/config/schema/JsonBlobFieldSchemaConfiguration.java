/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.schema.JsonBlobFieldSchema;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link JsonBlobFieldSchemaConfiguration} represents a configuration of schema of json blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JsonBlobFieldSchemaConfiguration extends StructuredBlobFieldSchemaConfiguration {
    private final DocumentSchemaFactoryConfiguration documentSchemaFactory;

    public JsonBlobFieldSchemaConfiguration(String name, String alias, String description, String blobStoreFieldName,
                                            boolean fullText, DocumentSchemaFactoryConfiguration documentSchemaFactory) {
        super(name, alias, description, null, blobStoreFieldName, true, false, null, false, 0,
                Collections.<StructuredBlobIndexSchemaConfiguration>emptyList(),
                fullText, fullText ? new JsonRecordIndexerSchemaConfiguration() : null);

        Assert.isTrue(fullText == (documentSchemaFactory != null));

        this.documentSchemaFactory = documentSchemaFactory;
    }

    public DocumentSchemaFactoryConfiguration getDocumentSchemaFactory() {
        return documentSchemaFactory;
    }

    @Override
    public boolean hasSerializationRegistry() {
        return false;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new JsonBlobFieldSchema(this, index, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonBlobFieldSchemaConfiguration))
            return false;

        JsonBlobFieldSchemaConfiguration configuration = (JsonBlobFieldSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(documentSchemaFactory, configuration.documentSchemaFactory);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof JsonBlobFieldSchemaConfiguration))
            return false;

        JsonBlobFieldSchemaConfiguration configuration = (JsonBlobFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && Objects.equals(documentSchemaFactory, configuration.documentSchemaFactory);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(documentSchemaFactory);
    }
}
