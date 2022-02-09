/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.config.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
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
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CustomHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExitPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExpressionIndexRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ExpressionIndexSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ForecastRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ForecastValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InfoSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InstanceRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.IntermediateExitPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogarithmicHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PercentageRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PrimaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.RateRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.SecondaryEntryPointSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackCounterSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackErrorLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackLogSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackNameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StatisticsRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.UniformHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.AggregationServiceSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.schema.AggregationSchemaBuilder;
import com.exametrika.impl.exadb.core.config.DatabaseConfigurationLoader;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.spi.aggregator.IAggregationFilter;
import com.exametrika.spi.aggregator.IAggregationLogFilter;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IBehaviorTypeLabelStrategy;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.aggregator.IMetricAggregationStrategy;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.IScopeAggregationStrategy;
import com.exametrika.spi.aggregator.MetricHierarchy;
import com.exametrika.spi.aggregator.ScopeHierarchy;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationLogFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.AggregationLogTransformerSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.BehaviorTypeLabelStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MeasurementFilterSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.config.schema.DocumentSchemaFactoryConfiguration;

/**
 * The {@link AggregationSchemaLoaderTests} are tests for {@link AggregationSchemaBuilder}.
 *
 * @author Medvedev-A
 * @see DatabaseConfigurationLoader
 */
