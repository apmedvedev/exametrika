package com.exametrika.tests.aggregator.forecast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfigurationBuilder;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.aggregator.forecast.AnomalyDetector;
import com.exametrika.impl.aggregator.forecast.AnomalyDetectorSpace;
import com.exametrika.impl.aggregator.forecast.AnomalyProbability;
import com.exametrika.impl.aggregator.forecast.AnomalyResult;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseFactory;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.tests.aggregator.forecast.ForecasterTest.TestTypeIdAllocator;

@SuppressWarnings("unused")
public class AnomalyDetectorTest {
    private static final String basePath = "/home/apmedvedev/work/exametrika/src/tests.aggregator/standalone/com/exametrika/tests/aggregator/forecast/plot";
    private Database database;
    private DatabaseConfiguration parameters;
    private DatabaseConfigurationBuilder builder;

    public static void main(String[] args) throws Exception {
        AnomalyDetectorTest test = new AnomalyDetectorTest();
        test.setUp();
        test.test();
        test.tearDown();
    }

    public void setUp() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "db");
        Files.emptyDir(tempDir);

        builder = new DatabaseConfigurationBuilder();
        builder.addPath(tempDir.getPath());
        builder.setResourceAllocator(new RootResourceAllocatorConfigurationBuilder().setResourceProvider(
                new FixedResourceProviderConfiguration(200000000)).toConfiguration());
        parameters = builder.toConfiguration();

        database = new DatabaseFactory().createDatabase(null, parameters);
        database.open();

        PeriodNodeSchemaConfiguration nodeConfiguration1 = new PeriodNodeSchemaConfiguration(
                "node1", new IndexedLocationFieldSchemaConfiguration("field"), java.util.Collections.<FieldSchemaConfiguration>emptyList(), null);
        PeriodSpaceSchemaConfiguration space1 = new PeriodSpaceSchemaConfiguration("space1", "space1", null,
                Arrays.asList(new PeriodSchemaConfiguration("p1", new HashSet(Arrays.asList(nodeConfiguration1)),
                        null, null, 10000000, 2, false, null)), 0, 0);

        final DomainSchemaConfiguration configuration1 = new DomainSchemaConfiguration("test", new HashSet(Arrays.asList(space1)));
        database.transaction(new SchemaOperation() {
            @Override
            public void run(ISchemaTransaction transaction) {
                transaction.addModule(new ModuleSchemaConfiguration("test", new Version(1, 0, 0), configuration1), null);
            }
        });
    }

    public void tearDown() {
        IOs.close(database);
    }

    private void test() throws Exception {
        //test1();
        //test2(36);
        test3(4);
    }

    private void test1() throws Exception {
        int COUNT = 4000;
        double[] values = new double[COUNT];
        for (int i = 0; i < COUNT; i++) {
            double angle = i * Math.PI / 50;
            values[i] = 50 * Math.sin(angle);
        }

        test(values);
    }

    private void test2(int index) throws Exception {
        String[] files = new String[]
                {
                        "/data/nab/artificialNoAnomaly/art_daily_no_noise.csv",
                        "/data/nab/artificialNoAnomaly/art_daily_perfect_square_wave.csv",
                        "/data/nab/artificialNoAnomaly/art_daily_small_noise.csv",
                        "/data/nab/artificialNoAnomaly/art_flatline.csv",
                        "/data/nab/artificialNoAnomaly/art_noisy.csv",

                        "/data/nab/artificialWithAnomaly/art_daily_flatmiddle.csv", //#5
                        "/data/nab/artificialWithAnomaly/art_daily_jumpsdown.csv",
                        "/data/nab/artificialWithAnomaly/art_daily_jumpsup.csv",
                        "/data/nab/artificialWithAnomaly/art_daily_nojump.csv",
                        "/data/nab/artificialWithAnomaly/art_increase_spike_density.csv",
                        "/data/nab/artificialWithAnomaly/art_load_balancer_spikes.csv",

                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_24ae8d.csv",//#11
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_ac20cd.csv",
                        "/data/nab/realAWSCloudwatch/rds_cpu_utilization_cc0c53.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_53ea38.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_c6585a.csv",
                        "/data/nab/realAWSCloudwatch/rds_cpu_utilization_e47b3b.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_5f5533.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_fe7f93.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_77c1ca.csv",
                        "/data/nab/realAWSCloudwatch/ec2_cpu_utilization_825cc2.csv",

                        "/data/nab/realAWSCloudwatch/ec2_network_in_257a54.csv",//#21
                        "/data/nab/realAWSCloudwatch/ec2_network_in_5abac7.csv",
                        "/data/nab/realAWSCloudwatch/iio_us-east-1_i-a2eb1cd9_NetworkIn.csv",
                        "/data/nab/realAWSCloudwatch/elb_request_count_8c0756.csv",
                        "/data/nab/realAWSCloudwatch/ec2_disk_write_bytes_c0d644.csv",
                        "/data/nab/realAWSCloudwatch/ec2_disk_write_bytes_1ef3de.csv",
                        "/data/nab/realAWSCloudwatch/grok_asg_anomaly.csv",

                        "/data/nab/realKnownCause/ambient_temperature_system_failure.csv",//#28
                        "/data/nab/realKnownCause/ec2_request_latency_system_failure.csv",
                        "/data/nab/realKnownCause/cpu_utilization_asg_misconfiguration.csv",
                        "/data/nab/realKnownCause/machine_temperature_system_failure.csv",

                        "/data/ambient_temperature.csv",//#32
                        "/data/machine_temperature.csv",
                        "/data/art_load_balancer_spikes.csv",
                        "/data/rds_connections.csv",
                        "/data/cpu_5f553.csv",
                        "/data/cpu_cc0c5.csv",
                        "/data/cpu_825cc.csv",

                        "/data/rec-center-hourly1.csv",//#39
                        "/data/rec-center-hourly2.csv"
                };
        test(files[index], 1);
    }

    private void test3(int index) throws Exception {
        String[] files = new String[]
                {
                        "/data/prelert/farequote_ISO_8601.csv",
                        "/data/prelert/farequote.csv",
                        "/data/prelert/network.csv",
                        "/data/prelert/power-data.csv",
                        "/data/twitter/raw_data.csv"
                };
        test(files[index], 2);
    }

    private void test(String resourcePath, int pos) throws Exception {
        resourcePath = "classpath:" + Classes.getResourcePath(AnomalyDetectorTest.class) + resourcePath;
        String data = IOs.load(resourcePath, "UTF-8");

        String[] parts = data.split("[\n]");
        double[] values = new double[parts.length - 1];
        int i = 0;
        for (String part : parts) {
            i++;
            if (i <= 1)
                continue;

            String[] p = part.split("[,]");

            double value = Double.parseDouble(p[pos]);
            values[i - 2] = value;
        }

        test(values);
    }

    private void test(final double[] values) throws IOException {
        testAnomaly(values);
    }

    private void testAnomaly(final double[] values) throws IOException {
        final AnomalyDetector.Parameters anomalyDetectorParameters = new AnomalyDetector.Parameters(1);
        anomalyDetectorParameters.fast = false;

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                try {
                    AnomalyProbability anomalyProbability = new AnomalyProbability();
                    IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                    CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                    TestTypeIdAllocator allocator = new TestTypeIdAllocator();
                    AnomalyDetectorSpace space = AnomalyDetectorSpace.create(cycleSchema.getContext(), 100, "anomaly", cycleSchema, 2, allocator);

                    AnomalyDetector anomalyDetector = space.createAnomalyDetector(anomalyDetectorParameters);

                    BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + "/anomaly.csv"));

                    long t = System.nanoTime();
                    for (int i = 0; i < values.length; i++) {
                        if ((i % 1000) == 0)
                            System.out.println(i);

                        AnomalyResult result = anomalyDetector.computeAnomaly(i, (float) values[i]);
                        double probability = anomalyProbability.compute(result.getAnomalyScore());
                        ForecastIOs.write(writer, i, values[i], result, probability);
                    }

                    t = System.nanoTime() - t;
                    System.out.println("Anomaly performance:" + t / values.length);

                    System.out.println("AnomalyDetector statistics:\n" + anomalyDetector.getStatistics());

                    IOs.close(writer);
                } catch (IOException e) {
                    Exceptions.wrapAndThrow(e);
                }
            }
        });
    }

    private void testPerfAnomaly(final double[] values) throws IOException {
        final AnomalyDetector.Parameters anomalyDetectorParameters = new AnomalyDetector.Parameters(1);
        anomalyDetectorParameters.fast = false;

        database.transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                IPeriodSpaceSchema periodSchema = transaction.getCurrentSchema().findDomain("test").findSpace("space1");
                CycleSchema cycleSchema = (CycleSchema) periodSchema.getCycles().get(0);

                TestTypeIdAllocator allocator = new TestTypeIdAllocator();
                AnomalyDetectorSpace space = AnomalyDetectorSpace.create(cycleSchema.getContext(), 100, "anomaly", cycleSchema, 2,
                        allocator);

                AnomalyDetector anomalyDetector = space.createAnomalyDetector(anomalyDetectorParameters);

                long t = System.nanoTime();
                for (int i = 0; i < values.length; i++) {
                    if ((i % 1000) == 0)
                        System.out.println(i);

                    anomalyDetector.computeAnomaly(i, (float) values[i]);
                }

                t = System.nanoTime() - t;
                System.out.println("Anomaly performance:" + t / values.length);

                System.out.println("AnomalyDetector statistics:\n" + anomalyDetector.getStatistics());
            }
        });
    }
}
