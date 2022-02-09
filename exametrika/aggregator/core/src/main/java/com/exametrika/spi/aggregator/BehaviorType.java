/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;

import java.util.List;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Strings;


/**
 * The {@link BehaviorType} is a behavior type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BehaviorType {
    private final String name;
    private final JsonObject metadata;
    private final List<String> labels;

    public BehaviorType(String name, JsonObject metadata, List<String> labels) {
        this.name = Strings.notNull(name);
        this.metadata = metadata;
        this.labels = Immutables.wrap(Collections.notNull(labels));
    }

    public String getName() {
        return name;
    }

    public JsonObject getMetadata() {
        return metadata;
    }

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return name + labels;
    }
}
