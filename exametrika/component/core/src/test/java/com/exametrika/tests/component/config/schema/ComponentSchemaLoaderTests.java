/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.component.config.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.GaugeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.MetricTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.AggregationServiceSchemaConfiguration;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.component.config.model.AgentComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration.Type;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.DisableMaintenanceModeActionSchemaConfiguration;
import com.exametrika.api.component.config.model.EnableMaintenanceModeActionSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComplexAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComponentJobOperationSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthSchemaConfiguration;
import com.exametrika.api.component.config.model.HostComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HostDeletionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.HostDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.LogActionSchemaConfiguration;
import com.exametrika.api.component.config.model.MailAlertChannelSchemaConfiguration;
import com.exametrika.api.component.config.model.NodeComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.NodeDeletionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.NodeDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.PatternGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.PredefinedGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.TagIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentServiceSchemaConfiguration;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaBuilder;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.api.exadb.jobs.config.schema.JobServiceSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.Json;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.component.config.schema.ComponentSchemaLoader;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.config.model.MetricAggregationStrategySchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.ScopeAggregationStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.tests.component.ComponentTests.TestComponentBindingStrategySchemaConfiguration;
import com.exametrika.tests.component.ComponentTests.TestLocalActionSchemaConfiguration;
import com.exametrika.tests.component.ComponentTests.TestRemoteActionSchemaConfiguration;
import com.exametrika.tests.component.ComponentTests.TestRuleSchemaConfiguration;

/**
 * The {@link ComponentSchemaLoaderTests} are tests for {@link ComponentSchemaLoader}.
 *
 * @author Medvedev-A
 */