@Ignore
public class AggregationSchemaLoaderTests {
    public static class TestConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.exadb", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/extension.dbschema", false));
            parameters.typeLoaders.put("TestAggregationLogFilter", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestMeasurementFilter", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestAggregationLogTransformer", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestScopeAggregationStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestMetricAggregationStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestAggregationFilter", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestErrorAggregationStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestDocumentSchemaFactory", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestObjectRepresentation", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestBehaviorTypeLabelStrategy", new TestSchemaConfigurationLoader());

            return parameters;
        }
    }

    public static class TestAggregationLogFilterSchemaConfiguration extends AggregationLogFilterSchemaConfiguration {
        private String pattern;

        public TestAggregationLogFilterSchemaConfiguration(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestAggregationLogFilterSchemaConfiguration))
                return false;

            TestAggregationLogFilterSchemaConfiguration configuration = (TestAggregationLogFilterSchemaConfiguration) o;
            return Objects.equals(pattern, configuration.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pattern);
        }

        @Override
        public IAggregationLogFilter createFilter(IDatabaseContext context) {
            return new TestAggregationLogFilter(pattern);
        }
    }

    public static class TestAggregationLogFilter implements IAggregationLogFilter {
        private final NameFilter filter;

        public TestAggregationLogFilter(String pattern) {
            if (pattern != null)
                filter = new NameFilter(pattern);
            else
                filter = null;
        }

        @Override
        public boolean allow(IPeriodAggregationField field, IAggregationRecord logRecord) {
            if (filter != null)
                return filter.match(logRecord.getValue().toString());
            else
                return true;
        }
    }

    public static class TestMeasurementFilterSchemaConfiguration extends MeasurementFilterSchemaConfiguration {
        private String pattern;

        public TestMeasurementFilterSchemaConfiguration(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestMeasurementFilterSchemaConfiguration))
                return false;

            TestMeasurementFilterSchemaConfiguration configuration = (TestMeasurementFilterSchemaConfiguration) o;
            return Objects.equals(pattern, configuration.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pattern);
        }

        @Override
        public IMeasurementFilter createFilter(IDatabaseContext context) {
            return new TestMeasurementFilter(pattern, context);
        }
    }

    public static class TestMeasurementFilter implements IMeasurementFilter {
        private final NameFilter filter;
        private final IPeriodNameManager nameManager;

        public TestMeasurementFilter(String pattern, IDatabaseContext context) {
            if (pattern != null)
                filter = new NameFilter(pattern);
            else
                filter = null;

            nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
        }

        @Override
        public boolean allow(Measurement measurement) {
            MeasurementId id = (MeasurementId) measurement.getId();
            String scope;
            if (id.getScopeId() != 0)
                scope = nameManager.findById(id.getScopeId()).getName().toString();
            else
                scope = "";
            String metric;
            if (id.getLocationId() != 0)
                metric = nameManager.findById(id.getLocationId()).getName().toString();
            else
                metric = "";

            String location = "scope:" + scope + ", metric:" + metric + ", ";
            if (filter != null)
                return filter.match(location + measurement.toString());
            else
                return true;
        }
    }

    public static class TestAggregationLogTransformerSchemaConfiguration extends AggregationLogTransformerSchemaConfiguration {
        public TestAggregationLogTransformerSchemaConfiguration() {
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestAggregationLogTransformerSchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IAggregationLogTransformer createTransformer(IDatabaseContext context) {
            return new TestAggregationLogTransformer();
        }
    }

    public static class TestAggregationLogTransformer implements IAggregationLogTransformer {
        @Override
        public List<Measurement> transform(IAggregationNode node) {
            List<Measurement> measurements = new ArrayList<Measurement>();
            IPeriodAggregationField field = node.getField(node.getSchema().getAggregationField());

            IComponentValue value = new ComponentValue(Collections.singletonList(
                    new NameValue(Collections.singletonList(new StandardValue(1, 1, 1, 1)))), null);
            MeasurementId id = new MeasurementId(node.getLocation().getScopeId(), node.getLocation().getMetricId(), "logCounter");

            for (@SuppressWarnings("unused") IAggregationRecord record : field.getPeriodRecords())
                measurements.add(new Measurement(id, value, 0, null));
            return measurements;
        }
    }

    public static class TestScopeAggregationStrategySchemaConfiguration extends ScopeAggregationStrategySchemaConfiguration {
        private String prefix;

        public TestScopeAggregationStrategySchemaConfiguration(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestScopeAggregationStrategySchemaConfiguration))
                return false;

            TestScopeAggregationStrategySchemaConfiguration configuration = (TestScopeAggregationStrategySchemaConfiguration) o;
            return Objects.equals(prefix, configuration.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(prefix);
        }

        @Override
        public IScopeAggregationStrategy createStrategy(IDatabaseContext context) {
            return new TestScopeAggregationStrategy(prefix);
        }
    }

    public static class TestScopeAggregationStrategy implements IScopeAggregationStrategy {
        private final String prefix;

        public TestScopeAggregationStrategy(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ScopeHierarchy getAggregationHierarchy(IAggregationNode node) {
            IScopeName scope = node.getScope();
            List<IScopeName> scopes = new ArrayList<IScopeName>();

            StringBuilder nameBuilder = new StringBuilder();
            boolean first = true;
            if (prefix != null) {
                scopes.add(ScopeName.get(prefix));
                nameBuilder.append(prefix);
                first = false;
            }

            for (int i = 0; i < scope.getSegments().size(); i++) {
                if (first)
                    first = false;
                else
                    nameBuilder.append('.');

                nameBuilder.append(scope.getSegments().get(i));
                scopes.add(ScopeName.get(nameBuilder.toString()));
            }

            return new ScopeHierarchy(scopes);
        }

        @Override
        public boolean allowSecondary(boolean transactionAggregation, ISecondaryEntryPointNode node) {
            return false;
        }
    }

    public static class TestMetricAggregationStrategySchemaConfiguration extends MetricAggregationStrategySchemaConfiguration {
        private String prefix;

        public TestMetricAggregationStrategySchemaConfiguration(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestMetricAggregationStrategySchemaConfiguration))
                return false;

            TestMetricAggregationStrategySchemaConfiguration configuration = (TestMetricAggregationStrategySchemaConfiguration) o;
            return Objects.equals(prefix, configuration.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(prefix);
        }

        @Override
        public IMetricAggregationStrategy createStrategy(IDatabaseContext context) {
            return new TestMetricAggregationStrategy(prefix);
        }
    }

    public static class TestMetricAggregationStrategy implements IMetricAggregationStrategy {
        private final String prefix;

        public TestMetricAggregationStrategy(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public MetricHierarchy getAggregationHierarchy(IMetricName metric) {
            List<IMetricName> metrics = new ArrayList<IMetricName>();

            StringBuilder nameBuilder = new StringBuilder();
            boolean first = true;
            if (prefix != null) {
                metrics.add(MetricName.get(prefix));
                nameBuilder.append(prefix);
                first = false;
            }

            if (!metric.isEmpty()) {
                for (int i = 0; i < metric.getSegments().size(); i++) {
                    if (first)
                        first = false;
                    else
                        nameBuilder.append('.');

                    nameBuilder.append(metric.getSegments().get(i));
                    metrics.add(MetricName.get(nameBuilder.toString()));
                }
            } else if (first)
                metrics.add(Names.rootMetric());

            return new MetricHierarchy(metrics);
        }
    }

    public static class TestAggregationFilterSchemaConfiguration extends AggregationFilterSchemaConfiguration {
        private String pattern;

        public TestAggregationFilterSchemaConfiguration(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestAggregationFilterSchemaConfiguration))
                return false;

            TestAggregationFilterSchemaConfiguration configuration = (TestAggregationFilterSchemaConfiguration) o;
            return Objects.equals(pattern, configuration.pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pattern);
        }

        @Override
        public IAggregationFilter createFilter(IDatabaseContext context) {
            return new TestAggregationFilter(pattern);
        }
    }

    public static class TestAggregationFilter implements IAggregationFilter {
        private final NameFilter filter;

        public TestAggregationFilter(String pattern) {
            if (pattern != null)
                filter = new NameFilter(pattern);
            else
                filter = null;
        }

        @Override
        public boolean deny(IScopeName scope, IMetricName metric) {
            if (filter != null)
                return !filter.match("scope:" + scope.toString() + ",metric:" + metric.toString());
            else
                return false;
        }
    }

    public static class TestErrorAggregationStrategySchemaConfiguration extends ErrorAggregationStrategySchemaConfiguration {
        private String pattern;
        private String prefix;

        public TestErrorAggregationStrategySchemaConfiguration(String pattern, String prefix) {
            this.pattern = pattern;
            this.prefix = prefix;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestErrorAggregationStrategySchemaConfiguration))
                return false;

            TestErrorAggregationStrategySchemaConfiguration configuration = (TestErrorAggregationStrategySchemaConfiguration) o;
            return Objects.equals(pattern, configuration.pattern) && Objects.equals(prefix, configuration.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pattern, prefix);
        }

        @Override
        public IErrorAggregationStrategy createStrategy() {
            return new TestErrorAggregationStrategy(pattern, prefix);
        }
    }

    public static class TestErrorAggregationStrategy implements IErrorAggregationStrategy {
        private final NameFilter filter;
        private String prefix;

        public TestErrorAggregationStrategy(String pattern, String prefix) {
            if (pattern != null)
                filter = new NameFilter(pattern);
            else
                filter = null;
            this.prefix = prefix;
        }

        @Override
        public String getDerivedType(String errorType) {
            if (filter != null && !filter.match(errorType))
                return null;

            if (prefix != null)
                return prefix + errorType;
            else
                return errorType;
        }
    }

    public static class TestDocumentSchemaFactoryConfiguration extends DocumentSchemaFactoryConfiguration {
        public TestDocumentSchemaFactoryConfiguration() {
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDocumentSchemaFactoryConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public DocumentSchemaConfiguration createSchema() {
            return null;
        }
    }

    public static class TestObjectRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
        public TestObjectRepresentationSchemaConfiguration(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestObjectRepresentationSchemaConfiguration))
                return false;

            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public IMetricComputer createComputer(ComponentValueSchemaConfiguration schema,
                                              ComponentRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                                              int metricIndex) {
            return null;
        }
    }

    public static class TestBehaviorTypeLabelStrategySchemaConfiguration extends BehaviorTypeLabelStrategySchemaConfiguration {
        public TestBehaviorTypeLabelStrategySchemaConfiguration() {
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestBehaviorTypeLabelStrategySchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public IBehaviorTypeLabelStrategy createStrategy() {
            return null;
        }
    }

    public static class TestSchemaConfigurationLoader extends AbstractExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            JsonObject element = (JsonObject) object;
            if (type.equals("TestAggregationLogFilter")) {
                String pattern = element.get("pattern", null);
                return new TestAggregationLogFilterSchemaConfiguration(pattern);
            } else if (type.equals("TestMeasurementFilter")) {
                String pattern = element.get("pattern", null);
                return new TestMeasurementFilterSchemaConfiguration(pattern);
            } else if (type.equals("TestAggregationLogTransformer"))
                return new TestAggregationLogTransformerSchemaConfiguration();
            else if (type.equals("TestScopeAggregationStrategy")) {
                String prefix = element.get("prefix", null);
                return new TestScopeAggregationStrategySchemaConfiguration(prefix);
            } else if (type.equals("TestMetricAggregationStrategy")) {
                String prefix = element.get("prefix", null);
                return new TestMetricAggregationStrategySchemaConfiguration(prefix);
            } else if (type.equals("TestAggregationFilter")) {
                String pattern = element.get("pattern", null);
                return new TestAggregationFilterSchemaConfiguration(pattern);
            } else if (type.equals("TestErrorAggregationStrategy")) {
                String pattern = element.get("pattern", null);
                String prefix = element.get("prefix", null);
                return new TestErrorAggregationStrategySchemaConfiguration(pattern, prefix);
            } else if (type.equals("TestDocumentSchemaFactory"))
                return new TestDocumentSchemaFactoryConfiguration();
            else if (type.equals("TestObjectRepresentation"))
                return new TestObjectRepresentationSchemaConfiguration(name);
            else if (type.equals("TestBehaviorTypeLabelStrategy"))
                return new TestBehaviorTypeLabelStrategySchemaConfiguration();
            else
                return Assert.error();
        }
    }

    @Test
    public void testSchemaLoad() throws Throwable {
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        Set<ModuleSchemaConfiguration> modules = loader.loadModules("classpath:" + getResourcePath() + "/config1.conf");
        AggregationSchemaConfiguration schema = ((AggregationServiceSchemaConfiguration) modules.iterator().next().getSchema(
        ).getDomains().get(0).findDomainService(AggregationServiceSchemaConfiguration.NAME)).getAggregationSchema();

        MetricTypeSchemaConfiguration gauge = new GaugeSchemaConfiguration("metricType1", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("default", Arrays.asList(new StandardRepresentationSchemaConfiguration(true)))), false);

        NameSchemaConfiguration name1 = new NameSchemaConfiguration("name1", Collections.<MetricTypeSchemaConfiguration>emptyList(), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);
        NameSchemaConfiguration name2 = new NameSchemaConfiguration("name2", Arrays.asList(gauge), true,
                Arrays.asList(new TestScopeAggregationStrategySchemaConfiguration(null)),
                Arrays.asList(new TestMetricAggregationStrategySchemaConfiguration(null)), new TestAggregationFilterSchemaConfiguration(null),
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, true, true, null, null);
        StackNameSchemaConfiguration stackName1 = new StackNameSchemaConfiguration("stackName1", Collections.<MetricTypeSchemaConfiguration>emptyList(), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false);
        StackNameSchemaConfiguration stackName2 = new StackNameSchemaConfiguration("stackName2", Arrays.asList(gauge), true,
                Arrays.asList(new TestScopeAggregationStrategySchemaConfiguration(null)),
                Arrays.asList(new TestMetricAggregationStrategySchemaConfiguration(null)), new TestAggregationFilterSchemaConfiguration(null),
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, true, true);
        StackSchemaConfiguration backgroundStack1 = new StackSchemaConfiguration("backgroundStack1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, null, null, null, null, null);
        StackSchemaConfiguration backgroundStack2 = new StackSchemaConfiguration("backgroundStack2", Arrays.asList(gauge), true,
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");
        ExitPointSchemaConfiguration backgroundExitPoint1 = new ExitPointSchemaConfiguration("backgroundExitPoint1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, null, null, null, null, null);
        ExitPointSchemaConfiguration backgroundExitPoint2 = new ExitPointSchemaConfiguration("backgroundExitPoint2",
                Arrays.asList(gauge), true, new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");
        BackgroundRootSchemaConfiguration backgroundRoot1 = new BackgroundRootSchemaConfiguration("backgroundRoot1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, false, false, null);
        BackgroundRootSchemaConfiguration backgroundRoot2 = new BackgroundRootSchemaConfiguration("backgroundRoot2",
                Arrays.asList(gauge), true, Arrays.asList(new TestScopeAggregationStrategySchemaConfiguration(null)),
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1", true, true, true, "test");

        StackSchemaConfiguration transactionStack1 = new StackSchemaConfiguration("transactionStack1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, null, null, null, null, null);
        StackSchemaConfiguration transactionStack2 = new StackSchemaConfiguration("transactionStack2",
                Arrays.asList(gauge), true, new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");
        ExitPointSchemaConfiguration exitPoint1 = new ExitPointSchemaConfiguration("exitPoint1", Collections.<MetricTypeSchemaConfiguration>emptyList(), false,
                null, null, null, null, null);
        ExitPointSchemaConfiguration exitPoint2 = new ExitPointSchemaConfiguration("exitPoint2", Arrays.asList(gauge), true,
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");
        IntermediateExitPointSchemaConfiguration intermediateExitPoint1 = new IntermediateExitPointSchemaConfiguration("intermediateExitPoint1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, null, null, null, null, null);
        IntermediateExitPointSchemaConfiguration intermediateExitPoint2 = new IntermediateExitPointSchemaConfiguration("intermediateExitPoint2",
                Arrays.asList(gauge), true, new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");

        PrimaryEntryPointSchemaConfiguration primaryEntryPoint1 = new PrimaryEntryPointSchemaConfiguration("primaryEntryPoint1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                null, null, null, null, null, false, false, null, false, null, null, false, null);
        PrimaryEntryPointSchemaConfiguration primaryEntryPoint2 = new PrimaryEntryPointSchemaConfiguration("primaryEntryPoint2",
                Arrays.asList(gauge), true, Arrays.asList(new TestScopeAggregationStrategySchemaConfiguration(null)),
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1", true, true, "stackError1", true, null, null, true, "test");

        SecondaryEntryPointSchemaConfiguration secondaryEntryPoint1 = new SecondaryEntryPointSchemaConfiguration("secondaryEntryPoint1",
                Collections.<MetricTypeSchemaConfiguration>emptyList(), false, null, null, null, null, null);
        SecondaryEntryPointSchemaConfiguration secondaryEntryPoint2 = new SecondaryEntryPointSchemaConfiguration("secondaryEntryPoint2", Arrays.asList(gauge), true,
                new TestMeasurementFilterSchemaConfiguration(null), null, null, null, "stackName1");

        LogSchemaConfiguration log1 = new LogSchemaConfiguration("stackLog1",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")), null, Collections.<AggregationLogTransformerSchemaConfiguration>emptyList(),
                false, null);

        StackLogSchemaConfiguration stackLog1 = new StackLogSchemaConfiguration("stackLog1", Collections.singletonList(log1), true, null, null, null, null, false);

        LogSchemaConfiguration log2 = new LogSchemaConfiguration("stackLog2",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")), null, Collections.<AggregationLogTransformerSchemaConfiguration>emptyList(),
                false, null);

        StackLogSchemaConfiguration stackLog2 = new StackLogSchemaConfiguration("stackLog2", Collections.singletonList(log2),
                true, new TestMeasurementFilterSchemaConfiguration(null), null, null, null, true);

        LogSchemaConfiguration log3 = new LogSchemaConfiguration("stackError1",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")), null, Collections.<AggregationLogTransformerSchemaConfiguration>emptyList(),
                false, null);
        StackErrorLogSchemaConfiguration stackError1 = new StackErrorLogSchemaConfiguration("stackError1", log3,
                null, false, false, false, null, null, null, Collections.<ErrorAggregationStrategySchemaConfiguration>emptyList(), null, false);

        LogSchemaConfiguration log4 = new LogSchemaConfiguration("stackError2",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")), null, Collections.<AggregationLogTransformerSchemaConfiguration>emptyList(),
                false, null);
        StackErrorLogSchemaConfiguration stackError2 = new StackErrorLogSchemaConfiguration("stackError2", log4,
                new TestMeasurementFilterSchemaConfiguration(null), true, true, true,
                "name1", "name1", new NameFilter("Hello*"), Arrays.asList(new TestErrorAggregationStrategySchemaConfiguration(null, null)),
                new NameFilter("Failure*"), true);

        MetricTypeSchemaConfiguration metricType1 = new GaugeSchemaConfiguration("metricType1", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("default", Arrays.asList(new StandardRepresentationSchemaConfiguration(true)))), false);
        MetricTypeSchemaConfiguration metricType2 = new CounterSchemaConfiguration("metricType2", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("default", Arrays.asList(new StandardRepresentationSchemaConfiguration(true)))));
        MetricTypeSchemaConfiguration metricType3 = new StackCounterSchemaConfiguration("metricType3", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new StackRepresentationSchemaConfiguration("default", Arrays.asList(new StandardRepresentationSchemaConfiguration(true)))));
        MetricTypeSchemaConfiguration metricType4 = new InfoSchemaConfiguration("metricType4",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")));
        LogSchemaConfiguration metricType5 = new LogSchemaConfiguration("metricType5",
                Arrays.asList(new ObjectRepresentationSchemaConfiguration("default")), null, Collections.<AggregationLogTransformerSchemaConfiguration>emptyList(),
                false, null);

        NameSchemaConfiguration test1 = new NameSchemaConfiguration("test1", Arrays.asList(metricType1, metricType2, metricType3, metricType4), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);
        NameSchemaConfiguration test2 = new NameSchemaConfiguration("test2", Arrays.asList(metricType5), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);

        FieldRepresentationSchemaConfiguration rep1 = new StandardRepresentationSchemaConfiguration(false);
        FieldRepresentationSchemaConfiguration rep2 = new StatisticsRepresentationSchemaConfiguration(true);
        FieldRepresentationSchemaConfiguration rep3 = new UniformHistogramRepresentationSchemaConfiguration(0, 100, 10,
                true, true, true, true, Arrays.asList(5, 50, 95), true);
        FieldRepresentationSchemaConfiguration rep4 = new LogarithmicHistogramRepresentationSchemaConfiguration(0, 10,
                true, true, false, true, Arrays.asList(10, 25, 50, 75, 90), true);
        FieldRepresentationSchemaConfiguration rep5 = new CustomHistogramRepresentationSchemaConfiguration(Arrays.asList(0l, 1l, 2l),
                true, true, false, true, Arrays.asList(10, 25, 50, 75, 90), true);
        FieldRepresentationSchemaConfiguration rep6 = new InstanceRepresentationSchemaConfiguration(true);
        FieldRepresentationSchemaConfiguration rep7 = new RateRepresentationSchemaConfiguration("standard.sum", true);
        FieldRepresentationSchemaConfiguration rep8 = new RateRepresentationSchemaConfiguration("rate(count)", "standard.count", true);
        FieldRepresentationSchemaConfiguration rep9 = new PeriodRepresentationSchemaConfiguration("standard.sum", true);
        FieldRepresentationSchemaConfiguration rep10 = new PeriodRepresentationSchemaConfiguration("period(count)", "period", "standard.count", true);
        FieldRepresentationSchemaConfiguration rep11 = new PercentageRepresentationSchemaConfiguration("parentScope", "standard.sum", true);
        FieldRepresentationSchemaConfiguration rep12 = new PercentageRepresentationSchemaConfiguration("percentage(count)",
                "parentScope", "args", "nodeType1", "inherent.standard.count", "total.standard.count", true);
        FieldRepresentationSchemaConfiguration rep13 = new AnomalyRepresentationSchemaConfiguration("anomaly1", false, true);
        FieldRepresentationSchemaConfiguration rep14 = new ForecastRepresentationSchemaConfiguration("anomaly2", true, false, 20, true);

        FieldValueSchemaConfiguration field5 = new AnomalyValueSchemaConfiguration("anomaly1", "test1", "std.count", true,
                false, 0.11f, 0.02f, 0.3f, 100, 50, 200, 10, 3, true, new TestBehaviorTypeLabelStrategySchemaConfiguration());
        FieldValueSchemaConfiguration field6 = new ForecastValueSchemaConfiguration("anomaly2", "test1", "std.sum",
                true, 0.10f, 0.01f, 0.25f, 200, 150, 100, 5, 2, false, null);

        MetricTypeSchemaConfiguration metricType11 = new GaugeSchemaConfiguration("metricType1",
                Arrays.asList(new StandardValueSchemaConfiguration(), new StatisticsValueSchemaConfiguration(),
                        new UniformHistogramValueSchemaConfiguration(0, 10, 10), new InstanceValueSchemaConfiguration(10, true), field5, field6),
                Arrays.asList(new NameRepresentationSchemaConfiguration("test1", Arrays.asList(rep1, rep2, rep3,
                        rep6, rep7, rep8, rep9, rep10, rep11, rep12, rep13, rep14)),
                        new NameRepresentationSchemaConfiguration("test2", Arrays.asList(rep1, rep4)),
                        new NameRepresentationSchemaConfiguration("test3", Arrays.asList(rep5))), false);

        MetricTypeSchemaConfiguration metricType21 = new AnomalyIndexSchemaConfiguration("metricType2", "test1", 2,
                Arrays.asList(new AnomalyIndexRepresentationSchemaConfiguration("test1")));
        MetricTypeSchemaConfiguration metricType31 = new ExpressionIndexSchemaConfiguration("metricType3", true, "test1",
                Arrays.asList(new ExpressionIndexRepresentationSchemaConfiguration("test1", "test1"),
                        new ExpressionIndexRepresentationSchemaConfiguration("test2", "test2")));

        NameSchemaConfiguration test3 = new NameSchemaConfiguration("test3", Arrays.asList(metricType11, metricType21, metricType31), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);

        MetricTypeSchemaConfiguration metricType6 = new LogSchemaConfiguration("metricType6",
                Arrays.asList(new TestObjectRepresentationSchemaConfiguration("test1")), new TestAggregationLogFilterSchemaConfiguration(null),
                Arrays.asList(new TestAggregationLogTransformerSchemaConfiguration()), true, new TestDocumentSchemaFactoryConfiguration());

        NameSchemaConfiguration test4 = new NameSchemaConfiguration("test4", Arrays.asList(metricType6), true,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);

        PeriodTypeSchemaConfiguration period1 = new PeriodTypeSchemaConfiguration("p1", com.exametrika.common.utils.Collections.<AggregationComponentTypeSchemaConfiguration>asSet(
                name1, name2, stackName1, stackName2, backgroundStack1, backgroundStack2, backgroundExitPoint1, backgroundExitPoint2,
                backgroundRoot1, backgroundRoot2, transactionStack1, transactionStack2, exitPoint1, exitPoint2, intermediateExitPoint1, intermediateExitPoint2,
                primaryEntryPoint1, primaryEntryPoint2, secondaryEntryPoint1, secondaryEntryPoint2, stackLog1, stackLog2, stackError1, stackError2),
                new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 10), 1, true, null);
        PeriodTypeSchemaConfiguration period2 = new PeriodTypeSchemaConfiguration("p2", com.exametrika.common.utils.Collections.<AggregationComponentTypeSchemaConfiguration>asSet(
                test1, test2, test3, test4),
                new StandardSchedulePeriodSchemaConfiguration(UnitType.MINUTE, Kind.ABSOLUTE, 10), 100, false, null);
        AggregationSchemaConfiguration ethalonSchema = new AggregationSchemaConfiguration(Arrays.asList(period1, period2), CombineType.STACK, 2);

        assertThat(schema, is(ethalonSchema));
    }

    private static String getResourcePath() {
        String className = AggregationSchemaLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
