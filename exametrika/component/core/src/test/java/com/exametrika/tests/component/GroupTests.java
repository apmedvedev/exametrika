/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.component;

import static org.hamcrest.CoreMatchers.is;
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
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
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
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.tests.component.ComponentTests.TestAgentActionExecutor;
import com.exametrika.tests.component.ComponentTests.TestAgentFailureDetector;


/**
 * The {@link GroupTests} are tests for groups of component model.
 *
 * @author Medvedev-A
 */
public class GroupTests {
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
    public void testGroups() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("groups1.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent root = ((ComponentRootNode) space.getRootNode()).getRootGroup();
                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("Group1"));
                assertThat(group1.getTags(), is(Arrays.asList("tag1")));
                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("Group1"));
                assertThat(group2.getTags(), is(Arrays.asList("tag2")));
                IGroupComponent group11 = space.findNode(nameManager.findByName(Names.getScope("group1.group11")).getId(), schema.findNode("Group2"));
                assertThat(group11.getTags(), is(Arrays.asList("tag11")));
                IGroupComponent group12 = space.findNode(nameManager.findByName(Names.getScope("group1.group12")).getId(), schema.findNode("Group2"));
                assertThat(group12.getTags(), is(Arrays.asList("tag12")));

                assertThat(Collections.toList(((IGroupComponentVersion) root.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group1, group2)));
                assertThat(Collections.toList(((IGroupComponentVersion) group1.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group11, group12)));
            }
        });

        Times.setTest(2000);
        createDatabase("groups2.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent root = ((ComponentRootNode) space.getRootNode()).getRootGroup();
                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("Group1"));
                assertThat(group1.getTags(), is(Arrays.asList("tag1-")));
                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("Group1"));
                IGroupComponent group3 = space.findNode(nameManager.findByName(Names.getScope("group3")).getId(), schema.findNode("Group1"));
                assertThat(group3.getTags(), is(Arrays.asList("tag3-")));
                IGroupComponent group11 = space.findNode(nameManager.findByName(Names.getScope("group1.group11")).getId(), schema.findNode("Group2"));
                assertThat(group11.getTags(), is(Arrays.asList("tag11-")));
                IGroupComponent group12 = space.findNode(nameManager.findByName(Names.getScope("group1.group12")).getId(), schema.findNode("Group2"));
                IGroupComponent group13 = space.findNode(nameManager.findByName(Names.getScope("group1.group13")).getId(), schema.findNode("Group2"));
                assertThat(group13.getTags(), is(Arrays.asList("tag13-")));

                assertThat(Collections.toList(((IGroupComponentVersion) root.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group1, group3)));
                assertThat(Collections.toList(((IGroupComponentVersion) group1.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group11, group13)));
                assertThat(group2.getCurrentVersion().isDeleted(), is(true));
                assertThat(group12.getCurrentVersion().isDeleted(), is(true));
            }
        });

        Times.setTest(3000);
        createDatabase("groups3.conf");

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodNameManager nameManager = transaction.findExtension(IPeriodNameManager.NAME);
                IObjectSpaceSchema schema = transaction.getCurrentSchema().findSchemaById("space:component.component");
                IObjectSpace space = schema.getSpace();

                IGroupComponent root = ((ComponentRootNode) space.getRootNode()).getRootGroup();
                IGroupComponent group1 = space.findNode(nameManager.findByName(Names.getScope("group1")).getId(), schema.findNode("Group1"));
                assertThat(group1.getTags(), is(Arrays.asList("tag1")));
                IGroupComponent group2 = space.findNode(nameManager.findByName(Names.getScope("group2")).getId(), schema.findNode("Group1"));
                assertThat(group2.getTags(), is(Arrays.asList("tag2")));
                IGroupComponent group3 = space.findNode(nameManager.findByName(Names.getScope("group3")).getId(), schema.findNode("Group1"));
                IGroupComponent group11 = space.findNode(nameManager.findByName(Names.getScope("group1.group11")).getId(), schema.findNode("Group2"));
                assertThat(group11.getTags(), is(Arrays.asList("tag11")));
                IGroupComponent group12 = space.findNode(nameManager.findByName(Names.getScope("group1.group12")).getId(), schema.findNode("Group2"));
                assertThat(group12.getTags(), is(Arrays.asList("tag12")));
                IGroupComponent group13 = space.findNode(nameManager.findByName(Names.getScope("group1.group13")).getId(), schema.findNode("Group2"));

                assertThat(Collections.toList(((IGroupComponentVersion) root.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group1, group2)));
                assertThat(Collections.toList(((IGroupComponentVersion) group1.getCurrentVersion()).getChildren().iterator()),
                        is(Arrays.asList(group11, group12)));

                assertThat(group2.getCurrentVersion().isDeleted(), is(false));
                assertThat(group12.getCurrentVersion().isDeleted(), is(false));

                assertThat(group3.getCurrentVersion().isDeleted(), is(true));
                assertThat(group13.getCurrentVersion().isDeleted(), is(true));
            }
        });

        checkDump("groups2", "groups2", Arrays.asList("component-component-28.json"));
    }

    @Test
    public void testDiscovery() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("groups4.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "groups4.data", false);

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

        failureDetector.active = false;

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                Times.setTest(3000);
                space.addPeriod();
            }
        });

        failureDetector.active = true;

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

        checkDump("groups3", "groups3", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
    }

    @Test
    public void testAvailability() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("groups5.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "groups5.data", false);

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

                ComponentNodeSchema groupSchema = componentSchema.findNode("Group1");
                INodeIndex<Long, IGroupComponent> groupIndex = componentSpace.getIndex(groupSchema.getIndexField());
                IGroupComponent group = groupIndex.find(nameManager.findByName(Names.getScope("groups.group1")).getId());
                group.enableMaintenanceMode("Test group maintenance.");
                group.enableMaintenanceMode(null);

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

                ComponentNodeSchema groupSchema = componentSchema.findNode("Group1");
                INodeIndex<Long, IGroupComponent> groupIndex = componentSpace.getIndex(groupSchema.getIndexField());
                IGroupComponent group = groupIndex.find(nameManager.findByName(Names.getScope("groups.group1")).getId());
                group.disableMaintenanceMode();
                group.disableMaintenanceMode();
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

        checkDump("groups4", "groups4", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json"));
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
