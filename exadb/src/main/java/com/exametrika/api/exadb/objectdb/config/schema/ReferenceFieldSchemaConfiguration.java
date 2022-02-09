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
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.ReferenceField;
import com.exametrika.impl.exadb.objectdb.fields.ReferenceFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.ReferenceFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link ReferenceFieldSchemaConfiguration} represents a configuration of schema of reference field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ReferenceFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final String fieldReference;
    private final boolean required;
    private final boolean owning;
    private final boolean bidirectional;
    private final Set<String> nodeReferences;
    private final String externalSpaceName;
    private final boolean stableOrder;

    public ReferenceFieldSchemaConfiguration(String name, String nodeReference) {
        this(name, name, null, null, nodeReference != null ? Collections.<String>singleton(nodeReference) : null, false, false, false, null, false);
    }

    public ReferenceFieldSchemaConfiguration(String name, boolean stableOrder) {
        this(name, name, null, null, null, false, false, false, null, stableOrder);
    }

    public ReferenceFieldSchemaConfiguration(String name, String nodeReference, String externalSpaceName) {
        this(name, name, null, null, nodeReference != null ? Collections.<String>singleton(nodeReference) : null, false, false, false,
                externalSpaceName, false);
    }

    public ReferenceFieldSchemaConfiguration(String name, String alias, String description, String fieldReference,
                                             Set<String> nodeReferences, boolean required, boolean owning, boolean bidirectional, String externalSpaceName,
                                             boolean stableOrder) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(ReferenceField.class));

        Assert.notNull(bidirectional == (fieldReference != null));
        Assert.isTrue(nodeReferences == null || bidirectional == nodeReferences.isEmpty());

        this.fieldReference = fieldReference;
        this.nodeReferences = Immutables.wrap(nodeReferences);
        this.required = required;
        this.owning = owning;
        this.bidirectional = bidirectional;
        this.externalSpaceName = externalSpaceName;
        this.stableOrder = stableOrder;
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

    public boolean isStableOrder() {
        return stableOrder;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new ReferenceFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof ReferenceFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new ReferenceFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ReferenceFieldSchemaConfiguration))
            return false;

        ReferenceFieldSchemaConfiguration configuration = (ReferenceFieldSchemaConfiguration) o;
        return super.equals(configuration) && bidirectional == configuration.bidirectional &&
                Objects.equals(externalSpaceName, configuration.externalSpaceName) && stableOrder == configuration.stableOrder;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ReferenceFieldSchemaConfiguration))
            return false;

        ReferenceFieldSchemaConfiguration configuration = (ReferenceFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && Objects.equals(fieldReference, configuration.fieldReference) &&
                Objects.equals(nodeReferences, configuration.nodeReferences) && required == configuration.required &&
                owning == configuration.owning && bidirectional == configuration.bidirectional &&
                Objects.equals(externalSpaceName, configuration.externalSpaceName) && stableOrder == configuration.stableOrder;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fieldReference, nodeReferences, required, owning, bidirectional,
                externalSpaceName, stableOrder);
    }
}
