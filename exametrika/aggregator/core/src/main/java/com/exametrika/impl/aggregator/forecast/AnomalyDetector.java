/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.TestMode;
import com.exametrika.impl.exadb.core.Constants;


/**
 * The {@link AnomalyDetector} is an anomaly detector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AnomalyDetector implements IAnomalyDetector {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1720;// <forecastSpace.header> + magic(short) + version(byte) + prevAdjusted(byte) + 
    private static final int BASE_OFFSET = AnomalyDetectorSpace.HEADER_SIZE;// + head(int) + count(int) + sum(float) + sumSquares(float) + 
    private static final int MAGIC_OFFSET = BASE_OFFSET;// + min(float) + max(float) + sensitivity(float) + anomalyCount(int) + currentElementId(int) +
    private static final int VERSION_OFFSET = BASE_OFFSET + 2;// + lastTime(long) + startElementId(int) +  + prevSimilarElementId(int) +
    private static final int PREV_ADJUSTED_OFFSET = BASE_OFFSET + 3;// + values(float * maxElementCount) +  + elements(<element.header> * maxElementCount)
    private static final int HEAD_OFFSET = BASE_OFFSET + 4;
    private static final int COUNT_OFFSET = BASE_OFFSET + 8;
    private static final int SUM_OFFSET = BASE_OFFSET + 12;
    private static final int SUM_SQUARES_OFFSET = BASE_OFFSET + 16;
    private static final int MIN_OFFSET = BASE_OFFSET + 20;
    private static final int MAX_OFFSET = BASE_OFFSET + 24;
    private static final int SENSITIVITY_OFFSET = BASE_OFFSET + 28;
    private static final int ANOMALY_COUNT_OFFSET = BASE_OFFSET + 32;
    private static final int CURRENT_ELEMENT_ID_OFFSET = BASE_OFFSET + 36;
    private static final int LAST_TIME_OFFSET = BASE_OFFSET + 40;
    private static final int START_ELEMENT_ID_OFFSET = BASE_OFFSET + 48;
    private static final int PREV_SIMILAR_ELEMENT_ID_OFFSET = BASE_OFFSET + 52;
    private static final int VALUES_OFFSET = BASE_OFFSET + 56;
    private static final int ELEMENT_HEADER_SIZE = 5;// type(int) + anomalyCount(byte)
    private static final int DATA_SIZE = ELEMENT_HEADER_SIZE + 4;
    private static final int ELEMENT_TYPE_OFFSET = 0;
    private static final int ELEMENT_ANOMALY_COUNT_OFFSET = 4;
    static final int FORECAST_WINDOW_SIZE = 20;
    static final float FORECAST_WARPING_BAND = 0.1f;
    static final int FORECAST_MAX_ELEMENT_COUNT = 10000;
    private final Parameters parameters;
    private final IRawPage headerPage;
    private final int maxElementCount;
    private final Dtw state;
    private final IBehaviorTypeIdAllocator typeIdAllocator;

    public static class Parameters {
        public boolean fast = false;
        public long aggregationPeriod;
        public float anomalyThreshold = 0.9f;
        public float warningThreshold = 0.7f;
        public boolean sensitivityAutoAdjustment = true;
        public float initialSensitivity = 0.1f;
        public float sensitivityIncrement = 0.01f;
        public float maxSensitivity = 0.25f;
        public int initialLearningPeriod = 200;
        public int initialAdjustmentLearningPeriod = 150;
        public int anomaliesEstimationPeriod = 100;
        public int maxAnomaliesPerEstimationPeriodPercentage = 5;
        public byte maxAnomaliesPerType = 2;
        public boolean disableTypes = false;

        public Parameters(long aggregationPeriod) {
            this.aggregationPeriod = aggregationPeriod;
        }
    }

    public static AnomalyDetector create(IRawPage headerPage, Parameters parameters, Dtw state, IBehaviorTypeIdAllocator typeIdAllocator) {
        AnomalyDetector space = new AnomalyDetector(headerPage, parameters, state, typeIdAllocator);
        space.writeHeader();

        return space;
    }

    public static AnomalyDetector open(IRawPage headerPage, Parameters parameters, Dtw state, IBehaviorTypeIdAllocator typeIdAllocator) {
        AnomalyDetector space = new AnomalyDetector(headerPage, parameters, state, typeIdAllocator);
        space.readHeader();

        return space;
    }

    @Override
    public int getId() {
        return (int) headerPage.getIndex();
    }

    @Override
    public AnomalyResult computeAnomaly(long time, float value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        int index = add(region);
        region.writeFloat(VALUES_OFFSET + index * 4, value);

        computeMinMax(region, value);

        int currentElementId = region.readInt(CURRENT_ELEMENT_ID_OFFSET) + 1;
        region.writeInt(CURRENT_ELEMENT_ID_OFFSET, currentElementId);

        if (currentElementId >= 2 * FORECAST_WINDOW_SIZE)
            computeEuclidianDistance(region);
        else {
            state.minDistance = Float.NaN;
            state.bestIndex = 0;
        }

        float distance = normalize(region, state.minDistance);
        if (distance < parameters.warningThreshold)
            distance = 0;

        state.behaviorType = 0;
        state.anomaly = false;
        state.primaryAnomaly = false;

        if (!parameters.disableTypes)
            addElement(region, distance, state.bestIndex);
        else {
            state.behaviorType = 0;
            state.anomaly = distance > parameters.anomalyThreshold;
            state.primaryAnomaly = state.anomaly;
        }

        if (TestMode.isTest() && !state.anomaly) {
            state.anomaly = true;
            state.primaryAnomaly = true;
        }

        int res = allowAnomalyScore(region, currentElementId, time);
        if (res > 0) {
            if (parameters.sensitivityAutoAdjustment && res >= 1)
                adjustSensitivity(region, distance, currentElementId);

            if (res == 2)
                return new AnomalyResult(distance, state.anomaly ? state.behaviorType : 0, state.anomaly, state.primaryAnomaly);
        }

        return new AnomalyResult(0.5f, state.behaviorType, state.anomaly, state.primaryAnomaly);
    }

    public JsonObject getStatistics() {
        IRawReadRegion region = headerPage.getReadRegion();

        int count = region.readInt(COUNT_OFFSET);
        float sum = region.readFloat(SUM_OFFSET);
        float min = region.readFloat(MIN_OFFSET);
        float max = region.readFloat(MAX_OFFSET);
        float sumSquares = region.readFloat(SUM_SQUARES_OFFSET);
        float mean = sum / count;
        float stddev = (float) Math.sqrt(sumSquares / count - mean * mean);

        float sensitivity = region.readFloat(SENSITIVITY_OFFSET);
        float anomalyCount = region.readFloat(ANOMALY_COUNT_OFFSET);

        int typesCount = 0;
        int multiTypesCount = 0;
        if (!parameters.disableTypes) {
            int elementsOffset = getElementsOffset();
            TIntIntMap types = new TIntIntHashMap();
            for (int i = 0; i < count; i++) {
                int elementOffset = elementsOffset + getIndex(region, i) * ELEMENT_HEADER_SIZE;
                int type = region.readInt(elementOffset + ELEMENT_TYPE_OFFSET);
                types.adjustOrPutValue(type, 1, 1);
            }

            final int[] counter = new int[1];
            types.forEachValue(new TIntProcedure() {
                @Override
                public boolean execute(int value) {
                    if (value > 1)
                        counter[0]++;
                    return true;
                }
            });

            typesCount = types.size();
            multiTypesCount = counter[0];
        }

        return Json.object()
                .put("count", count)
                .put("mean", mean)
                .put("stddev", stddev)
                .put("min", min)
                .put("max", max)
                .put("sensitivity", sensitivity)
                .put("anomalyCount", anomalyCount)
                .put("types", typesCount)
                .put("multiTypes", multiTypesCount)
                .toObject();
    }

    private AnomalyDetector(IRawPage headerPage, Parameters parameters, Dtw state, IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.notNull(headerPage);
        Assert.notNull(parameters);
        Assert.notNull(state);
        Assert.notNull(typeIdAllocator);

        this.parameters = parameters;
        this.headerPage = headerPage;
        if (!parameters.disableTypes)
            this.maxElementCount = Math.min(FORECAST_MAX_ELEMENT_COUNT, (headerPage.getSize() - VALUES_OFFSET) / DATA_SIZE);
        else
            this.maxElementCount = Math.min(FORECAST_MAX_ELEMENT_COUNT, (headerPage.getSize() - VALUES_OFFSET) / 4);
        this.state = state;
        this.typeIdAllocator = typeIdAllocator;
    }

    private void readHeader() {
        IRawReadRegion region = headerPage.getReadRegion();

        short magic = region.readShort(MAGIC_OFFSET);
        byte version = region.readByte(VERSION_OFFSET);

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(headerPage.getFile().getIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(headerPage.getFile().getIndex(), version, Constants.VERSION));
    }

    private void writeHeader() {
        IRawWriteRegion region = headerPage.getWriteRegion();

        region.fill(BASE_OFFSET, headerPage.getSize() - BASE_OFFSET, (byte) 0);
        region.writeShort(MAGIC_OFFSET, MAGIC);
        region.writeByte(VERSION_OFFSET, Constants.VERSION);

        region.writeFloat(MIN_OFFSET, Float.MAX_VALUE);
        region.writeFloat(MAX_OFFSET, -Float.MAX_VALUE);
        region.writeFloat(SENSITIVITY_OFFSET, parameters.initialSensitivity);
    }

    private void computeMinMax(IRawWriteRegion region, float value) {
        int count = region.readInt(COUNT_OFFSET);
        float sum = region.readFloat(SUM_OFFSET) + value;
        region.writeFloat(SUM_OFFSET, sum);
        float sumSquares = region.readFloat(SUM_SQUARES_OFFSET) + value * value;
        region.writeFloat(SUM_SQUARES_OFFSET, sumSquares);

        float average = sum / count;
        float stddev = (float) Math.sqrt(sumSquares / count - average * average);

        if (value < average - 3 * stddev || value > average + 3 * stddev)
            return;

        if (value < region.readFloat(MIN_OFFSET))
            region.writeFloat(MIN_OFFSET, value);
        if (value > region.readFloat(MAX_OFFSET))
            region.writeFloat(MAX_OFFSET, value);
    }

    private void correctStatistics(IRawWriteRegion region, int index) {
        float value = region.readFloat(VALUES_OFFSET + index * 4);

        float sum = region.readFloat(SUM_OFFSET) - value;
        region.writeFloat(SUM_OFFSET, sum);

        float sumSquares = region.readFloat(SUM_SQUARES_OFFSET) - value * value;
        region.writeFloat(SUM_SQUARES_OFFSET, sumSquares);
    }

    private void computeEuclidianDistance(IRawReadRegion region) {
        float min = region.readFloat(MIN_OFFSET);
        float max = region.readFloat(MAX_OFFSET);
        float sensitivity = region.readFloat(SENSITIVITY_OFFSET);
        float sensitivityThreshold = parameters.warningThreshold / parameters.anomalyThreshold * sensitivity;
        float distanceThreshold = sensitivityThreshold * sensitivityThreshold * (max - min) * (max - min) * FORECAST_WINDOW_SIZE;
        float maxDistanceThreshold = sensitivity * sensitivity * (max - min) * (max - min) * FORECAST_WINDOW_SIZE;

        int count = region.readInt(COUNT_OFFSET);

        float minSumSquares = Float.MAX_VALUE;
        int bestIndex = 0;
        for (int i = count - 1 - FORECAST_WINDOW_SIZE; i >= FORECAST_WINDOW_SIZE - 1; i--) {
            float sumSquares = 0;
            for (int k = 0; k < FORECAST_WINDOW_SIZE; k++) {
                int queryOffset = VALUES_OFFSET + getIndex(region, count - 1 - k) * 4;
                int dataOffset = VALUES_OFFSET + getIndex(region, i - k) * 4;

                float delta = region.readFloat(dataOffset) - region.readFloat(queryOffset);
                sumSquares += delta * delta;
            }
            if (sumSquares < minSumSquares) {
                minSumSquares = sumSquares;
                bestIndex = i;
                if (minSumSquares <= distanceThreshold)
                    break;
            }
        }

        if (parameters.fast || minSumSquares <= maxDistanceThreshold) {
            state.minDistance = (float) Math.sqrt(minSumSquares);
            state.bestIndex = bestIndex;
        } else
            computeDtwDistance(region);
    }

    private void computeDtwDistance(IRawReadRegion region) {
        int count = region.readInt(COUNT_OFFSET);
        copy(region, count - FORECAST_WINDOW_SIZE, FORECAST_WINDOW_SIZE, state.getQuery());
        copy(region, 0, count - FORECAST_WINDOW_SIZE, state.getData());
        state.setDataLength(count - FORECAST_WINDOW_SIZE);

        Dtw.reverse(state.getQuery(), FORECAST_WINDOW_SIZE);
        Dtw.reverse(state.getData(), state.getDataLength());

        state.compute(0);
        state.minDistance = state.getDistance();
        state.bestIndex = count - state.getIndex() - 2;
    }

    private void addElement(IRawWriteRegion region, float distance, int prevSimilarIndex) {
        int elementsOffset = getElementsOffset();
        int currentIndex = region.readInt(COUNT_OFFSET) - 1;
        int currentElementOffset = elementsOffset + ELEMENT_HEADER_SIZE * getIndex(region, currentIndex);
        int currentElementId = region.readInt(CURRENT_ELEMENT_ID_OFFSET);
        boolean anomaly = distance > parameters.anomalyThreshold;

        if (prevSimilarIndex != 0) {
            int prevSimilarElementOffset = elementsOffset + ELEMENT_HEADER_SIZE * getIndex(region, prevSimilarIndex);
            int prevSimilarElementId = currentElementId - (currentIndex - prevSimilarIndex);

            int type = region.readInt(prevSimilarElementOffset + ELEMENT_TYPE_OFFSET);
            int anomalyCount = region.readByte(prevSimilarElementOffset + ELEMENT_ANOMALY_COUNT_OFFSET) - 1;
            if (!anomaly && type != 0) {
                region.writeInt(currentElementOffset + ELEMENT_TYPE_OFFSET, type);
                region.writeByte(currentElementOffset + ELEMENT_ANOMALY_COUNT_OFFSET, (byte) Math.max(0, anomalyCount));
                region.writeInt(PREV_SIMILAR_ELEMENT_ID_OFFSET, prevSimilarElementId);

                state.behaviorType = type;
                state.anomaly = anomalyCount > 0;
                state.primaryAnomaly = false;
                return;
            }
        }

        int typeId = 0;
        byte initialAnomalyCount = 0;
        if (anomaly && currentElementId >= parameters.initialLearningPeriod) {
            initialAnomalyCount = parameters.maxAnomaliesPerType;
            int prevElementOffset = getElementOffset(region, currentElementId - 1);
            int anomalyCount = region.readByte(prevElementOffset + ELEMENT_ANOMALY_COUNT_OFFSET);
            if (anomalyCount == 0)
                typeId = typeIdAllocator.allocateTypeId();
            else
                typeId = region.readInt(prevElementOffset + ELEMENT_TYPE_OFFSET);
        }

        region.writeInt(currentElementOffset + ELEMENT_TYPE_OFFSET, typeId);
        region.writeByte(currentElementOffset + ELEMENT_ANOMALY_COUNT_OFFSET, initialAnomalyCount);
        region.writeInt(PREV_SIMILAR_ELEMENT_ID_OFFSET, 0);

        state.behaviorType = typeId;
        state.anomaly = anomaly;
        state.primaryAnomaly = anomaly;
    }

    private void adjustSensitivity(IRawWriteRegion region, float distance, int currentElementId) {
        int anomalyCount = region.readInt(ANOMALY_COUNT_OFFSET);
        if (distance > parameters.anomalyThreshold)
            anomalyCount++;

        if ((currentElementId % parameters.anomaliesEstimationPeriod) == 0) {
            byte prevAdjusted = region.readByte(PREV_ADJUSTED_OFFSET);
            float sensitivity = region.readFloat(SENSITIVITY_OFFSET);
            if (anomalyCount >= parameters.anomaliesEstimationPeriod * parameters.maxAnomaliesPerEstimationPeriodPercentage / 100) {
                sensitivity = Math.min(parameters.maxSensitivity, sensitivity + parameters.sensitivityIncrement);
                prevAdjusted = 1;
            } else if (prevAdjusted != 0) {
                sensitivity -= parameters.sensitivityIncrement;
                prevAdjusted = 0;
            }

            region.writeFloat(SENSITIVITY_OFFSET, sensitivity);
            region.writeByte(PREV_ADJUSTED_OFFSET, prevAdjusted);
            anomalyCount = 0;
        }

        region.writeInt(ANOMALY_COUNT_OFFSET, anomalyCount);
    }

    private float normalize(IRawReadRegion region, float distance) {
        float min = region.readFloat(MIN_OFFSET);
        float max = region.readFloat(MAX_OFFSET);
        distance /= Math.abs(max - min) * Math.sqrt(FORECAST_WINDOW_SIZE);
        if (Float.isNaN(distance))
            distance = 0;

        float sensitivity = region.readFloat(SENSITIVITY_OFFSET);
        return Math.max(0, Math.min(1, distance / sensitivity * parameters.anomalyThreshold));
    }

    private int allowAnomalyScore(IRawWriteRegion region, int currentElementId, long time) {
        long lastTime = region.readLong(LAST_TIME_OFFSET);

        if (time - lastTime > 2 * parameters.aggregationPeriod)
            region.writeInt(START_ELEMENT_ID_OFFSET, currentElementId + FORECAST_WINDOW_SIZE);

        region.writeLong(LAST_TIME_OFFSET, time);

        int startElementId = region.readInt(START_ELEMENT_ID_OFFSET);

        if (currentElementId >= startElementId) {
            if (currentElementId >= parameters.initialLearningPeriod)
                return 2;
            if (currentElementId >= parameters.initialAdjustmentLearningPeriod)
                return 1;
        }

        return 0;
    }

    private int getIndex(IRawReadRegion region, int pos) {
        return (region.readInt(HEAD_OFFSET) + pos) % maxElementCount;
    }

    private int add(IRawWriteRegion region) {
        int head = region.readInt(HEAD_OFFSET);
        int count = region.readInt(COUNT_OFFSET);

        int next = (head + count) % maxElementCount;
        if (count == maxElementCount) {
            correctStatistics(region, region.readInt(HEAD_OFFSET));
            region.writeInt(HEAD_OFFSET, (head + 1) % maxElementCount);
        } else
            region.writeInt(COUNT_OFFSET, count + 1);

        return next;
    }

    private void copy(IRawReadRegion region, int pos, int length, float[] destination) {
        int head = region.readInt(HEAD_OFFSET);
        if ((head + pos < maxElementCount) == (head + pos + length - 1 < maxElementCount))
            region.readFloatArray(VALUES_OFFSET + ((head + pos) % maxElementCount) * 4, destination, 0, length);
        else {
            int l = maxElementCount - (head + pos);
            region.readFloatArray(VALUES_OFFSET + (head + pos) * 4, destination, 0, l);
            region.readFloatArray(VALUES_OFFSET, destination, l, length - l);
        }
    }

    private int getElementOffset(IRawReadRegion region, int elementId) {
        if (elementId == 0)
            return 0;

        int elementsOffset = getElementsOffset();
        int currentElementIndex = region.readInt(COUNT_OFFSET) - 1;
        int currentElementId = region.readInt(CURRENT_ELEMENT_ID_OFFSET);
        int elementIndex = currentElementIndex - (currentElementId - elementId);
        if (elementIndex >= 0)
            return elementsOffset + ELEMENT_HEADER_SIZE * getIndex(region, elementIndex);
        else
            return 0;
    }

    private int getElementsOffset() {
        return VALUES_OFFSET + maxElementCount * 4;
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
