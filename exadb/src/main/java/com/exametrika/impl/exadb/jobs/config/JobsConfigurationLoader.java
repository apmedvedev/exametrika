/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.jobs.config;

import com.exametrika.api.exadb.jobs.config.JobServiceConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link JobsConfigurationLoader} is a loader of {@link JobServiceConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobsConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("JobService")) {
            Long threadCount = element.get("threadCount", null);
            if (threadCount == null)
                threadCount = Long.valueOf(Runtime.getRuntime().availableProcessors() * 2);
            long schedulePeriod = element.get("schedulePeriod");
            return new JobServiceConfiguration(threadCount.intValue(), schedulePeriod);
        } else
            throw new InvalidConfigurationException();
    }
}