/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.api.aggregator.schema.ILogAggregationFieldSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.fields.LogAggregationField;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.aggregator.IRuleService;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link LogAggregationFieldSchema} is a aggregation log field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class LogAggregationFieldSchema extends StructuredBlobFieldSchema implements ILogAggregationFieldSchema {
    private final boolean logMetric;
    private final IComponentValueSerializer serializer;
    private int metadataFieldIndex = -1;
    private final List<IComponentRepresentationSchema> representations;
    private final Map<String, IComponentRepresentationSchema> representationsMap;
    private List<IComponentRepresentationSchema> baseRepresentations;
    private final IDocumentSchema documentSchema;
    private IPeriodNameManager nameManager;
    private IRuleService ruleService;
    private final IComponentRepresentationSchema ruleRepresentation;

    public LogAggregationFieldSchema(LogAggregationFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        List<IComponentRepresentationSchema> representations = new ArrayList<IComponentRepresentationSchema>();
        Map<String, IComponentRepresentationSchema> representationsMap = new HashMap<String, IComponentRepresentationSchema>();
        for (int i = 0; i < configuration.getComponentType().getRepresentations().size(); i++) {
            ComponentRepresentationSchemaConfiguration representationConfiguration = configuration.getComponentType().getRepresentations().get(i);
            IComponentRepresentationSchema representation = new ComponentRepresentationSchema(configuration.getComponentType().getMetrics(),
                    representationConfiguration, i);

            representations.add(representation);
            representationsMap.put(representationConfiguration.getName(), representation);
        }

        this.representations = Immutables.wrap(representations);
        this.representationsMap = representationsMap;
        this.serializer = configuration.getComponentType().getMetrics().createSerializer(false);

        if (configuration.getComponentType().getRuleRepresentation() != null) {
            ruleRepresentation = representationsMap.get(configuration.getComponentType().getRuleRepresentation());
            Assert.notNull(ruleRepresentation);
        } else
            ruleRepresentation = null;

        AggregationComponentTypeSchemaConfiguration componentType = configuration.getComponentType();
        if (componentType.isLog()) {
            logMetric = true;
            if (((LogSchemaConfiguration) componentType.getMetricTypes().get(0)).isFullTextIndex())
                documentSchema = ((LogSchemaConfiguration) componentType.getMetricTypes().get(0)).getDocument().createSchema().createSchema();
            else
                documentSchema = null;
        } else {
            logMetric = false;
            documentSchema = null;
        }
    }

    @Override
    public LogAggregationFieldSchemaConfiguration getConfiguration() {
        return (LogAggregationFieldSchemaConfiguration) configuration;
    }

    @Override
    public IAggregationNodeSchema getParent() {
        return (IAggregationNodeSchema) super.getParent();
    }

    @Override
    public boolean isLogMetric() {
        return logMetric;
    }

    @Override
    public int getMetadataFieldIndex() {
        return metadataFieldIndex;
    }

    @Override
    public IComponentValueSerializer getSerializer() {
        return serializer;
    }

    @Override
    public List<IComponentRepresentationSchema> getRepresentations() {
        return representations;
    }

    @Override
    public IComponentRepresentationSchema findRepresentation(String name) {
        Assert.notNull(name);

        return representationsMap.get(name);
    }

    @Override
    public List<IComponentRepresentationSchema> getBaseRepresentations() {
        return baseRepresentations;
    }

    @Override
    public IDocumentSchema getDocumentSchema() {
        return documentSchema;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new LogAggregationField((ISimpleField) field);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        metadataFieldIndex = getParent().findField(getConfiguration().getName() +
                LogAggregationFieldSchemaConfiguration.METADATA_FIELD_SUFFIX).getIndex();

        if (getParent().getParent().getConfiguration().isNonAggregating()) {
            Set<String> baseRepresentationNames = new HashSet<String>();
            getConfiguration().getComponentType().getMetrics().buildBaseRepresentations(baseRepresentationNames);
            if (!baseRepresentationNames.isEmpty()) {
                List<IComponentRepresentationSchema> baseRepresentations = new ArrayList<IComponentRepresentationSchema>();
                for (String name : baseRepresentationNames) {
                    IComponentRepresentationSchema schema = representationsMap.get(name);
                    Assert.notNull(schema);
                    baseRepresentations.add(schema);
                }

                this.baseRepresentations = baseRepresentations;
            } else
                this.baseRepresentations = null;
        }
    }

    public IPeriodNameManager getPeriodNameManager() {
        if (nameManager == null)
            nameManager = ((CycleSchema) getParent().getParent()).getContext().getTransactionProvider().getTransaction(
            ).findExtension(IPeriodNameManager.NAME);

        return nameManager;
    }

    public IRuleService getRuleService() {
        if (ruleService == null)
            ruleService = ((CycleSchema) getParent().getParent()).getContext().getTransactionProvider().getTransaction(
            ).findDomainService(IRuleService.NAME);

        return ruleService;
    }

    @Override
    public IComponentRepresentationSchema getRuleRepresentation() {
        return ruleRepresentation;
    }

    @Override
    protected INodeSchema getRootNode() {
        return getParent().getParent().getCyclePeriodRootNode();
    }
}
