/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.config.schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.CustomHistogramValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.LogarithmicHistogramValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StatisticsValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.UniformHistogramValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyIndexSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AnomalyValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.BackgroundRootSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComputedMetricSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CustomHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ErrorsIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExitPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExpressionIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExpressionIndexSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ForecastRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ForecastValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.HealthIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InfoSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InstanceRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.IntermediateExitPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogarithmicHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameMetricAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PercentageRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PrimaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.RateRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SecondaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SimpleComponentBindingStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SimpleErrorAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SimpleMeasurementFilterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SimpleMetricAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SimpleScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackCounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackErrorLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackIdsMetricSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackNameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StatisticsRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.UniformHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.WorkloadIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.ArchiveOperationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LocationKeyNormalizerSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodAggregationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodDatabaseExtensionSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.SimpleArchivePolicySchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.SimpleTruncationPolicySchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.TruncationOperationSchemaConfiguration;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.impl.aggregator.schema.AggregationSchemaBuilder;
import com.exametrika.impl.exadb.core.config.schema.SchemaLoadContext;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationAnalyzerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationLogFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.BehaviorTypeLabelStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentBindingStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDeletionStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.ArchivePolicySchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.TruncationPolicySchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;


/**
 * The {@link PeriodSchemaLoader} is a loader of perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("ArchiveOperation")) {
            NameFilter spaceFilter = loadNameFilter(element.get("spaceFilter", null));
            List<String> periods = JsonUtils.toList((JsonArray) element.get("periods", null));
            ArchivePolicySchemaConfiguration archivePolicy = loadArchivePolicy((JsonObject) element.get("archivePolicy"), context);
            ArchiveStoreSchemaConfiguration archiveStore = load(null, null, (JsonObject) element.get("archiveStore"), context);

            return new ArchiveOperationSchemaConfiguration(spaceFilter, periods, archivePolicy, archiveStore);
        } else if (type.equals("TruncationOperation")) {
            NameFilter spaceFilter = loadNameFilter(element.get("spaceFilter", null));
            List<String> periods = JsonUtils.toList((JsonArray) element.get("periods", null));
            TruncationPolicySchemaConfiguration truncationPolicy = loadTruncationPolicy((JsonObject) element.get("truncationPolicy"), context);

            return new TruncationOperationSchemaConfiguration(spaceFilter, periods, truncationPolicy);
        } else if (type.equals("PeriodSpace")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            long fullTextPathIndex = element.get("fullTextPathIndex");
            List<PeriodSchemaConfiguration> periods = loadPeriods((JsonObject) element.get("periods"), context);

            return new PeriodSpaceSchemaConfiguration(name, alias, description, periods, (int) pathIndex, (int) fullTextPathIndex,
                    false, CombineType.STACK, false);
        } else if (type.equals("IndexedLocationField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");

            return new IndexedLocationFieldSchemaConfiguration(name, alias, description, (int) pathIndex);
        } else if (type.equals("PeriodDatabaseExtension")) {
            String alias = element.get("alias", PeriodDatabaseExtensionSchemaConfiguration.NAME);
            String description = element.get("description", null);
            NameSpaceSchemaConfiguration nameSpace = loadNameSpace("nameSpace", (JsonObject) element.get("nameSpace"));

            return new PeriodDatabaseExtensionSchemaConfiguration(alias, description, nameSpace);
        } else if (type.equals("LocationKeyNormalizer"))
            return new LocationKeyNormalizerSchemaConfiguration();
        else if (type.equals("LogAggregationField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            AggregationComponentTypeSchemaConfiguration componentType = loadComponentType(name, (JsonObject) element.get("componentType"), context);
            String blobStoreField = element.get("blobStoreField");

            return new LogAggregationFieldSchemaConfiguration(name, alias, description, blobStoreField, componentType);
        } else if (type.equals("PeriodAggregationField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            String aggregationLogNode = element.get("aggregationLogNode", null);
            AggregationComponentTypeSchemaConfiguration componentType = loadComponentType(name, (JsonObject) element.get("componentType"),
                    context);

            return new PeriodAggregationFieldSchemaConfiguration(name, alias, description, componentType, aggregationLogNode);
        } else if (type.equals("AggregationSchema")) {
            loadAggregationSchema(element, context);
            return null;
        } else if (type.equals("PeriodTypeAggregationSchema")) {
            Set<AggregationComponentTypeSchemaConfiguration> componentTypes = new LinkedHashSet<AggregationComponentTypeSchemaConfiguration>();
            for (Map.Entry<String, Object> entry : element)
                componentTypes.add(loadComponentType(entry.getKey(), (JsonObject) entry.getValue(), context));

            return componentTypes;
        } else if (type.equals("SimpleMeasurementFilter")) {
            NameFilter scopeFilter = loadNameFilter(element.get("scopeFilter", null));
            NameFilter metricFilter = loadNameFilter(element.get("metricFilter", null));

            return new SimpleMeasurementFilterSchemaConfiguration(scopeFilter, metricFilter);
        } else
            throw new InvalidConfigurationException();
    }

    private void loadAggregationSchema(JsonObject element, ILoadContext context) {
        SchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);
        ModuleSchemaConfiguration currentModule = loadContext.getCurrentModule();

        long version = element.get("version");
        String combineTypeStr = element.get("combineType");
        CombineType combineType;
        if (combineTypeStr.equals("stack"))
            combineType = CombineType.STACK;
        else if (combineTypeStr.equals("transaction"))
            combineType = CombineType.TRANSACTION;
        else if (combineTypeStr.equals("node"))
            combineType = CombineType.NODE;
        else if (combineTypeStr.equals("all"))
            combineType = CombineType.ALL;
        else
            combineType = Assert.error();

        List<PeriodTypeSchemaConfiguration> periodTypes = new ArrayList<PeriodTypeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("periodTypes"))
            periodTypes.add(loadPeriodType(entry.getKey(), (JsonObject) entry.getValue(), context));

        AggregationSchemaConfiguration aggregationSchema = new AggregationSchemaConfiguration(periodTypes, combineType, (int) version);
        AggregationSchemaBuilder builder = new AggregationSchemaBuilder();
        builder.buildSchema(aggregationSchema, currentModule);
    }

    private PeriodTypeSchemaConfiguration loadPeriodType(String name, JsonObject element, ILoadContext context) {
        StandardSchedulePeriodSchemaConfiguration period = load(null, "StandardSchedulePeriod", element.get("period"), context);
        long cyclePeriodCount = element.get("cyclePeriodCount");
        boolean nonAggregating = element.get("nonAggregating");
        String parentDomain = element.get("parentDomain", null);

        Set<AggregationComponentTypeSchemaConfiguration> componentTypes = new LinkedHashSet<AggregationComponentTypeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("componentTypes"))
            componentTypes.add(loadComponentType(entry.getKey(), (JsonObject) entry.getValue(), context));

        return new PeriodTypeSchemaConfiguration(name, componentTypes, period, (int) cyclePeriodCount, nonAggregating,
                parentDomain);
    }

    private AggregationComponentTypeSchemaConfiguration loadComponentType(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("NameComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies = loadScopeAggregationStrategies(
                    (JsonArray) element.get("scopeAggregationStrategies"), context);

            List<MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies = loadMetricAggregationStrategies(
                    (JsonArray) element.get("metricAggregationStrategies"), context);

            AggregationFilterSchemaConfiguration aggregationFilter = load(null, null, element.get("aggregationFilter", null), context);
            boolean allowTransferDerived = element.get("allowTransferDerived");
            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");

            List<ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies = new ArrayList<ComponentDiscoveryStrategySchemaConfiguration>();
            for (Object child : (JsonArray) element.get("componentDiscoveryStrategies")) {
                ComponentDiscoveryStrategySchemaConfiguration componentDiscoveryStrategy = load(null, null, child, context);
                componentDiscoveryStrategies.add(componentDiscoveryStrategy);
            }

            ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy = load(null, null, element.get("componentDeletionStrategy", null), context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            return new NameSchemaConfiguration(name, metricTypes, hasLog, scopeAggregationStrategies,
                    metricAggregationStrategies, aggregationFilter, filter, componentBindingStrategies, ruleRepresentation, analyzers,
                    allowTransferDerived, allowHierarchyAggregation, componentDiscoveryStrategies, componentDeletionStrategy);
        } else if (type.equals("StackNameComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies = loadScopeAggregationStrategies(
                    (JsonArray) element.get("scopeAggregationStrategies"), context);

            List<MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies = loadMetricAggregationStrategies(
                    (JsonArray) element.get("metricAggregationStrategies"), context);

            AggregationFilterSchemaConfiguration aggregationFilter = load(null, null, element.get("aggregationFilter", null), context);
            boolean allowTransferDerived = element.get("allowTransferDerived");
            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            return new StackNameSchemaConfiguration(name, metricTypes, hasLog, scopeAggregationStrategies,
                    metricAggregationStrategies, aggregationFilter, filter, componentBindingStrategies, ruleRepresentation,
                    analyzers, allowTransferDerived, allowHierarchyAggregation);
        } else if (type.equals("BackgroundRootComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies = loadScopeAggregationStrategies(
                    (JsonArray) element.get("scopeAggregationStrategies"), context);

            boolean allowStackNameAggregation = element.get("allowStackNameAggregation");
            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");
            String stackNameComponentType = element.get("stackNameComponentType", null);
            boolean allowAnomaliesCorrelation = element.get("allowAnomaliesCorrelation");
            String anomaliesComponentType = element.get("anomaliesComponentType", null);
            return new BackgroundRootSchemaConfiguration(name, metricTypes, hasLog, scopeAggregationStrategies,
                    filter, componentBindingStrategies, ruleRepresentation, analyzers, stackNameComponentType, allowHierarchyAggregation,
                    allowStackNameAggregation, allowAnomaliesCorrelation, anomaliesComponentType);
        } else if (type.equals("StackComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            String stackNameComponentType = element.get("stackNameComponentType", null);
            return new StackSchemaConfiguration(name, metricTypes, hasLog, filter, componentBindingStrategies,
                    ruleRepresentation, analyzers, stackNameComponentType);
        } else if (type.equals("PrimaryEntryPointComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies = loadScopeAggregationStrategies(
                    (JsonArray) element.get("scopeAggregationStrategies"), context);

            boolean allowStackNameAggregation = element.get("allowStackNameAggregation");
            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");
            boolean allowTransactionFailureDependenciesAggregation = element.get("allowTransactionFailureDependenciesAggregation");
            String stackNameComponentType = element.get("stackNameComponentType", null);
            String transactionFailureDependenciesComponentType = element.get("transactionFailureDependenciesComponentType", null);

            List<ComponentDiscoveryStrategySchemaConfiguration> componentDiscoveryStrategies = new ArrayList<ComponentDiscoveryStrategySchemaConfiguration>();
            for (Object child : (JsonArray) element.get("componentDiscoveryStrategies")) {
                ComponentDiscoveryStrategySchemaConfiguration componentDiscoveryStrategy = load(null, null, child, context);
                componentDiscoveryStrategies.add(componentDiscoveryStrategy);
            }

            ComponentDeletionStrategySchemaConfiguration componentDeletionStrategy = load(null, null,
                    element.get("componentDeletionStrategy", null), context);

            boolean allowAnomaliesCorrelation = element.get("allowAnomaliesCorrelation");
            String anomaliesComponentType = element.get("anomaliesComponentType", null);

            return new PrimaryEntryPointSchemaConfiguration(name, metricTypes, hasLog, scopeAggregationStrategies,
                    filter, componentBindingStrategies, ruleRepresentation, analyzers,
                    stackNameComponentType, allowHierarchyAggregation, allowStackNameAggregation,
                    transactionFailureDependenciesComponentType, allowTransactionFailureDependenciesAggregation,
                    componentDiscoveryStrategies, componentDeletionStrategy, allowAnomaliesCorrelation, anomaliesComponentType);
        } else if (type.equals("SecondaryEntryPointComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            String stackNameComponentType = element.get("stackNameComponentType", null);
            return new SecondaryEntryPointSchemaConfiguration(name, metricTypes, hasLog, filter, componentBindingStrategies,
                    ruleRepresentation, analyzers, stackNameComponentType);
        } else if (type.equals("ExitPointComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            String stackNameComponentType = element.get("stackNameComponentType", null);
            return new ExitPointSchemaConfiguration(name, metricTypes, hasLog, filter, componentBindingStrategies,
                    ruleRepresentation, analyzers, stackNameComponentType);
        } else if (type.equals("IntermediateExitPointComponentType")) {
            boolean hasLog = element.get("hasLog");
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            String stackNameComponentType = element.get("stackNameComponentType", null);
            return new IntermediateExitPointSchemaConfiguration(name, metricTypes, hasLog, filter, componentBindingStrategies,
                    ruleRepresentation, analyzers, stackNameComponentType);
        } else if (type.equals("StackLogComponentType")) {
            boolean hasLog = element.get("hasLog");
            List<MetricTypeSchemaConfiguration> metricTypes = loadMetricTypes(element, context);
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);

            List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = loadComponentBindingStrategies(
                    (JsonArray) element.get("componentBindingStrategies"), context);
            String ruleRepresentation = element.get("ruleRepresentation", null);

            List<AggregationAnalyzerSchemaConfiguration> analyzers = loadAnalyzers((JsonArray) element.get("analyzers"), context);

            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");
            return new StackLogSchemaConfiguration(name, metricTypes, hasLog, filter, componentBindingStrategies,
                    ruleRepresentation, analyzers, allowHierarchyAggregation);
        } else if (type.equals("StackErrorLogComponentType")) {
            LogSchemaConfiguration metricType = (LogSchemaConfiguration) loadMetricType(name, "Log", (JsonObject) element.get("metricType"), context);
            MeasurementFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);

            boolean allowHierarchyAggregation = element.get("allowHierarchyAggregation");
            boolean allowTypedErrorAggregation = element.get("allowTypedErrorAggregation");
            boolean allowTransactionFailureAggregation = element.get("allowTransactionFailureAggregation");
            String errorComponentType = element.get("errorComponentType", null);
            String transactionFailureComponentType = element.get("transactionFailureComponentType", null);
            NameFilter stackTraceFilter = loadNameFilter(element.get("stackTraceFilter", null));
            NameFilter transactionFailureFilter = loadNameFilter(element.get("transactionFailureFilter", null));
            List<ErrorAggregationStrategySchemaConfiguration> errorAggregationStrategies = new ArrayList<ErrorAggregationStrategySchemaConfiguration>();
            for (Object child : (JsonArray) element.get("errorAggregationStrategies")) {
                JsonObject childElement = (JsonObject) child;
                String childType = getType(childElement);
                if (childType.equals("SimpleErrorAggregationStrategy")) {
                    String pattern = childElement.get("pattern", null);
                    String prefix = childElement.get("prefix", null);
                    errorAggregationStrategies.add(new SimpleErrorAggregationStrategySchemaConfiguration(pattern, prefix));
                } else
                    errorAggregationStrategies.add((ErrorAggregationStrategySchemaConfiguration) load(null, null, child, context));
            }
            boolean transactionFailureErrorLog = element.get("transactionFailureErrorLog");

            return new StackErrorLogSchemaConfiguration(name, metricType, filter,
                    allowHierarchyAggregation, allowTypedErrorAggregation, allowTransactionFailureAggregation,
                    errorComponentType, transactionFailureComponentType, stackTraceFilter, errorAggregationStrategies,
                    transactionFailureFilter, transactionFailureErrorLog);
        } else
            return Assert.error();
    }

    private List<MetricTypeSchemaConfiguration> loadMetricTypes(JsonObject element, ILoadContext context) {
        List<MetricTypeSchemaConfiguration> metricTypes = new ArrayList<MetricTypeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("metricTypes"))
            metricTypes.add(loadMetricType(entry.getKey(), null, (JsonObject) entry.getValue(), context));
        return metricTypes;
    }

    private List<MetricRepresentationSchemaConfiguration> loadMetricRepresentations(String type, JsonObject object,
                                                                                    ILoadContext context) {
        List<MetricRepresentationSchemaConfiguration> list = new ArrayList<MetricRepresentationSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : object) {
            if (entry.getValue() instanceof JsonObject)
                list.add(loadMetricRepresentation(type, entry.getKey(), (JsonObject) entry.getValue(), context));
            else
                list.add(loadMetricRepresentation(type, entry.getKey(), entry.getValue(), context));
        }

        return list;
    }

    private MetricRepresentationSchemaConfiguration loadMetricRepresentation(String type, String name, JsonObject element,
                                                                             ILoadContext context) {
        type = getType(element, type);

        if (type.equals("ObjectMetricRepresentation"))
            return new ObjectRepresentationSchemaConfiguration(name);
        else if (type.equals("NameMetricRepresentation")) {
            List<FieldRepresentationSchemaConfiguration> fields = loadFieldRepresentations((JsonArray) element.get("fields"), context);
            return new NameRepresentationSchemaConfiguration(name, fields);
        } else if (type.equals("StackMetricRepresentation")) {
            List<FieldRepresentationSchemaConfiguration> fields = loadFieldRepresentations((JsonArray) element.get("fields"), context);
            return new StackRepresentationSchemaConfiguration(name, fields);
        } else if (type.equals("AnomalyIndexRepresentation"))
            return new AnomalyIndexRepresentationSchemaConfiguration(name);
        else if (type.equals("ExpressionIndexRepresentation") || type.equals("CompoundExpressionIndexRepresentation")) {
            String expression = element.get("expression");
            return new ExpressionIndexRepresentationSchemaConfiguration(name, expression);
        } else if (type.equals("WorkloadIndexRepresentation"))
            return new WorkloadIndexRepresentationSchemaConfiguration(name);
        else if (type.equals("ErrorsIndexRepresentation"))
            return new ErrorsIndexRepresentationSchemaConfiguration(name);
        else if (type.equals("HealthIndexRepresentation"))
            return new HealthIndexRepresentationSchemaConfiguration(name);
        else
            return load(name, type, element, context);
    }

    private MetricRepresentationSchemaConfiguration loadMetricRepresentation(String type, String name, Object element,
                                                                             ILoadContext context) {
        if (type.equals("CompoundExpressionIndexRepresentation"))
            return new ExpressionIndexRepresentationSchemaConfiguration(name, (String) element);
        else
            return load(name, type, element, context);
    }

    private MetricTypeSchemaConfiguration loadMetricType(String name, String defaultType, JsonObject element, ILoadContext context) {
        String type = getType(element, defaultType);
        if (type.equals("Gauge")) {
            List<FieldValueSchemaConfiguration> fields = loadFields((JsonArray) element.get("fields"), context);
            List<NameRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("NameMetricRepresentation",
                    (JsonObject) element.get("representations"), context);
            boolean sumByGroup = element.get("sumByGroup");
            return new GaugeSchemaConfiguration(name, fields, representations, sumByGroup);
        } else if (type.equals("Counter")) {
            List<FieldValueSchemaConfiguration> fields = loadFields((JsonArray) element.get("fields"), context);
            List<NameRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("NameMetricRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new CounterSchemaConfiguration(name, fields, representations);
        } else if (type.equals("StackCounter")) {
            List<FieldValueSchemaConfiguration> fields = loadFields((JsonArray) element.get("fields"), context);
            List<StackRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("StackMetricRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new StackCounterSchemaConfiguration(name, fields, representations);
        } else if (type.equals("Info")) {
            List<ObjectRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("ObjectMetricRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new InfoSchemaConfiguration(name, representations);
        } else if (type.equals("Log")) {
            boolean fullTextIndex = element.get("fullTextIndex");
            AggregationLogFilterSchemaConfiguration filter = load(null, null, element.get("filter", null), context);
            List<AggregationLogTransformerSchemaConfiguration> transformers = new ArrayList<AggregationLogTransformerSchemaConfiguration>();
            for (Object child : (JsonArray) element.get("transformers")) {
                AggregationLogTransformerSchemaConfiguration transformer = load(null, null, child, context);
                transformers.add(transformer);
            }

            DocumentSchemaFactoryConfiguration documentSchemaFactory = load(null, null, element.get("documentSchemaFactory", null), context);
            List<ObjectRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("ObjectMetricRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new LogSchemaConfiguration(name, representations, filter, transformers, fullTextIndex, documentSchemaFactory);
        } else if (type.equals("AnomalyIndex")) {
            String baseRepresentation = element.get("baseRepresentation");
            long minAnomalyMetricCount = element.get("minAnomalyMetricCount");
            List<AnomalyIndexRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("AnomalyIndexRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new AnomalyIndexSchemaConfiguration(name, baseRepresentation, (int) minAnomalyMetricCount, representations);
        } else if (type.equals("ExpressionIndex")) {
            boolean stored = element.get("stored");
            String baseRepresentation = element.get("baseRepresentation");
            List<ExpressionIndexRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("CompoundExpressionIndexRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new ExpressionIndexSchemaConfiguration(name, stored, baseRepresentation, representations);
        } else if (type.equals("ComputedMetric")) {
            List<MetricRepresentationSchemaConfiguration> representations = loadMetricRepresentations(null,
                    (JsonObject) element.get("representations"), context);
            return new ComputedMetricSchemaConfiguration(name, representations);
        } else if (type.equals("StackIdsMetric")) {
            return new StackIdsMetricSchemaConfiguration(name);
        } else
            return load(name, type, element, context);
    }

    private List<FieldValueSchemaConfiguration> loadFields(JsonArray array, ILoadContext context) {
        List<FieldValueSchemaConfiguration> list = new ArrayList<FieldValueSchemaConfiguration>();
        for (Object element : array)
            list.add(loadField((JsonObject) element, context));

        return list;
    }

    private List<FieldRepresentationSchemaConfiguration> loadFieldRepresentations(JsonArray array, ILoadContext context) {
        List<FieldRepresentationSchemaConfiguration> list = new ArrayList<FieldRepresentationSchemaConfiguration>();
        for (Object element : array)
            list.add(loadFieldRepresentation((JsonObject) element, context));

        return list;
    }

    private FieldValueSchemaConfiguration loadField(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("StandardFields"))
            return new StandardValueSchemaConfiguration();
        else if (type.equals("StatisticsFields"))
            return new StatisticsValueSchemaConfiguration();
        else if (type.equals("UniformHistogramFields")) {
            long minBound = element.get("minBound");
            long maxBound = element.get("maxBound");
            long binCount = element.get("binCount");
            return new UniformHistogramValueSchemaConfiguration(minBound, maxBound, (int) binCount);
        } else if (type.equals("LogarithmicHistogramFields")) {
            long minBound = element.get("minBound");
            long binCount = element.get("binCount");
            return new LogarithmicHistogramValueSchemaConfiguration(minBound, (int) binCount);
        } else if (type.equals("CustomHistogramFields")) {
            List<Long> bounds = new ArrayList<Long>();
            for (Object bound : (JsonArray) element.get("bounds"))
                bounds.add((Long) bound);

            return new CustomHistogramValueSchemaConfiguration(bounds);
        } else if (type.equals("InstanceFields")) {
            long instanceCount = element.get("instanceCount");
            boolean max = element.get("max");
            return new InstanceValueSchemaConfiguration((int) instanceCount, max);
        } else if (type.equals("AnomalyFields") || type.equals("ForecastFields")) {
            String name = element.get("name");
            String baseRepresentation = element.get("baseRepresentation");
            String baseField = element.get("baseField");
            boolean fast = element.get("fast", false);
            boolean sensitivityAutoAdjustment = element.get("sensitivityAutoAdjustment");
            double initialSensitivity = element.get("initialSensitivity");
            double sensitivityIncrement = element.get("sensitivityIncrement");
            double maxSensitivity = element.get("maxSensitivity");
            long initialLearningPeriod = element.get("initialLearningPeriod");
            long initialAdjustmentLearningPeriod = element.get("initialAdjustmentLearningPeriod");
            long anomaliesEstimationPeriod = element.get("anomaliesEstimationPeriod");
            long maxAnomaliesPerEstimationPeriodPercentage = element.get("maxAnomaliesPerEstimationPeriodPercentage");
            long maxAnomaliesPerType = element.get("maxAnomaliesPerType");
            boolean anomalyAutoLabeling = element.get("anomalyAutoLabeling");
            BehaviorTypeLabelStrategySchemaConfiguration behaviorTypeLabelStrategy = load(null, null,
                    element.get("behaviorTypeLabelStrategy", null), context);

            if (type.equals("AnomalyFields"))
                return new AnomalyValueSchemaConfiguration(name, baseRepresentation, baseField, fast, sensitivityAutoAdjustment,
                        (float) initialSensitivity, (float) sensitivityIncrement, (float) maxSensitivity, (int) initialLearningPeriod,
                        (int) initialAdjustmentLearningPeriod, (int) anomaliesEstimationPeriod, (int) maxAnomaliesPerEstimationPeriodPercentage,
                        (int) maxAnomaliesPerType, anomalyAutoLabeling, behaviorTypeLabelStrategy);
            else
                return new ForecastValueSchemaConfiguration(name, baseRepresentation, baseField, sensitivityAutoAdjustment,
                        (float) initialSensitivity, (float) sensitivityIncrement, (float) maxSensitivity, (int) initialLearningPeriod,
                        (int) initialAdjustmentLearningPeriod, (int) anomaliesEstimationPeriod, (int) maxAnomaliesPerEstimationPeriodPercentage,
                        (int) maxAnomaliesPerType, anomalyAutoLabeling, behaviorTypeLabelStrategy);
        } else
            return load(null, type, element, context);
    }

    private FieldRepresentationSchemaConfiguration loadFieldRepresentation(JsonObject element, ILoadContext context) {
        String type = getType(element);
        boolean enabled = element.get("enabled");
        if (type.equals("StandardRepresentation"))
            return new StandardRepresentationSchemaConfiguration(enabled);
        else if (type.equals("StatisticsRepresentation"))
            return new StatisticsRepresentationSchemaConfiguration(enabled);
        else if (type.equals("UniformHistogramRepresentation")) {
            boolean computeValues = element.get("computeValues");
            boolean computePercentages = element.get("computePercentages");
            boolean computeCumulativePercentages = element.get("computeCumulativePercentages");
            List<Integer> percentiles = new ArrayList<Integer>();
            for (Object percentile : (JsonArray) element.get("percentiles"))
                percentiles.add(((Long) percentile).intValue());
            boolean computeScale = element.get("computeScale");
            long minBound = element.get("minBound");
            long maxBound = element.get("maxBound");
            long binCount = element.get("binCount");
            return new UniformHistogramRepresentationSchemaConfiguration(minBound, maxBound, (int) binCount,
                    computeValues, computePercentages, computeCumulativePercentages, computeScale, percentiles, enabled);
        } else if (type.equals("LogarithmicHistogramRepresentation")) {
            boolean computeValues = element.get("computeValues");
            boolean computePercentages = element.get("computePercentages");
            boolean computeCumulativePercentages = element.get("computeCumulativePercentages");
            List<Integer> percentiles = new ArrayList<Integer>();
            for (Object percentile : (JsonArray) element.get("percentiles"))
                percentiles.add(((Long) percentile).intValue());
            boolean computeScale = element.get("computeScale");
            long minBound = element.get("minBound");
            long binCount = element.get("binCount");
            return new LogarithmicHistogramRepresentationSchemaConfiguration(minBound, (int) binCount,
                    computeValues, computePercentages, computeCumulativePercentages, computeScale, percentiles, enabled);
        } else if (type.equals("CustomHistogramRepresentation")) {
            boolean computeValues = element.get("computeValues");
            boolean computePercentages = element.get("computePercentages");
            boolean computeCumulativePercentages = element.get("computeCumulativePercentages");
            List<Integer> percentiles = new ArrayList<Integer>();
            for (Object percentile : (JsonArray) element.get("percentiles"))
                percentiles.add(((Long) percentile).intValue());

            boolean computeScale = element.get("computeScale");
            List<Long> bounds = new ArrayList<Long>();
            for (Object bound : (JsonArray) element.get("bounds"))
                bounds.add((Long) bound);

            return new CustomHistogramRepresentationSchemaConfiguration(bounds,
                    computeValues, computePercentages, computeCumulativePercentages, computeScale, percentiles, enabled);
        } else if (type.equals("InstanceRepresentation"))
            return new InstanceRepresentationSchemaConfiguration(enabled);
        else if (type.equals("RateRepresentation")) {
            String name = element.get("name", null);
            String baseField = element.get("baseField");
            return new RateRepresentationSchemaConfiguration(name, baseField, enabled);
        } else if (type.equals("PeriodRepresentation")) {
            String name = element.get("name", null);
            String navigationType = element.get("navigationType");
            String baseField = element.get("baseField");
            return new PeriodRepresentationSchemaConfiguration(name, navigationType, baseField, enabled);
        } else if (type.equals("PercentageRepresentation")) {
            String name = element.get("name", null);
            String navigationType = element.get("navigationType");
            String navigationArgs = element.get("navigationArgs", null);
            String nodeType = element.get("nodeType", null);
            String baseField = element.get("baseField");
            String currentField = element.get("currentField", baseField);

            return new PercentageRepresentationSchemaConfiguration(name, navigationType, navigationArgs, nodeType, currentField, baseField, enabled);
        } else if (type.equals("AnomalyRepresentation")) {
            String name = element.get("name", null);
            boolean computeBehaviorTypes = element.get("computeBehaviorTypes");
            return new AnomalyRepresentationSchemaConfiguration(name, computeBehaviorTypes, enabled);
        } else if (type.equals("ForecastRepresentation")) {
            String name = element.get("name", null);
            boolean computeBehaviorTypes = element.get("computeBehaviorTypes");
            boolean computePredictions = element.get("computePredictions");
            long predictionsStepCount = element.get("predictionsStepCount");
            return new ForecastRepresentationSchemaConfiguration(name, computeBehaviorTypes, computePredictions,
                    (int) predictionsStepCount, enabled);
        } else
            return load(null, type, element, context);
    }

    private NameSpaceSchemaConfiguration loadNameSpace(String name, JsonObject element) {
        String alias = element.get("alias", name);
        String description = element.get("description", null);
        long pathIndex = element.get("pathIndex");
        long indexPathIndex = element.get("indexPathIndex");
        long maxNameSize = element.get("maxNameSize");
        return new NameSpaceSchemaConfiguration(name, alias, description, (int) pathIndex, (int) indexPathIndex, (int) maxNameSize);
    }

    private List<PeriodSchemaConfiguration> loadPeriods(JsonObject element, ILoadContext context) {
        List<PeriodSchemaConfiguration> periods = new ArrayList<PeriodSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            periods.add(loadPeriod(entry.getKey(), (JsonObject) entry.getValue(), context));

        return periods;
    }

    private PeriodSchemaConfiguration loadPeriod(String name, JsonObject element, ILoadContext context) {
        String alias = element.get("alias", name);
        String description = element.get("description", null);
        StandardSchedulePeriodSchemaConfiguration period = load(null, "StandardSchedulePeriod", element.get("period"), context);
        long cyclePeriodCount = element.get("cyclePeriodCount");
        boolean nonAggregating = element.get("nonAggregating");
        String parentDomain = element.get("parentDomain", null);
        String rootNode = element.get("rootNode", null);
        String cyclePeriodRootNode = element.get("cyclePeriodRootNode", null);
        Set<PeriodNodeSchemaConfiguration> nodes = loadNodes((JsonObject) element.get("nodes"), context);

        return new PeriodSchemaConfiguration(name, alias, description, nodes, rootNode, cyclePeriodRootNode, period,
                (int) cyclePeriodCount, nonAggregating, parentDomain);
    }

    private Set<PeriodNodeSchemaConfiguration> loadNodes(JsonObject element, ILoadContext context) {
        Set<PeriodNodeSchemaConfiguration> nodes = new LinkedHashSet<PeriodNodeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            nodes.add((PeriodNodeSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

        return nodes;
    }

    private NameFilter loadNameFilter(Object element) {
        if (element == null)
            return null;
        else if (element instanceof String)
            return new NameFilter((String) element);

        JsonObject object = (JsonObject) element;
        String expression = object.get("expression", null);
        List<NameFilter> includeFilters = loadNameFilters((JsonArray) object.get("include", null));
        List<NameFilter> excludeFilters = loadNameFilters((JsonArray) object.get("exclude", null));

        return new NameFilter(expression, includeFilters, excludeFilters);
    }

    private List<NameFilter> loadNameFilters(JsonArray element) {
        if (element == null)
            return null;

        List<NameFilter> list = new ArrayList<NameFilter>();
        for (Object e : element)
            list.add(loadNameFilter(e));

        return list;
    }

    private ArchivePolicySchemaConfiguration loadArchivePolicy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("SimpleArchivePolicy")) {
            long maxFileSize = element.get("maxFileSize");
            return new SimpleArchivePolicySchemaConfiguration(maxFileSize);
        } else
            return load(null, type, element, context);
    }

    private TruncationPolicySchemaConfiguration loadTruncationPolicy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("SimpleTruncationPolicy")) {
            long minRetentionPeriod = element.get("minRetentionPeriod");
            long maxRetentionPeriod = element.get("maxRetentionPeriod");
            long minFreeSpace = element.get("minFreeSpace");
            long pathIndex = element.get("pathIndex");
            return new SimpleTruncationPolicySchemaConfiguration(minRetentionPeriod, maxRetentionPeriod, minFreeSpace, (int) pathIndex);
        } else
            return load(null, type, element, context);
    }

    private List<ScopeAggregationStrategySchemaConfiguration> loadScopeAggregationStrategies(JsonArray element, ILoadContext context) {
        List<ScopeAggregationStrategySchemaConfiguration> scopeAggregationStrategies = new ArrayList<ScopeAggregationStrategySchemaConfiguration>();
        for (Object child : element) {
            ScopeAggregationStrategySchemaConfiguration scopeAggregationStrategy;

            JsonObject object = (JsonObject) child;
            String type = getType(object);
            if (type.equals("SimpleScopeAggregationStrategy"))
                scopeAggregationStrategy = new SimpleScopeAggregationStrategySchemaConfiguration((Boolean) object.get("hasSubScope"));
            else if (type.equals("NameScopeAggregationStrategy"))
                scopeAggregationStrategy = new NameScopeAggregationStrategySchemaConfiguration();
            else
                scopeAggregationStrategy = load(null, null, child, context);

            scopeAggregationStrategies.add(scopeAggregationStrategy);
        }

        return scopeAggregationStrategies;
    }

    private List<MetricAggregationStrategySchemaConfiguration> loadMetricAggregationStrategies(JsonArray element, ILoadContext context) {
        List<MetricAggregationStrategySchemaConfiguration> metricAggregationStrategies = new ArrayList<MetricAggregationStrategySchemaConfiguration>();
        for (Object child : element) {
            MetricAggregationStrategySchemaConfiguration metricAggregationStrategy;

            JsonObject object = (JsonObject) child;
            String type = getType(object);
            if (type.equals("SimpleMetricAggregationStrategy")) {
                String root = object.get("root", null);
                metricAggregationStrategy = new SimpleMetricAggregationStrategySchemaConfiguration(root);
            } else if (type.equals("NameMetricAggregationStrategy")) {
                String root = object.get("root", null);
                metricAggregationStrategy = new NameMetricAggregationStrategySchemaConfiguration(root);
            } else
                metricAggregationStrategy = load(null, null, child, context);

            metricAggregationStrategies.add(metricAggregationStrategy);
        }

        return metricAggregationStrategies;
    }

    private List<ComponentBindingStrategySchemaConfiguration> loadComponentBindingStrategies(JsonArray element, ILoadContext context) {
        List<ComponentBindingStrategySchemaConfiguration> componentBindingStrategies = new ArrayList<ComponentBindingStrategySchemaConfiguration>();
        for (Object child : element) {
            ComponentBindingStrategySchemaConfiguration componentBindingStrategy;

            JsonObject object = (JsonObject) child;
            String type = getType(object);
            if (type.equals("SimpleComponentBindingStrategy")) {
                boolean hasSubScope = object.get("hasSubScope");
                componentBindingStrategy = new SimpleComponentBindingStrategySchemaConfiguration(hasSubScope);
            } else
                componentBindingStrategy = load(null, null, child, context);

            componentBindingStrategies.add(componentBindingStrategy);
        }

        return componentBindingStrategies;
    }

    private List<AggregationAnalyzerSchemaConfiguration> loadAnalyzers(JsonArray element, ILoadContext context) {
        List<AggregationAnalyzerSchemaConfiguration> analyzers = new ArrayList<AggregationAnalyzerSchemaConfiguration>();
        for (Object child : element) {
            AggregationAnalyzerSchemaConfiguration analyzer = load(null, null, child, context);
            analyzers.add(analyzer);
        }

        return analyzers;
    }
}