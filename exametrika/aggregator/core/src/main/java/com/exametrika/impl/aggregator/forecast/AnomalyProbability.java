/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;

import com.exametrika.impl.aggregator.common.meters.MovingAverage;


/**
 * The {@link AnomalyProbability} contains methods to compute anomaly probability score.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AnomalyProbability {
    private static final int AVERAGING_WINDOW = 10;
    private static final double RED_THRESHOLD = 0.0001;
    private static final double YELLOW_THRESHOLD = 0.001;
    private static final double[] Q = new double[71];
    private final int learningPeriod;
    private final int probationaryPeriod;
    private final MovingAverage movingAverage = new MovingAverage(AVERAGING_WINDOW);
    private final Distribution distribution = new Distribution();
    private double prevLikelihood = 0;
    private int iterationCount;

    static {
        Q[0] = 0.500000000d;
        Q[1] = 0.460172163d;
        Q[2] = 0.420740291d;
        Q[3] = 0.382088578d;
        Q[4] = 0.344578258d;
        Q[5] = 0.308537539d;
        Q[6] = 0.274253118d;
        Q[7] = 0.241963652d;
        Q[8] = 0.211855399d;
        Q[9] = 0.184060125d;
        Q[10] = 0.158655254d;
        Q[11] = 0.135666061d;
        Q[12] = 0.115069670d;
        Q[13] = 0.096800485d;
        Q[14] = 0.080756659d;
        Q[15] = 0.066807201d;
        Q[16] = 0.054799292d;
        Q[17] = 0.044565463d;
        Q[18] = 0.035930319d;
        Q[19] = 0.028716560d;
        Q[20] = 0.022750132d;
        Q[21] = 0.017864421d;
        Q[22] = 0.013903448d;
        Q[23] = 0.010724110d;
        Q[24] = 0.008197536d;
        Q[25] = 0.006209665d;
        Q[26] = 0.004661188d;
        Q[27] = 0.003466974d;
        Q[28] = 0.002555130d;
        Q[29] = 0.001865813d;
        Q[30] = 0.001349898d;
        Q[31] = 0.000967603d;
        Q[32] = 0.000687138d;
        Q[33] = 0.000483424d;
        Q[34] = 0.000336929d;
        Q[35] = 0.000232629d;
        Q[36] = 0.000159109d;
        Q[37] = 0.000107800d;
        Q[38] = 0.000072348d;
        Q[39] = 0.000048096d;
        Q[40] = 0.000031671d;
        Q[41] = 0.000021771135897d;
        Q[42] = 0.000014034063752d;
        Q[43] = 0.000008961673661d;
        Q[44] = 0.000005668743475d;
        Q[45] = 0.000003551942468d;
        Q[46] = 0.000002204533058d;
        Q[47] = 0.000001355281953d;
        Q[48] = 0.000000825270644d;
        Q[49] = 0.000000497747091d;
        Q[50] = 0.000000297343903d;
        Q[51] = 0.000000175930101d;
        Q[52] = 0.000000103096834d;
        Q[53] = 0.000000059836778d;
        Q[54] = 0.000000034395590d;
        Q[55] = 0.000000019581382d;
        Q[56] = 0.000000011040394d;
        Q[57] = 0.000000006164833d;
        Q[58] = 0.000000003409172d;
        Q[59] = 0.000000001867079d;
        Q[60] = 0.000000001012647d;
        Q[61] = 0.000000000543915d;
        Q[62] = 0.000000000289320d;
        Q[63] = 0.000000000152404d;
        Q[64] = 0.000000000079502d;
        Q[65] = 0.000000000041070d;
        Q[66] = 0.000000000021010d;
        Q[67] = 0.000000000010644d;
        Q[68] = 0.000000000005340d;
        Q[69] = 0.000000000002653d;
        Q[70] = 0.000000000001305d;
    }

    public AnomalyProbability() {
        this(100, 50);
    }

    public AnomalyProbability(int learningPeriod, int estimationSamples) {
        probationaryPeriod = learningPeriod + estimationSamples;
        this.learningPeriod = learningPeriod;
    }

    public static double computeLog(double probability) {
        return Math.log(1.0000000001 - probability) / -23.02585084720009d;
    }

    public double compute(double anomalyScore) {
        double likelihood = 0.5;

        double averagedAnomalyScore = movingAverage.next(anomalyScore);
        if (iterationCount > learningPeriod)
            distribution.update(averagedAnomalyScore);

        if (iterationCount > probationaryPeriod) {
            likelihood = normalProbability(averagedAnomalyScore, distribution);

            if (prevLikelihood != 0)
                likelihood = filterLikelihood(likelihood, prevLikelihood);

            prevLikelihood = likelihood;
        }

        iterationCount++;

        return 1.0 - likelihood;
    }

    private double filterLikelihood(double likelihood, double prevLikelihood) {
        if (likelihood <= RED_THRESHOLD) {
            if (prevLikelihood > RED_THRESHOLD)
                return likelihood;
            else
                return YELLOW_THRESHOLD;
        } else
            return likelihood;
    }

    private double normalProbability(double x, Distribution distribution) {
        if (x < distribution.mean) {
            double xp = 2 * distribution.mean - x;
            return 1.0 - normalProbability(xp, distribution);
        }

        double xs = 10 * (x - distribution.mean) / distribution.stddev;
        int xsl = (int) Math.round(xs);
        if (xsl > 70)
            return 0.0;
        else
            return Q[xsl];
    }

    private static class Distribution {
        private int count;
        private double sum;
        private double sumSquares;
        private double mean;
        private double variance;
        private double stddev;

        public void update(double value) {
            count++;
            sum += value;
            sumSquares += value * value;

            mean = sum / count;
            variance = sumSquares / count - mean * mean;

            if (mean < 0.03)
                mean = 0.03;

            if (variance < 0.0003)
                variance = 0.0003;

            if (variance > 0)
                stddev = Math.sqrt(variance);
            else
                stddev = 0;
        }
    }
}
