/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IStatisticsValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Objects;


/**
 * The {@link StatisticsValue} is a measurement data for statistics fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StatisticsValue implements IStatisticsValue {
    private final double sumSquares;

    public StatisticsValue(double sumSquares) {
        this.sumSquares = sumSquares;
    }

    @Override
    public double getSumSquares() {
        return sumSquares;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "stat");
        fields.put("sumSquares", sumSquares);

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StatisticsValue))
            return false;

        StatisticsValue data = (StatisticsValue) o;
        return sumSquares == data.sumSquares;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sumSquares);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
