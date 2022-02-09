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
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.component.IComponentService;
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
import com.exametrika.api.exadb.security.ISecuredTransaction;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISession;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.SecuredOperation;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.TestMode;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.AggregationService;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.security.LoginOperation;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.tests.component.ComponentTests.TestAgentActionExecutor;
import com.exametrika.tests.component.ComponentTests.TestAgentFailureDetector;


/**
 * The {@link ComponentSecurityTests} are tests for groups of component model.
 *
 * @author Medvedev-A
 */
public class ComponentSecurityTests {
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
    public void testComponentSecurity() throws Throwable {
        TestMode.setTest(true);
        Times.setTest(1000);
        createDatabase("security1.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "security1.data", false);

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ISecurityService securityService = transaction.findDomainService(ISecurityService.NAME);
                IUser user1 = securityService.addUser("user1");
                user1.setPassword("user1");
                user1.addRole("role1");
                user1.setLabels(Arrays.asList("hosts.host1", "groups"));
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

        final ISession[] out = new ISession[1];
        database.transactionSync(new LoginOperation("user1", "user1") {
            @Override
            protected void onLogin(ISession session) {
                out[0] = session;
            }
        });
        final ISession session1 = out[0];

        session1.transactionSync(new SecuredOperation() {
            @Override
            public void run(ISecuredTransaction transaction) {
                final IComponentService componentService = transaction.findDomainService(IComponentService.NAME);
                IHostComponent host1 = componentService.findComponent("hosts.host1");
                assertThat(host1 != null, is(true));
                host1.setOptions(Json.object().put("key1", "value1").toObject());

                IHostComponent host2 = componentService.findComponent("hosts.host2");
                assertThat(host2 == null, is(true));

                IGroupComponent group1 = componentService.findComponent("groups.group1.group11");
                assertThat(Collections.toList(((IGroupComponentVersion) group1.get()).getComponents().iterator()), is((List) Arrays.asList(host1)));

                IGroupComponent group2 = componentService.findComponent("groups.group2.group21");
                assertThat(group2 == null, is(true));

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            componentService.createGroup("test", "Group1");
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }

                try {
                    new Expected(IllegalStateException.class, new Runnable() {
                        @Override
                        public void run() {
                            componentService.getRootGroup();
                        }
                    });
                } catch (Throwable e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });

        checkDump("security1", "security1", Arrays.asList("component-component-28.json", "aggregation-aggregation-p2-17.json",
                "system-security-49.json"));
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
