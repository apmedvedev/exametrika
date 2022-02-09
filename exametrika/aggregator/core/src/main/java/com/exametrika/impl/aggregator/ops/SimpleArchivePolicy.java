/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.ops;

import java.io.File;

import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.config.schema.SimpleArchivePolicySchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.spi.aggregator.IArchivePolicy;


/**
 * The {@link SimpleArchivePolicy} is a simple archive policy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleArchivePolicy implements IArchivePolicy {
    private final SimpleArchivePolicySchemaConfiguration configuration;

    public SimpleArchivePolicy(SimpleArchivePolicySchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public boolean allow(IPeriodSpace space) {
        PeriodSpace periodSpace = (PeriodSpace) space;
        int pathIndex = periodSpace.getSchema().getParent().getConfiguration().getPathIndex();
        String path = periodSpace.getTransaction().getDatabase().getConfiguration().getPaths().get(pathIndex);
        File spaceDir = new File(path, periodSpace.getFilePrefix());
        long size = Files.getSize(spaceDir);

        if (size > configuration.getMaxFileSize())
            return true;
        else
            return false;
    }
}
