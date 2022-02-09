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
import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.api.aggregator.schema.IPeriodAggregationFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.aggregator.fields.PeriodAggregationField;
import com.exametrika.impl.exadb.objectdb.schema.ComplexFieldSchema;
import com.exametrika.spi.aggregator.IRuleService;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link PeriodAggregationFieldSchema} is a aggregation field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class PeriodAggregationFieldSchema extends ComplexFieldSchema implements IPeriodAggregationFieldSchema {
    private final boolean logMetric;
    private IFieldSchema aggregationLog;
    private final IComponentValueSerializer serializer;
    private final IComponentAggregator aggregator;
    private int metadataFieldIndex = -1;
    private int analysisFieldIndex = -1;
    private int logReferenceFieldIndex = -1;
    private final List<IComponentRepresentationSchema> representations;
    private final Map<String, IComponentRepresentationSchema> representationsMap;
    private final List<IComponentRepresentationSchema> baseRepresentations;
    private final IComponentRepresentationSchema ruleRepresentation;
    private IPeriodNameManager nameManager;
    private IRuleService ruleService;

    public PeriodAggregationFieldSchema(PeriodAggregationFieldSchemaConfiguration configuration, int index, int offset) {
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
        this.serializer = configuration.getComponentType().getMetrics().createSerializer(true);
        this.aggregator = configuration.getComponentType().getMetrics().createAggregator();

        Set<String> baseRepresentationNames = new HashSet<String>();
        configuration.getComponentType().getMetrics().buildBaseRepresentations(baseRepresentationNames);
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

        if (configuration.getComponentType().getRuleRepresentation() != null) {
            ruleRepresentation = representationsMap.get(configuration.getComponentType().getRuleRepresentation());
            Assert.notNull(ruleRepresentation);
        } else
            ruleRepresentation = null;

        AggregationComponentTypeSchemaConfiguration componentType = configuration.getComponentType();
        if (componentType.isLog())
            logMetric = true;
        else
            logMetric = false;
    }

    @Override
    public PeriodAggregationFieldSchemaConfiguration getConfiguration() {
        return (PeriodAggregationFieldSchemaConfiguration) configuration;
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
    public int getLogReferenceFieldIndex() {
        return logReferenceFieldIndex;
    }

    @Override
    public int getAnalysisFieldIndex() {
        return analysisFieldIndex;
    }

    @Override
    public IFieldSchema getAggregationLog() {
        return aggregationLog;
    }

    @Override
    public IComponentValueSerializer getSerializer() {
        return serializer;
    }

    @Override
    public IComponentAggregator getAggregator() {
        return aggregator;
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
    public IComponentRepresentationSchema getRuleRepresentation() {
        return ruleRepresentation;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new PeriodAggregationField((IComplexField) field);
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
    public void resolveDependencies() {
        super.resolveDependencies();

        metadataFieldIndex = getParent().findField(getConfiguration().getName() +
                PeriodAggregationFieldSchemaConfiguration.METADATA_FIELD_SUFFIX).getIndex();

        if (!getConfiguration().getComponentType().getAnalyzers().isEmpty())
            analysisFieldIndex = getParent().findField(getConfiguration().getName() +
                    PeriodAggregationFieldSchemaConfiguration.ANALYSIS_FIELD_SUFFIX).getIndex();
        else
            analysisFieldIndex = -1;

        if (getConfiguration().getComponentType().hasLog()) {
            IAggregationNodeSchema aggregationLogNode = getParent().getParent().findNode(getConfiguration().getAggregationLogNodeType());
            Assert.notNull(aggregationLogNode);

            aggregationLog = aggregationLogNode.getAggregationField();
            Assert.notNull(aggregationLog);

            logReferenceFieldIndex = getParent().findField(getConfiguration().getName() +
                    PeriodAggregationFieldSchemaConfiguration.LOG_FIELD_SUFFIX).getIndex();
        } else {
            aggregationLog = null;
            logReferenceFieldIndex = -1;
        }
    }
}
