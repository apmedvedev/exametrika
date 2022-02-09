/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.AggregationNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.AggregationServiceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.BackgroundRootNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.ExitPointNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.IntermediateExitPointNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogRootNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PrimaryEntryPointNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.RootNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.SecondaryEntryPointNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackErrorLogNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackLogNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackNameNodeSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.StackNodeSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedUuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link AggregationSchemaBuilder} is a builder of perfdb module schema configuration for specified aggregation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AggregationSchemaBuilder {
    public void buildSchema(AggregationSchemaConfiguration schema, ModuleSchemaConfiguration module) {
        Assert.notNull(schema);

        List<PeriodSchemaConfiguration> periods = buildPeriods(schema);

        PeriodSpaceSchemaConfiguration aggregationSpace = new PeriodSpaceSchemaConfiguration("aggregation", "aggregation",
                "Aggregation space.", periods, 0, 0, true, schema.getCombineType(), true);
        DomainServiceSchemaConfiguration aggregationService = new AggregationServiceSchemaConfiguration(schema);

        DomainSchemaConfiguration aggregationDomain = new DomainSchemaConfiguration("aggregation", "aggregation",
                "Aggregation domain.", Collections.singleton(aggregationSpace), Collections.singleton(aggregationService));

        module.getSchema().addDomain(aggregationDomain);
    }

    private List<PeriodSchemaConfiguration> buildPeriods(AggregationSchemaConfiguration schema) {
        List<PeriodSchemaConfiguration> periods = new ArrayList<PeriodSchemaConfiguration>(schema.getPeriodTypes().size());
        for (PeriodTypeSchemaConfiguration periodType : schema.getPeriodTypes()) {
            Set<PeriodNodeSchemaConfiguration> nodes = buildNodes(periodType, periodType.isNonAggregating());

            periods.add(new PeriodSchemaConfiguration(periodType.getName(), periodType.getName(), null, nodes,
                    periodType.isNonAggregating() ? "logRoot" : "root", "logRoot",
                    periodType.getPeriod(), periodType.getCyclePeriodCount(), periodType.isNonAggregating(), periodType.getParentDomain()));
        }

        return periods;
    }

    private Set<PeriodNodeSchemaConfiguration> buildNodes(PeriodTypeSchemaConfiguration schema, boolean nonAggregating) {
        Set<AggregationComponentTypeSchemaConfiguration> componentTypes = schema.getComponentTypes();

        Set<PeriodNodeSchemaConfiguration> nodes = new LinkedHashSet<PeriodNodeSchemaConfiguration>();

        LogRootNodeSchemaConfiguration logRootNode = new LogRootNodeSchemaConfiguration("logRoot", "logRoot", "Log root node.",
                new IndexedLocationFieldSchemaConfiguration("location"), Arrays.asList(
                new ReferenceFieldSchemaConfiguration("nameNodes", true),
                new ReferenceFieldSchemaConfiguration("backgroundRoots", true),
                new ReferenceFieldSchemaConfiguration("transactionRoots", true),
                new ReferenceFieldSchemaConfiguration("secondaryEntryPoints", true),
                new ReferenceFieldSchemaConfiguration("logs", true),
                new ReferenceFieldSchemaConfiguration("derivedRoots", true),
                new BlobStoreFieldSchemaConfiguration("blobStore", "blobStore", null, 0, Long.MAX_VALUE, null, PageType.SMALL,
                        false, Collections.<String, String>emptyMap(), false)));

        nodes.add(logRootNode);

        if (!nonAggregating) {
            RootNodeSchemaConfiguration rootNode = new RootNodeSchemaConfiguration("root", "root", "Root node.",
                    new IndexedLocationFieldSchemaConfiguration("location"), Arrays.asList(
                    new ReferenceFieldSchemaConfiguration("nameNodes", true),
                    new ReferenceFieldSchemaConfiguration("backgroundRoots", true),
                    new ReferenceFieldSchemaConfiguration("transactionRoots", true),
                    new ReferenceFieldSchemaConfiguration("secondaryEntryPoints", true),
                    new ReferenceFieldSchemaConfiguration("logs", true),
                    new ReferenceFieldSchemaConfiguration("derivedRoots", true)
            ));

            nodes.add(rootNode);
        }

        if (nonAggregating) {
            for (AggregationComponentTypeSchemaConfiguration componentType : componentTypes) {
                FieldSchemaConfiguration aggregationField;
                if (componentType.hasLog())
                    aggregationField = new LogAggregationFieldSchemaConfiguration(componentType.getName(),
                            componentType.getName(), null, "blobStore", componentType);
                else
                    aggregationField = new PeriodAggregationFieldSchemaConfiguration(componentType.getName(),
                            componentType.getName(), null, componentType, null);

                AggregationNodeSchemaConfiguration node = createNode(componentType, aggregationField);
                nodes.add(node);
            }
        } else {
            for (AggregationComponentTypeSchemaConfiguration componentType : componentTypes)
                buildAggregationNodes(nodes, componentType);
        }

        return nodes;
    }

    private void buildAggregationNodes(Set<PeriodNodeSchemaConfiguration> nodes,
                                       AggregationComponentTypeSchemaConfiguration componentType) {
        if (componentType.hasLog()) {
            LogAggregationFieldSchemaConfiguration aggregationField = new LogAggregationFieldSchemaConfiguration(
                    componentType.getName(), componentType.getName(), null, "blobStore", componentType);

            AggregationNodeSchemaConfiguration node = createDerivedLogAggregationNode("nad.", componentType, aggregationField);
            nodes.add(node);
        }

        PeriodAggregationFieldSchemaConfiguration aggregationField = new PeriodAggregationFieldSchemaConfiguration(
                componentType.getName(), componentType.getName(), null, componentType,
                componentType.hasLog() ? ("nad." + componentType.getName()) : null);

        AggregationNodeSchemaConfiguration node = createNode(componentType, aggregationField);
        nodes.add(node);
    }

    private AggregationNodeSchemaConfiguration createNode(AggregationComponentTypeSchemaConfiguration componentType,
                                                          FieldSchemaConfiguration aggregationField) {
        switch (componentType.getKind()) {
            case NAME: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("scopeParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("scopeChildren", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("metricParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("metricChildren", null));

                return new NameNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            case STACK_NAME: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("scopeParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("scopeChildren", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("metricParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("metricChildren", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependencies", null));

                return new StackNameNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            case STACK: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));

                return new StackNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"),
                        componentType, aggregationField, fields);
            }
            case BACKGROUND_ROOT: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));
                fields.add(new ReferenceFieldSchemaConfiguration("exitPoints", null));
                fields.add(new ReferenceFieldSchemaConfiguration("logs", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("scopeParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("scopeChildren", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("anomalies", null));

                return new BackgroundRootNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"),
                        componentType, aggregationField, fields);
            }
            case PRIMARY_ENTRY_POINT: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));
                fields.add(new ReferenceFieldSchemaConfiguration("exitPoints", null));
                fields.add(new ReferenceFieldSchemaConfiguration("logs", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("scopeParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("scopeChildren", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionFailures", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("anomalies", null));

                return new PrimaryEntryPointNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"),
                        componentType, aggregationField, fields);
            }
            case SECONDARY_ENTRY_POINT: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));
                fields.add(new ReferenceFieldSchemaConfiguration("exitPoints", null));
                fields.add(new ReferenceFieldSchemaConfiguration("logs", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("scopeParent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("scopeChildren", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parentExitPoint", null));
                fields.add(new IndexedUuidFieldSchemaConfiguration("stackId", false, 0, IndexType.BTREE, false, true, true, "stackId"));

                return new SecondaryEntryPointNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"),
                        componentType, aggregationField, fields);
            }
            case EXIT_POINT: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));

                return new ExitPointNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            case INTERMEDIATE_EXIT_POINT: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("root", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
                fields.add(new ReferenceFieldSchemaConfiguration("children", null));
                fields.add(new ReferenceFieldSchemaConfiguration("dependents", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("transactionRoot", null));
                fields.add(new SingleReferenceFieldSchemaConfiguration("childEntryPoint", null));
                fields.add(new IndexedUuidFieldSchemaConfiguration("stackId", false, 0, IndexType.BTREE, false, true, true, "stackId"));

                return new IntermediateExitPointNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            case STACK_LOG: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("mainNode", null));

                return new StackLogNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            case STACK_ERROR_LOG: {
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
                fields.add(new SingleReferenceFieldSchemaConfiguration("mainNode", null));

                return new StackErrorLogNodeSchemaConfiguration(componentType.getName(), componentType.getName(),
                        null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
            }
            default:
                return Assert.error();
        }
    }

    private AggregationNodeSchemaConfiguration createDerivedLogAggregationNode(String prefix, AggregationComponentTypeSchemaConfiguration componentType,
                                                                               FieldSchemaConfiguration aggregationField) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));

        return new LogAggregationNodeSchemaConfiguration(prefix + componentType.getName(), prefix + componentType.getName(),
                null, new IndexedLocationFieldSchemaConfiguration("location"), componentType, aggregationField, fields);
    }
}
