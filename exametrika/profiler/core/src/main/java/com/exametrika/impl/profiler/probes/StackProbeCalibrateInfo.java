/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link StackProbeCalibrateInfo} is a calibrate info.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackProbeCalibrateInfo {
    public static final int MIN_OVERHEAD = 5;
    public static final int MAX_SAMPLING_RATIO = 10000;
    public static final long SLOW_THRESHOLD = 1000000000;
    public static final long FAST_THRESHOLD = 100000;
    public final long normalCollectorInnerOverhead;
    public final long normalCollectorEstimatingOuterOverhead;
    public final long normalCollectorFullOuterOverhead;
    public final long fastCollectorInnerOverhead;
    public final long fastCollectorEstimatingOuterOverhead;
    public final long fastCollectorFullOuterOverhead;
    public final String configHash;

    public StackProbeCalibrateInfo() {
        this(0, 0, 0, 0, 0, 0, "");
    }

    public StackProbeCalibrateInfo(long normalCollectorInnerOverhead, long normalCollectorEstimatingOuterOverhead,
                                   long normalCollectorFullOuterOverhead, long fastCollectorInnerOverhead, long fastCollectorEstimatingOuterOverhead,
                                   long fastCollectorFullOuterOverhead, String configHash) {
        Assert.notNull(configHash);

        this.normalCollectorInnerOverhead = normalCollectorInnerOverhead;
        this.normalCollectorEstimatingOuterOverhead = normalCollectorEstimatingOuterOverhead;
        this.normalCollectorFullOuterOverhead = normalCollectorFullOuterOverhead;
        this.fastCollectorInnerOverhead = fastCollectorInnerOverhead;
        this.fastCollectorEstimatingOuterOverhead = fastCollectorEstimatingOuterOverhead;
        this.fastCollectorFullOuterOverhead = fastCollectorFullOuterOverhead;
        this.configHash = configHash;
    }

    public JsonObject toJson() {
        return Json.object()
                .put("normalCollectorInnerOverhead", normalCollectorInnerOverhead)
                .put("normalCollectorEstimatingOuterOverhead", normalCollectorEstimatingOuterOverhead)
                .put("normalCollectorFullOuterOverhead", normalCollectorFullOuterOverhead)
                .put("fastCollectorInnerOverhead", fastCollectorInnerOverhead)
                .put("fastCollectorEstimatingOuterOverhead", fastCollectorEstimatingOuterOverhead)
                .put("fastCollectorFullOuterOverhead", fastCollectorFullOuterOverhead)
                .put("configHash", configHash)
                .toObject();
    }

    public static StackProbeCalibrateInfo fromJson(JsonObject object) {
        long normalCollectorInnerOverhead = object.get("normalCollectorInnerOverhead");
        long normalCollectorEstimatingOuterOverhead = object.get("normalCollectorEstimatingOuterOverhead");
        long normalCollectorFullOuterOverhead = object.get("normalCollectorFullOuterOverhead");
        long fastCollectorInnerOverhead = object.get("fastCollectorInnerOverhead");
        long fastCollectorEstimatingOuterOverhead = object.get("fastCollectorEstimatingOuterOverhead");
        long fastCollectorFullOuterOverhead = object.get("fastCollectorFullOuterOverhead");
        String configHash = object.get("configHash");

        return new StackProbeCalibrateInfo(normalCollectorInnerOverhead, normalCollectorEstimatingOuterOverhead, normalCollectorFullOuterOverhead,
                fastCollectorInnerOverhead, fastCollectorEstimatingOuterOverhead, fastCollectorFullOuterOverhead, configHash);
    }
}
