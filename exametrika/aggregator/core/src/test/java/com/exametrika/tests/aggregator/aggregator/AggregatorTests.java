/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.aggregator;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
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
import com.exametrika.impl.aggregator.AggregationService;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.spi.aggregator.IParentDomainHandlerFactory;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link AggregatorTests} are tests for aggregator.
 *
 * @author Medvedev-A
 */
public class AggregatorTests {
    private DatabaseConfiguration parentConfiguration;
    private IDatabaseFactory.Parameters parentParameters;
    private DatabaseConfiguration configuration;
    private IDatabaseFactory.Parameters parameters;
    private Database parentDatabase;
    private Database database;
    private File tempDir1;
    private File tempDir2;

    @Before
    public void setUp() {
        tempDir1 = new File(System.getProperty("java.io.tmpdir"), "db1");
        Files.emptyDir(tempDir1);

        DatabaseConfigurationBuilder builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir1.getPath());
        builder.setTimerPeriod(10);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        parentConfiguration = builder.toConfiguration();

        parentParameters = new IDatabaseFactory.Parameters();
        parentDatabase = new DatabaseFactory().createDatabase(parentParameters, parentConfiguration);
        parentDatabase.open();

        tempDir2 = new File(System.getProperty("java.io.tmpdir"), "db2");
        Files.emptyDir(tempDir2);

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir2.getPath());
        builder.setTimerPeriod(10);
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(100000000)).toConfiguration());
        configuration = builder.toConfiguration();

        parameters = new IDatabaseFactory.Parameters();
        parameters.parameters.put("parentDomain", parentDatabase);
        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();
    }

    @After
    public void tearDown() {
        IOs.close(database);
        IOs.close(parentDatabase);
        Numbers.clearTest();
    }

    @Test
    public void testNameAggregation1() throws Throwable {
        testCase("name1.conf", "name1Parent.conf", Arrays.asList("name1.data"), "name1", "name1", Arrays.asList("aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json", "aggregation-aggregation-p3-42.json", "aggregation-aggregation-p4-56.json"),
                Arrays.asList("aggregation-aggregation-p4-17.json"));
    }

    @Test
    public void testNameAggregation2() throws Throwable {
        testCase("name2.conf", "name2Parent.conf", Arrays.asList("name2.data"), "name2", "name2", Arrays.asList("aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json", "aggregation-aggregation-p3-42.json", "aggregation-aggregation-p4-56.json"),
                Arrays.asList("aggregation-aggregation-p3-17.json", "aggregation-aggregation-p4-31.json"));
    }

    @Test
    public void testNameAggregation3() throws Throwable {
        TestMode.setTest(true);
        testCase("name3.conf", null, Arrays.asList("name1.data"), "name3", "name3", Arrays.asList("aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json", "aggregation-aggregation-p3-42.json", "aggregation-aggregation-p4-56.json"), null);
    }

    @Test
    public void testStackAggregation1() throws Throwable {
        testCase("stack1.conf", "stack1Parent.conf", Arrays.asList("stack1.data"), "stack1", "stack1", Arrays.asList("aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json", "aggregation-aggregation-p3-42.json"), Arrays.asList("aggregation-aggregation-p3-17.json"));
    }

    @Test
    public void testStackAggregation2() throws Throwable {
        TestMode.setTest(true);
        testCase("stack2.conf", null, Arrays.asList("stack1.data"), "stack2", "stack2", Arrays.asList("aggregation-aggregation-p1-17.json",
                "aggregation-aggregation-p2-28.json", "aggregation-aggregation-p3-42.json"), null);
    }

    @Test
    public void testTransactionAggregation1() throws Throwable {
        Numbers.setTest();
        testCase("tx1.conf", "tx1Parent.conf", Arrays.asList("tx11.data", "tx12.data", "tx13.data", "tx14.data"), "tx1", "tx1",
                Arrays.asList("aggregation-aggregation-p1-17.json",
                        "aggregation-aggregation-p2-30.json", "aggregation-aggregation-p3-46.json"),
                Arrays.asList("aggregation-aggregation-p3-17.json"));
    }

    @Test
    public void testTransactionAggregation2() throws Throwable {
        Numbers.setTest();
        TestMode.setTest(true);
        testCase("tx2.conf", null, Arrays.asList("tx11.data", "tx12.data", "tx13.data", "tx14.data"), "tx2", "tx2",
                Arrays.asList("aggregation-aggregation-p1-17.json",
                        "aggregation-aggregation-p2-30.json", "aggregation-aggregation-p3-46.json"), null);
    }

    @Test
    public void testBackgroundAggregation1() throws Throwable {
        Numbers.setTest();
        testCase("bk1.conf", "bk1Parent.conf", Arrays.asList("bk11.data", "bk12.data", "bk13.data", "bk14.data"), "bk1", "bk1",
                Arrays.asList("aggregation-aggregation-p1-17.json",
                        "aggregation-aggregation-p2-30.json", "aggregation-aggregation-p3-46.json"),
                Arrays.asList("aggregation-aggregation-p3-17.json"));
    }

    @Test
    public void testBackgroundAggregation2() throws Throwable {
        Numbers.setTest();
        TestMode.setTest(true);
        testCase("bk2.conf", null, Arrays.asList("bk11.data", "bk12.data", "bk13.data", "bk14.data"), "bk2", "bk2",
                Arrays.asList("aggregation-aggregation-p1-17.json",
                        "aggregation-aggregation-p2-30.json", "aggregation-aggregation-p3-46.json"), null);
    }

    private void testCase(String schemaPath, String parentSchemaPath, List<String> dataPaths, String dumpDirectoryPath, String ethalonDirectoryPath,
                          List<String> resultFileNames, List<String> parentDomainFileNames) {
        final String resourcePath = "classpath:" + Classes.getResourcePath(getClass()) + "/data/";
        final String schemaResourcePath = resourcePath + schemaPath;
        database.transactionSync(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModules(schemaResourcePath, null);
            }
        });

        if (parentSchemaPath != null) {
            final String parentSchemaResourcePath = resourcePath + parentSchemaPath;
            parentDatabase.transactionSync(new SchemaOperation() {
                @Override
                public void run(ISchemaTransaction transaction) {
                    transaction.addModules(parentSchemaResourcePath, null);
                }
            });
        }

        final List<JsonObject> contentsList = new ArrayList<JsonObject>();
        for (String dataPath : dataPaths)
            contentsList.add((JsonObject) JsonSerializers.load(resourcePath + dataPath, false));

        for (int i = 0; i < 2; i++) {
            for (int k = 0; k < 2; k++) {
                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                        AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                        for (JsonObject contents : contentsList) {
                            MeasurementSet measurements = Measurements.fromJson(contents, dictionary);
                            aggregationService.aggregate(measurements);
                        }
                    }
                });

                database.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p2");
                        PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                        space.addPeriod();
                    }
                });
            }

            database.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p3");
                    PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                    space.addPeriod();
                }
            });

            if (parentSchemaPath != null) {
                parentDatabase.transactionSync(new Operation() {
                    @Override
                    public void run(ITransaction transaction) {
                        ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p3");
                        if (schema != null) {
                            PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                            space.addPeriod();
                        }
                    }
                });
            }
        }

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p4");
                if (schema != null) {
                    PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                    space.addPeriod();
                }
            }
        });

        if (parentSchemaPath != null) {
            parentDatabase.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    ICycleSchema schema = transaction.getCurrentSchema().findSchemaById("period:aggregation.aggregation.p4");
                    if (schema != null) {
                        PeriodSpace space = (PeriodSpace) schema.getCurrentCycle().getSpace();
                        space.addPeriod();
                    }
                }
            });
        }

        IOs.close(database);
        IOs.close(parentDatabase);

        parentDatabase = new DatabaseFactory().createDatabase(parentParameters, parentConfiguration);
        parentDatabase.open();

        database = new DatabaseFactory().createDatabase(parameters, configuration);
        database.open();

        database.getOperations().dump(new File(tempDir2, dumpDirectoryPath).getPath(), new DumpContext(IDumpContext.DUMP_ORPHANED,
                Json.object().put("dumpLogs", true).toObject()), null);

        if (parentSchemaPath != null)
            parentDatabase.getOperations().dump(new File(tempDir1, dumpDirectoryPath).getPath(), new DumpContext(IDumpContext.DUMP_ORPHANED,
                    Json.object().put("dumpLogs", true).toObject()), null);

        boolean failed = false;
        for (String resultFileName : resultFileNames)
            failed = !compare(tempDir2, dumpDirectoryPath, ethalonDirectoryPath, resourcePath, resultFileName, resultFileName) || failed;

        if (parentSchemaPath != null) {
            for (String resultFileName : parentDomainFileNames)
                failed = !compare(tempDir1, dumpDirectoryPath, ethalonDirectoryPath, resourcePath, resultFileName,
                        "parent-" + resultFileName) || failed;
        }

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

    public static class TestParentDomainHandlerFactory implements IParentDomainHandlerFactory {
        @Override
        public IMeasurementHandler createHander(IDatabaseContext context) {
            return new TestParentDomainHandler(context);
        }
    }

    private static class TestParentDomainHandler implements IMeasurementHandler {
        private final IDatabase parentDomain;
        private final IAggregationSchema schema;
        private final SerializeNameDictionary serializeDictionary;
        private DeserializeNameDictionary deserializeDictionary;

        public TestParentDomainHandler(IDatabaseContext context) {
            parentDomain = context.getDatabase().findParameter("parentDomain");
            ITransaction transaction = context.getTransactionProvider().getTransaction();
            AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);
            schema = aggregationService.getSchema().getConfiguration().getAggregationSchema().createSchema();

            INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
            serializeDictionary = new SerializeNameDictionary(dictionary);
        }

        @Override
        public boolean canHandle() {
            return true;
        }

        @Override
        public void handle(MeasurementSet measurements) {
            final ByteOutputStream outputStream = new ByteOutputStream();
            DataSerialization serialization = new DataSerialization(outputStream);
            serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);
            MeasurementSerializers.serializeMeasurementSet(serialization, measurements, schema, serializeDictionary);

            parentDomain.transactionSync(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    if (deserializeDictionary == null) {
                        INameDictionary dictionary = transaction.findExtension(IPeriodNameManager.NAME);
                        deserializeDictionary = new DeserializeNameDictionary(dictionary, null);
                    }

                    AggregationService aggregationService = transaction.findDomainService(AggregationService.NAME);

                    if (aggregationService != null) {
                        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
                        DataDeserialization deserialization = new DataDeserialization(inputStream);
                        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

                        MeasurementSet measurements = MeasurementSerializers.deserializeMeasurementSet(deserialization, schema, deserializeDictionary);
                        aggregationService.aggregate(measurements);
                    }
                }
            });
        }
    }
}
