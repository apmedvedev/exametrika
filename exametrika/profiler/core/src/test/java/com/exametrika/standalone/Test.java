import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.profiler.config.StackProbeConfiguration.CombineType;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Debug;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.profiler.modelling.MeasurementsGenerator;
import com.exametrika.impl.profiler.modelling.MeasurementsGenerator.MeasurementProfile;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;


public class Test {
    public static void main(String[] args) {
        int nodesCount = 10;//100
        int primaryEntryPointNodesCount = 10;//100
        int transactionsPerNodeCount = 20;//100
        int transactionSegmentsDepth = 3;//3
        int logRecordsCount = 100;//100
        int stackDepth = 20;//20
        int leafStackEntriesCount = 100;//100
        int maxEndExitPointsCount = 10;//10
        int maxIntermediateExitPointsCount = 30;//30
        int exitPointsPerEntryCount = 10;//10
        CombineType combineType = CombineType.NODE;//null
        MeasurementProfile measurementProfile = MeasurementProfile.PROD;
        MeasurementsGenerator generator = new MeasurementsGenerator(nodesCount, primaryEntryPointNodesCount, transactionsPerNodeCount,
                transactionSegmentsDepth, logRecordsCount, stackDepth, leafStackEntriesCount, maxEndExitPointsCount,
                maxIntermediateExitPointsCount, exitPointsPerEntryCount, combineType, 1, measurementProfile);
        List<MeasurementSet> measurements = generator.generate();
        JsonArrayBuilder builder = new JsonArrayBuilder();
        int i = 0;
        for (MeasurementSet set : measurements)
            for (Measurement measurement : set.getMeasurements()) {
                builder.add(toJson(measurement, false));
                i++;
            }

        Debug.print("Measurements: " + i);
        File file = new File(System.getProperty("java.io.tmpdir"), "measurements.json");
        JsonSerializers.write(file, builder, true);

        IAggregationSchema schema = generator.generateSchema();
        SerializeNameDictionary dictionary = new SerializeNameDictionary();
        final ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, dictionary);
        for (MeasurementSet set : measurements)
            MeasurementSerializers.serializeMeasurementSet(serialization, set, schema, dictionary);

        try {
            File file2 = new File(System.getProperty("java.io.tmpdir"), "measurements.data");
            OutputStream stream = new BufferedOutputStream(new FileOutputStream(file2));
            stream.write(outputStream.getBuffer(), 0, outputStream.getLength());
            stream.close();
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private static JsonObject toJson(Measurement measurement, boolean full) {
        return Json.object()
                .put("id", Measurements.toJson(measurement.getId()))
                .putIf("value", measurement.getValue().toJson(), full)
                .putIf("metadata", measurement.getValue().getMetadata(), !full)
                .putIf("period", measurement.getPeriod(), full)
                .toObject();
    }
}
