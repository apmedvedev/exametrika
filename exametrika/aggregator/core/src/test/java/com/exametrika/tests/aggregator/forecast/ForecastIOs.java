package com.exametrika.tests.aggregator.forecast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import com.exametrika.impl.aggregator.forecast.AnomalyResult;
import com.exametrika.impl.aggregator.forecast.errors.PredictionErrorEstimator;

public final class ForecastIOs {
    private static final NumberFormat format;

    static {
        format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(false);
        format.setMaximumIntegerDigits(30);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        format.setRoundingMode(RoundingMode.HALF_UP);
    }

    public static void write(BufferedWriter writer, int i, double value, AnomalyResult result, double anomalyProbability) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(i);
        builder.append(',');
        builder.append(format.format(value));
        builder.append(',');
        builder.append(format.format(result.getAnomalyScore()));
        builder.append(',');
        builder.append(format.format(anomalyProbability));
        builder.append(',');
        builder.append(result.getBehaviorType());
        builder.append(',');
        builder.append(result.isAnomaly() ? "anomaly" : "");
        builder.append(',');
        builder.append(result.isPrimaryAnomaly() ? "primary" : "");
        builder.append('\n');

        writer.write(builder.toString());
    }

    public static void write(BufferedWriter writer, int i, double value, int predictionStepCount, float[] predictions,
                             PredictionErrorEstimator errorEstimator) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(i);
        builder.append(',');
        builder.append(format.format(value));

        for (int k = 0; k < predictionStepCount; k++) {
            builder.append(',');
            builder.append(format.format(predictions[k]));
//            builder.append(',');
//            builder.append(format.format(errorEstimator.getMaeError(k)));
//            builder.append(',');
//            builder.append(format.format(errorEstimator.getMaseError(k)));
//            builder.append(',');
//            builder.append(format.format(errorEstimator.getMapeError(k)));
//            builder.append(',');
//            builder.append(format.format(errorEstimator.getAltMapeError(k)));
//            builder.append(',');
//            builder.append(format.format(errorEstimator.getRmseError(k)));
        }

        builder.append('\n');
        writer.write(builder.toString());
    }

    private ForecastIOs() {
    }

}
