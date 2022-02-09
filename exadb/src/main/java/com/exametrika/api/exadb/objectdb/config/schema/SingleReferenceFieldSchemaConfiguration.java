/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.SingleReferenceField;
import com.exametrika.impl.exadb.objectdb.fields.SingleReferenceFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.SingleReferenceFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link SingleReferenceFieldSchemaConfiguration} represents a configuration of schema of single reference field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SingleReferenceFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final String fieldReference;
    private final boolean required;
    private final boolean owning;
    private final boolean bidirectional;
    private final Set<String> nodeReferences;
    private final String externalSpaceName;

    public SingleReferenceFieldSchemaConfiguration(String name, String nodeReference) {
        this(name, name, null, null, nodeReference != null ? Collections.<String>singleton(nodeReference) : null,
                false, false, false, null);
    }

    public SingleReferenceFieldSchemaConfiguration(String name, String nodeReference, String externalSpaceName) {
        this(name, name, null, null, nodeReference != null ? Collections.<String>singleton(nodeReference) : null,
                false, false, false, externalSpaceName);
    }

    public SingleReferenceFieldSchemaConfiguration(String name, String alias, String description, String fieldReference,
                                                   Set<String> nodeReferences, boolean required, boolean owning, boolean bidirectional, String externalSpaceName) {
        this(name, alias, description, fieldReference, nodeReferences, required, owning, bidirectional, externalSpaceName, 0, 0);
    }

    public SingleReferenceFieldSchemaConfiguration(String name, String alias, String description, String fieldReference,
                                                   Set<String> nodeReferences, boolean required, boolean owning, boolean bidirectional, String externalSpaceName, int size, int cacheSize) {
        super(name, alias, description, size + SingleReferenceField.HEADER_SIZE, cacheSize + Memory.getShallowSize(SingleReferenceField.class));

        Assert.notNull(bidirectional == (fieldReference != null));
        Assert.isTrue(nodeReferences == null || bidirectional == nodeReferences.isEmpty());

        this.fieldReference = fieldReference;
        this.nodeReferences = Immutables.wrap(nodeReferences);
        this.required = required;
        this.owning = owning;
        this.bidirectional = bidirectional;
        this.externalSpaceName = externalSpaceName;
    }

    public String getFieldReference() {
        return fieldReference;
    }

    public Set<String> getNodeReferences() {
        return nodeReferences;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isOwning() {
        return owning;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public String getExternalSpaceName() {
        return externalSpaceName;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new SingleReferenceFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof SingleReferenceFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new SingleReferenceFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SingleReferenceFieldSchemaConfiguration))
            return false;

        SingleReferenceFieldSchemaConfiguration configuration = (SingleReferenceFieldSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(fieldReference, configuration.fieldReference) &&
                Objects.equals(nodeReferences, configuration.nodeReferences) && required == configuration.required &&
                owning == configuration.owning && bidirectional == configuration.bidirectional &&
                Objects.equals(externalSpaceName, configuration.externalSpaceName);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof SingleReferenceFieldSchemaConfiguration))
            return false;

        SingleReferenceFieldSchemaConfiguration configuration = (SingleReferenceFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && bidirectional == configuration.bidirectional &&
                Objects.equals(externalSpaceName, configuration.externalSpaceName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fieldReference, nodeReferences, required, owning, bidirectional,
                externalSpaceName);
    }
}
