/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.component;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.component.config.MailAlertChannelConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComplexAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleRuleSchemaConfiguration;
import com.exametrika.api.component.config.model.MailAlertChannelSchemaConfiguration;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.IIncidentGroup;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.TestMode;
import com.exametrika.common.utils.Threads;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.AggregationService;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.nodes.HostComponentNode;
import com.exametrika.impl.component.nodes.IncidentGroupNode;
import com.exametrika.impl.component.nodes.IncidentNode;
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.jobs.schedule.ScheduleExpressionParser;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.tests.component.ComponentTests.TestAgentActionExecutor;
import com.exametrika.tests.component.ComponentTests.TestAgentFailureDetector;


/**
 * The {@link AlertTests} are tests for alerts of component model.
 *
 * @author Medvedev-A
 */
public class AlertTests {
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfiguration configuration;
    private Database database;
    private File tempDir;
    private TestAgentActionExecutor actionExecutor = new TestAgentActionExecutor();
    private TestAgentFailureDetector failureDetector = new TestAgentFailureDetector();

    @Before
    public void setUp() {
        TestMode.setTest(true);
        Times.setTest(0);
        tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setTimerPeriod(10);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        builder.addDomainService(new AlertServiceConfiguration(10, java.util.Collections.singletonMap("mail",
                new MailAlertChannelConfiguration("mail", "host", 123, "userName", "password", true, "senderName", "senderAddress", 1000))));
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put(IAgentActionExecutor.NAME, actionExecutor);
        parameters.parameters.put(IAgentFailureDetector.NAME, failureDetector);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
        Numbers.clearTest();
        Times.clearTest();
    }

