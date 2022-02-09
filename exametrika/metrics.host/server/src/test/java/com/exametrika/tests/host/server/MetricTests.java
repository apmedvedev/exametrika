/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.host.server;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.component.config.MailAlertChannelConfiguration;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Classes;
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
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.component.IAgentActionExecutor;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.tests.component.ComponentTests;
import com.exametrika.tests.component.ComponentTests.TestAgentActionExecutor;
import com.exametrika.tests.component.ComponentTests.TestAgentFailureDetector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * The {@link MetricTests} are tests for metrics of component model.
 *
 * @author Medvedev-A
 */
public class MetricTests {
    private IDatabaseFactory.Parameters parameters;
    private DatabaseConfiguration configuration;
    private Database database;
    private File tempDir;
    private TestAgentActionExecutor actionExecutor = new TestAgentActionExecutor();
    private TestAgentFailureDetector failureDetector = new TestAgentFailureDetector();

    @Before
    public void setUp() {
        TestMode.setTest(false);
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
    public void testHost() throws Throwable {
        TestMode.setTest(false);
        Times.setTest(1000);
        createDatabase("host.conf");

        final String resourcePath = "classpath:" + Classes.getResourcePath(ComponentTests.class) + "/data/";
        final JsonObject contents = (JsonObject) JsonSerializers.load(resourcePath + "host.data", false);

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

        checkDump("host", "host", Arrays.asList("aggregation-aggregation-p1-17.json", "aggregation-aggregation-p2-28.json",
                "component-component-126.json"));
    }

    private void createDatabase(String schemaConfigName) {
        final String resourcePath = "classpath:" + Classes.getResourcePath(ComponentTests.class) + "/data/";
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

        final String resourcePath = "classpath:" + Classes.getResourcePath(ComponentTests.class) + "/data/";
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
