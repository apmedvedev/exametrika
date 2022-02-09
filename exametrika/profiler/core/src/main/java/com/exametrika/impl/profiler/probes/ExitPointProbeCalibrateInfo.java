/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link ExitPointProbeCalibrateInfo} is an exit point calibrate info.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExitPointProbeCalibrateInfo {
    public final long normalCollectorFullOuterOverhead;
    public final long fastCollectorFullOuterOverhead;
    public final String configHash;

    public ExitPointProbeCalibrateInfo() {
        this(0, 0, "");
    }

    public ExitPointProbeCalibrateInfo(long normalCollectorFullOuterOverhead, long fastCollectorFullOuterOverhead, String configHash) {
        Assert.notNull(configHash);

        this.normalCollectorFullOuterOverhead = normalCollectorFullOuterOverhead;
        this.fastCollectorFullOuterOverhead = fastCollectorFullOuterOverhead;
        this.configHash = configHash;
    }

    public JsonObject toJson() {
        return Json.object()
                .put("normalCollectorFullOuterOverhead", normalCollectorFullOuterOverhead)
                .put("fastCollectorFullOuterOverhead", fastCollectorFullOuterOverhead)
                .put("configHash", configHash)
                .toObject();
    }

    public static ExitPointProbeCalibrateInfo fromJson(JsonObject object) {
        long normalCollectorFullOuterOverhead = object.get("normalCollectorFullOuterOverhead");
        long fastCollectorFullOuterOverhead = object.get("fastCollectorFullOuterOverhead");
        String configHash = object.get("configHash");

        return new ExitPointProbeCalibrateInfo(normalCollectorFullOuterOverhead, fastCollectorFullOuterOverhead, configHash);
    }
}