    @Test
    public void testAlerts() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("alerts1.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "alerts1.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));

                ComponentRootNode root = space.getRootNode();
                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));
                root.getRootGroup().addChild(group1);
                IGroupComponent group2 = space.createNode(nameManager.addName(Names.getScope("group2")).getId(), schema.findNode("AggregationGroup"));
                root.getRootGroup().addChild(group2);

                group1.addComponent(host1);
                group2.addComponent(host1);

                AlertSchemaConfiguration alert1 = createSimpleAlert("alert1", "true");
                ExpressionSimpleRuleSchemaConfiguration rule2 = new ExpressionSimpleRuleSchemaConfiguration("rule2",
                        "fact('rule2', true)", true);
                ExpressionSimpleRuleSchemaConfiguration rule3 = new ExpressionSimpleRuleSchemaConfiguration("rule3",
                        "fact('rule3', true)", true);
                AlertSchemaConfiguration alert4 = createComplexAlert("alert4", "(hasFact('rule2') && hasFact('rule3'))");

                AlertSchemaConfiguration check1 = createHealthAlert("check1", "true");

                host1.addAlert(alert1);
                host1.addRule(rule2);
                host1.addRule(rule3);
                host1.addAlert(alert4);
                host1.addAlert(check1);

                ExpressionSimpleRuleSchemaConfiguration rule5 = new ExpressionSimpleRuleSchemaConfiguration("rule5",
                        "fact('rule5', true)", true);
                AlertSchemaConfiguration rule6 = createComplexAlert("alert6", "(hasFact('rule5'))");
                AlertSchemaConfiguration group1TestAlert = createSimpleAlert("test", "true");
                AlertSchemaConfiguration check2 = createHealthAlert("check2", "true");
                group1.addGroupRule(rule5);
                group1.addGroupAlert(rule6);
                group1.addGroupAlert(group1TestAlert);
                group1.addGroupAlert(check2);

                AlertSchemaConfiguration group2TestAlert = createSimpleAlert("test", "true");
                group2.addGroupAlert(group2TestAlert);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(3000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));

                group1.addGroupAlert(createSimpleAlert("alert9", "true"));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(4000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));

                host1.removeAlert("alert1");
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(5000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                IGroupComponent group1 = space.findNode(nameManager.addName(Names.getScope("group1")).getId(), schema.findNode("AggregationGroup"));

                group1.removeComponent(host1);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(6000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("AggregationGroup"));

                group2.delete();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(7000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.enableMaintenanceMode("test");
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(8000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.disableMaintenanceMode();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(9000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                host1.addAlert(createSimpleAlert("alert10", "(hasMetric('metricType1') && metric('metricType1.std.count') >= 20)"));
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(10000);
                space.addPeriod();
            }
        });

        checkDump("alerts", "alerts", Arrays.asList("component-component-39.json", "aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json"));
    }

    @Test
    public void testHealthAlert() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("alerts2.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "alerts2.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        Times.setTest(3000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");

        Threads.sleep(300);

        Times.setTest(4000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(5000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        Times.setTest(6000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.listener.onAgentFailed("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(7000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        Times.setTest(8000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(8500);
                space.addPeriod();
            }
        });

        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                IObjectSpaceSchema componentSchema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                ComponentNodeSchema nodeSchema = componentSchema.findNode("Host");
                IObjectSpace componentSpace = componentSchema.getSpace();
                INodeIndex<Long, IHostComponent> index = componentSpace.getIndex(nodeSchema.getIndexField());
                IHostComponent component = index.find(nameManager.findByName(Names.getScope("hosts.host1")).getId());
                Times.setTest(9000);
                component.enableMaintenanceMode("Test maintenance.");
                component.enableMaintenanceMode(null);
            }
        });

        Threads.sleep(300);

        Times.setTest(10000);
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");
        failureDetector.active = false;
        failureDetector.listener.onAgentFailed("hosts.host1");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema componentSchema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                ComponentNodeSchema nodeSchema = componentSchema.findNode("Host");
                IObjectSpace componentSpace = componentSchema.getSpace();
                INodeIndex<Long, IHostComponent> index = componentSpace.getIndex(nodeSchema.getIndexField());
                IHostComponent component = index.find(nameManager.findByName(Names.getScope("hosts.host1")).getId());
                Times.setTest(11000);
                component.disableMaintenanceMode();
                component.disableMaintenanceMode();
            }
        });

        Threads.sleep(300);

        Times.setTest(12000);
        failureDetector.active = true;
        failureDetector.listener.onAgentActivated("hosts.host1");

        Threads.sleep(300);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(13000);
                space.addPeriod();
            }
        });

        Threads.sleep(300);

        checkDump("alerts2", "alerts2", Arrays.asList("component-component-39.json", "aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json"));
    }

    @Test
    public void testSimpleAlert() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);

        createDatabase("alerts3.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "alerts3.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(14000);
                space.addPeriod();
            }
        });

        checkDump("alerts3", "alerts3", Arrays.asList("component-component-39.json", "aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json"));
    }

    @Test
    public void testIncidentGroups() {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("alerts4.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "alerts4.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(2000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.findByName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                assertThat(host1.findIncident("simple1"), nullValue());
                assertThat(host1.findIncident("simple2"), nullValue());

                IGroupComponent group1 = space.createNode(nameManager.addName(Names.getScope("group1")).getId(),
                        schema.findNode("AggregationGroup"));
                group1.addComponent(host1);
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(3000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                IncidentNode alert1 = (IncidentNode) host1.findIncident("simple1");
                assertThat(alert1.getName(), is("simple1"));
                assertThat(alert1.getCreationTime(), is(3000l));
                assertThat(alert1.getComponent() == host1, is(true));
                assertThat(alert1.getAlert() == host1.findAlertSchema("simple1"), is(true));

                IncidentNode alert2 = (IncidentNode) host1.findIncident("simple2");
                assertThat(alert2.getName(), is("simple2"));
                assertThat(alert2.getCreationTime(), is(3000l));
                assertThat(alert2.getComponent() == host1, is(true));
                assertThat(alert2.getAlert() == host1.findAlertSchema("simple2"), is(true));

                GroupComponentNode group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(),
                        schema.findNode("AggregationGroup"));

                IncidentGroupNode alert3 = (IncidentGroupNode) group1.findIncident("tagAlert");
                assertThat(alert3.getName(), is("tagAlert"));
                assertThat(alert3.getCreationTime(), is(3000l));
                assertThat(alert3.getComponent() == group1, is(true));
                assertThat(alert3.getAlert() == group1.findAlertSchema("tagAlert"), is(true));
                assertThat(Collections.toList(alert3.getChildren().iterator()), is((List) Arrays.asList(alert1, alert2)));

                IncidentGroupNode alert4 = (IncidentGroupNode) group1.findIncident("expressionAlert");
                assertThat(alert4.getName(), is("expressionAlert"));
                assertThat(alert4.getCreationTime(), is(3000l));
                assertThat(alert4.getComponent() == group1, is(true));
                assertThat(alert4.getAlert() == group1.findAlertSchema("expressionAlert"), is(true));
                assertThat(Collections.toList(alert4.getChildren().iterator()), is((List) Arrays.asList(alert1)));
            }
        });

        Times.setTest(9000);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(10000);
                space.addPeriod();
            }
        });


        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                assertThat(host1.findIncident("simple1"), nullValue());
                assertThat(host1.findIncident("simple2"), nullValue());

                GroupComponentNode group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(),
                        schema.findNode("AggregationGroup"));

                assertThat(group1.findIncident("tagAlert"), nullValue());
                assertThat(group1.findIncident("expressionAlert"), nullValue());
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                aggregationService.aggregate(measurements);
                aggregationService.aggregate(measurements);

                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(12000);
                space.addPeriod();
            }
        });

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);

                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                HostComponentNode host1 = space.findNode(nameManager.addName(Names.getScope("hosts.host1")).getId(), schema.findNode("Host"));
                IIncident alert1 = host1.findIncident("simple1");
                IIncident alert2 = host1.findIncident("simple2");

                GroupComponentNode group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(),
                        schema.findNode("AggregationGroup"));

                IIncidentGroup alert3 = (IIncidentGroup) group1.findIncident("tagAlert");
                assertThat(Collections.toList(alert3.getChildren().iterator()), is(Arrays.asList(alert1, alert2)));

                IIncidentGroup alert4 = (IIncidentGroup) group1.findIncident("expressionAlert");
                assertThat(Collections.toList(alert4.getChildren().iterator()), is(Arrays.asList(alert1)));

                alert3.delete(true);
                alert4.delete(true);

                assertThat(host1.findIncident("simple1"), nullValue());
                assertThat(host1.findIncident("simple2"), nullValue());
                assertThat(group1.findIncident("tagAlert"), nullValue());
                assertThat(group1.findIncident("expressionAlert"), nullValue());
            }
        });

        checkDump("alerts4", "alerts4", Arrays.asList("component-component-39.json", "aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json"));
    }

    private AlertSchemaConfiguration createSimpleAlert(String name, String condition) {
        return new ExpressionSimpleAlertSchemaConfiguration(name, null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "Alert <%name%> is on.", "Alert <%name%> is on.", null,
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"),
                        new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1),
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", "offSubject", "statusSubject", false, "senderName", "senderAddress")), Arrays.asList("tag1", "tag2"), true,
                condition, null);
    }

    private AlertSchemaConfiguration createComplexAlert(String name, String condition) {
        return new ExpressionComplexAlertSchemaConfiguration(name, null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "Alert <%name%> is on.", "Alert <%name%> is on.", null,
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"),
                        new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1),
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", "offSubject", "statusSubject", false, "senderName", "senderAddress")), Arrays.asList("tag1", "tag2"), true,
                condition, null);
    }

    private AlertSchemaConfiguration createHealthAlert(String name, String condition) {
        return new HealthAlertSchemaConfiguration(name, null, Arrays.asList(
                new MailAlertChannelSchemaConfiguration("mail", "Alert <%name%> is on.", "Alert <%name%> is on.", null,
                        new ScheduleExpressionParser("dd.MM.yyyy", "HH:mm").parse("time(00:00..23:59)"),
                        new StandardSchedulePeriodSchemaConfiguration(UnitType.SECOND, Kind.ABSOLUTE, 1),
                        Arrays.asList(new AlertRecipientSchemaConfiguration("recipient1", "address1")),
                        "onSubject", "offSubject", "statusSubject", false, "senderName", "senderAddress")), Arrays.asList("tag1", "tag2"), true,
                IHealthComponentVersion.State.HEALTH_ERROR);
    }

    private void createDatabase(String schemaConfigName) {
        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final String schemaResourcePath = resourcePath + schemaConfigName;
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModules(schemaResourcePath, null);
            }
        });
    }

    private void checkDump(String dumpDirectoryPath, String ethalonDirectoryPath, List<String> resultFileNames) {
        database.getOperations().dump(new File(tempDir, dumpDirectoryPath).getPath(), new DumpContext(IDumpContext.DUMP_ORPHANED,
                Json.object().put("dumpLogs", true).toObject()), null);

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        boolean failed = false;
        for (String resultFileName : resultFileNames)
            failed = !compare(tempDir, dumpDirectoryPath, ethalonDirectoryPath, resourcePath, resultFileName, resultFileName) || failed;

        if (failed)
            assertTrue(false);
    }

    private boolean compare(File baseDir, String dumpDirectoryPath, String ethalonDirectoryPath, final String resourcePath,
                            String resultFileName, String ethalonFileName) {
        JsonObject result = JsonSerializers.load(new File(baseDir, dumpDirectoryPath + File.separator + resultFileName).getPath(), false);
        JsonObject ethalon = JsonSerializers.load(resourcePath + ethalonDirectoryPath + "/" + ethalonFileName, false);
        if (!result.equals(ethalon)) {
            System.out.println("result: " + resultFileName);
            System.out.println(new JsonDiff(true).diff(result, ethalon));
            return false;
        }

        return true;
    }
}
