/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;


class TempState {
    float minDistance;
    int bestIndex;
    int behaviorType;
    boolean anomaly;
    boolean primaryAnomaly;
}