@Ignore
public class ComponentSchemaLoaderTests {
    public static class TestConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.component", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/extension.dbschema", false));
            parameters.typeLoaders.put("TestLocalAction", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestRemoteAction", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestRule", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestComponentBindingStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestGroupAvailabilityCondition", new TestSchemaConfigurationLoader());

            return parameters;
        }
    }

    public static class TestSchemaConfigurationLoader extends AbstractExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            if (type.equals("TestLocalAction"))
                return new TestLocalActionSchemaConfiguration(name);
            if (type.equals("TestRemoteAction"))
                return new TestRemoteActionSchemaConfiguration(name);
            if (type.equals("TestRule"))
                return new TestRuleSchemaConfiguration(name);
            if (type.equals("TestComponentBindingStrategy"))
                return new TestComponentBindingStrategySchemaConfiguration();
            if (type.equals("TestGroupAvailabilityCondition"))
                return new TestGroupAvailabilityConditionSchemaConfiguration();
            else
                return Assert.error();
        }
    }

    @Test
    public void testSchemaLoad() throws Throwable {
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        Map<String, ModuleSchemaConfiguration> modules = getMap(loader.loadModules("classpath:" + getResourcePath() + "/config1.conf"));
        ModularDatabaseSchemaConfiguration configuration = new ModularDatabaseSchemaConfiguration("test", "test", null,
                new HashSet<ModuleSchemaConfiguration>(modules.values()), null, null);
        AggregationSchemaConfiguration aggregationSchema = ((AggregationServiceSchemaConfiguration) configuration.getCombinedSchema()
                .findDomain("aggregation").findDomainService(AggregationServiceSchemaConfiguration.NAME)).getAggregationSchema();
        ComponentModelSchemaConfiguration componentModel = ((ComponentServiceSchemaConfiguration) configuration.getCombinedSchema()
                .findDomain("component").findDomainService(ComponentServiceSchemaConfiguration.NAME)).getComponentModel();
        Map<String, JobSchemaConfiguration> predefinedJobs = ((JobServiceSchemaConfiguration) configuration.getCombinedSchema()
                .findDomain("system").findDomainService(JobServiceSchemaConfiguration.NAME)).getPredefinedJobs();

        Map<String, JobSchemaConfiguration> predefinedJobs1 = new HashMap<String, JobSchemaConfiguration>();
        JobSchemaConfiguration job1 = new JobSchemaBuilder()
                .name("job1")
                .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('job1')"))
                .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(10:30)"))
                .oneTime().toJob();
        predefinedJobs1.put("job1", job1);
        predefinedJobs1.put("job2", new JobSchemaBuilder()
                .name("job2")
                .operation(new ExpressionComponentJobOperationSchemaConfiguration("component.log('job2')"))
                .schedule(new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(10:30)"))
                .recurrent().period(new StandardSchedulePeriodSchemaConfiguration(UnitType.MINUTE, Kind.ABSOLUTE, 10)).toJob());
        assertThat(predefinedJobs, is(predefinedJobs1));

        LogActionSchemaConfiguration action1 = new LogActionSchemaConfiguration("action1");
        EnableMaintenanceModeActionSchemaConfiguration action2 = new EnableMaintenanceModeActionSchemaConfiguration("action2");
        DisableMaintenanceModeActionSchemaConfiguration action3 = new DisableMaintenanceModeActionSchemaConfiguration("action3");
        TestRuleSchemaConfiguration rule1 = new TestRuleSchemaConfiguration("rule1");
        AlertSchemaConfiguration alert1 = new HealthAlertSchemaConfiguration("alert1", null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "onTemplate", "offTemplate", "statusTemplate",
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(10:30)"),
                        new StandardSchedulePeriodSchemaConfiguration(UnitType.MINUTE, Kind.ABSOLUTE, 10),
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1"), new AlertRecipientSchemaConfiguration(Type.ROLE, "recipient2"),
                                new AlertRecipientSchemaConfiguration(Type.USER_GROUP, "recipient3"), new AlertRecipientSchemaConfiguration(Type.USER, "recipient4")),
                        "onSubject", "offSubject", "statusSubject", false, "senderName", "senderAddress")),
                Arrays.asList("tag1", "tag2"), false, IHealthComponentVersion.State.HEALTH_ERROR);
        AlertSchemaConfiguration alert2 = new ExpressionSimpleAlertSchemaConfiguration("alert2", null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "onTemplate", null, null, null, null,
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", null, null, true, null, null)), null, true, "onCondition", "offCondition");
        AlertSchemaConfiguration alert3 = new ExpressionComplexAlertSchemaConfiguration("alert3", null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "onTemplate", null, null, null, null,
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", null, null, true, null, null)), null, true, "onCondition", "offCondition");
        AlertSchemaConfiguration alert4 = new TagIncidentGroupSchemaConfiguration("alert4", null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "onTemplate", null, null, null, null,
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", null, null, true, null, null)), null, true, "pattern");
        AlertSchemaConfiguration alert5 = new ExpressionIncidentGroupSchemaConfiguration("alert5", null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "onTemplate", null, null, null, null,
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", null, null, true, null, null)), null, true, "expression");
        ComponentSchemaConfiguration component1 = new ComponentSchemaConfiguration("component1", com.exametrika.common.utils.Collections.asSet(
                action1, action2, action3), com.exametrika.common.utils.Collections.asSet(rule1), Collections.<SelectorSchemaConfiguration>emptySet(),
                com.exametrika.common.utils.Collections.asSet(alert1, alert2, alert3, alert4, alert5),
                Arrays.asList(new PatternGroupDiscoveryStrategySchemaConfiguration("TestGroup", "groups.*", "groups")),
                Collections.<JobSchemaConfiguration>emptySet());

        String profilerConfiguration = new ConfigurationLoader().createInlineConfiguration("classpath:" + getResourcePath() + "/profiler.conf", null);
        ComponentSchemaConfiguration component2 = new ComponentSchemaConfiguration("component2",
                Collections.<ActionSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<SelectorSchemaConfiguration>emptySet(), Collections.<AlertSchemaConfiguration>emptySet(),
                Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(), Collections.<JobSchemaConfiguration>emptySet());
        AgentComponentSchemaConfiguration component3 = new AgentComponentSchemaConfiguration("component3",
                Collections.<ActionSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<SelectorSchemaConfiguration>emptySet(), Collections.<AlertSchemaConfiguration>emptySet(),
                Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(), Collections.<JobSchemaConfiguration>emptySet(),
                "test1", profilerConfiguration);
        HostComponentSchemaConfiguration component4 = new HostComponentSchemaConfiguration("component4",
                Collections.<ActionSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<SelectorSchemaConfiguration>emptySet(), Collections.<AlertSchemaConfiguration>emptySet(),
                Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(), Collections.<JobSchemaConfiguration>emptySet(),
                "test1", profilerConfiguration);
        NodeComponentSchemaConfiguration component5 = new NodeComponentSchemaConfiguration("component5",
                Collections.<ActionSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<SelectorSchemaConfiguration>emptySet(), Collections.<AlertSchemaConfiguration>emptySet(),
                Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(), Collections.<JobSchemaConfiguration>emptySet(),
                "test1", profilerConfiguration);
        TransactionComponentSchemaConfiguration component6 = new TransactionComponentSchemaConfiguration("component6",
                Collections.<ActionSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<SelectorSchemaConfiguration>emptySet(), Collections.<AlertSchemaConfiguration>emptySet(),
                Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(), Collections.<JobSchemaConfiguration>emptySet(), "healthType");
        GroupComponentSchemaConfiguration component7 = new GroupComponentSchemaConfiguration("component7");
        GroupComponentSchemaConfiguration component8 = new GroupComponentSchemaConfiguration("component8",
                Collections.<ActionSchemaConfiguration>emptySet(),
                Collections.<RuleSchemaConfiguration>emptySet(), Collections.<SelectorSchemaConfiguration>emptySet(),
                Collections.<AlertSchemaConfiguration>emptySet(), Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(),
                Collections.<JobSchemaConfiguration>emptySet(), false, false, com.exametrika.common.utils.Collections.asSet("type1", "type2"),
                com.exametrika.common.utils.Collections.asSet("type3", "type4"), true,
                new TestGroupAvailabilityConditionSchemaConfiguration(), "healthType");

        Map<String, PredefinedGroupSchemaConfiguration> subGroups = new HashMap<String, PredefinedGroupSchemaConfiguration>();
        subGroups.put("group11", new PredefinedGroupSchemaConfiguration("component7", "group11",
                Collections.<String, PredefinedGroupSchemaConfiguration>emptyMap()));
        subGroups.put("group12", new PredefinedGroupSchemaConfiguration("component7", "group12",
                Collections.<String, PredefinedGroupSchemaConfiguration>emptyMap()));

        Map<String, PredefinedGroupSchemaConfiguration> groups = new HashMap<String, PredefinedGroupSchemaConfiguration>();
        groups.put("group1", new PredefinedGroupSchemaConfiguration("component8", "group1",
                subGroups, Json.object().put("key1", "value1").put("key2", "value2").toObject(),
                Json.object().put("key3", "value3").put("key4", "value4").toObject(), com.exametrika.common.utils.Collections.asSet("tag1", "tag2"),
                com.exametrika.common.utils.Collections.asSet(new TestRuleSchemaConfiguration("rule1")),
                com.exametrika.common.utils.Collections.asSet(alert2),
                com.exametrika.common.utils.Collections.asSet(new TestRuleSchemaConfiguration("rule2")),
                com.exametrika.common.utils.Collections.asSet(alert2), Collections.singleton(job1)));
        groups.put("group2", new PredefinedGroupSchemaConfiguration("component8", "group2",
                Collections.<String, PredefinedGroupSchemaConfiguration>emptyMap()));

        ComponentModelSchemaConfiguration ethalonModel = new ComponentModelSchemaConfiguration(com.exametrika.common.utils.Collections.asSet(
                component1, component2, component3, component4, component5, component6, component7, component8),
                new HealthSchemaConfiguration("p2", new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0),
                        new CounterConfiguration(true, false, 0),
                        new CounterConfiguration(true, false, 0), new CounterConfiguration(true, false, 0)), groups, null, 2);

        assertThat(componentModel, is(ethalonModel));

        MetricTypeSchemaConfiguration metricType1 = new GaugeSchemaConfiguration("metricType1", Arrays.asList(new StandardValueSchemaConfiguration()),
                Arrays.asList(new NameRepresentationSchemaConfiguration("default", Arrays.asList(new StandardRepresentationSchemaConfiguration(true)))), false);
        NameSchemaConfiguration test21 = new NameSchemaConfiguration("test2", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);
        NameSchemaConfiguration test2 = new NameSchemaConfiguration("test2", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, Arrays.asList(
                new NodeDiscoveryStrategySchemaConfiguration("Node"), new HostDiscoveryStrategySchemaConfiguration("Host"),
                new TransactionDiscoveryStrategySchemaConfiguration("Transaction")), new NodeDeletionStrategySchemaConfiguration());
        NameSchemaConfiguration test3 = new NameSchemaConfiguration("test3", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null,
                new HostDeletionStrategySchemaConfiguration());
        NameSchemaConfiguration test4 = new NameSchemaConfiguration("test4", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);
        NameSchemaConfiguration test5 = new NameSchemaConfiguration("test5", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);
        NameSchemaConfiguration test6 = new NameSchemaConfiguration("test6", Arrays.asList(metricType1), false,
                Collections.<ScopeAggregationStrategySchemaConfiguration>emptyList(),
                Collections.<MetricAggregationStrategySchemaConfiguration>emptyList(), null, null, null, null, null, false, false, null, null);

        PeriodTypeSchemaConfiguration period1 = new PeriodTypeSchemaConfiguration("p1", com.exametrika.common.utils.Collections.<AggregationComponentTypeSchemaConfiguration>asSet(
                test21),
                new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 10), 1, true, null);
        PeriodTypeSchemaConfiguration period2 = new PeriodTypeSchemaConfiguration("p2", com.exametrika.common.utils.Collections.<AggregationComponentTypeSchemaConfiguration>asSet(
                test2, test3, test4, test5, test6),
                new StandardSchedulePeriodSchemaConfiguration(UnitType.MINUTE, Kind.ABSOLUTE, 10), 100, false, "test");
        AggregationSchemaConfiguration ethalonSchema = new AggregationSchemaConfiguration(Arrays.asList(period1, period2), CombineType.STACK, 2);

        assertThat(aggregationSchema, is(ethalonSchema));
    }

    private Map<String, ModuleSchemaConfiguration> getMap(Set<ModuleSchemaConfiguration> modules) {
        Map<String, ModuleSchemaConfiguration> map = new HashMap<String, ModuleSchemaConfiguration>();
        for (ModuleSchemaConfiguration module : modules)
            map.put(module.getName(), module);

        return map;
    }

    public static class TestGroupAvailabilityConditionSchemaConfiguration extends GroupAvailabilityConditionSchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestGroupAvailabilityConditionSchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public ICondition<IGroupComponent> createCondition(IDatabaseContext context) {
            return null;
        }
    }

    private static String getResourcePath() {
        String className = ComponentSchemaLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
