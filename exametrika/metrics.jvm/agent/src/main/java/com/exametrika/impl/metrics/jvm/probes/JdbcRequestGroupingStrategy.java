/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.common.utils.Strings;
import com.exametrika.impl.metrics.jvm.probes.JdbcProbe.JdbcRawRequest;
import com.exametrika.spi.metrics.jvm.JdbcQueryInfo;
import com.exametrika.spi.profiler.IRequestGroupingStrategy;
import com.exametrika.spi.profiler.IScope;


/**
 * The {@link JdbcRequestGroupingStrategy} is a Jdbc request grouping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcRequestGroupingStrategy implements IRequestGroupingStrategy {
    @Override
    public String getRequestGroupName(IScope scope, Object request, String name, int level) {
        level++;
        JdbcRawRequest jdbcRequest = (JdbcRawRequest) request;
        JdbcQueryInfo query = jdbcRequest.getQuery();
        int parametersCount = query.getParameters().size();
        if (level <= parametersCount)
            return Strings.truncate(query.buildParametersText(parametersCount - level), 512, true);
        else if (level == parametersCount + 1)
            return jdbcRequest.getUrl();
        else
            return null;
    }
}
