/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import com.exametrika.spi.profiler.IRequestGroupingStrategy;
import com.exametrika.spi.profiler.IScope;


/**
 * The {@link UrlRequestGroupingStrategy} is a Url request grouping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UrlRequestGroupingStrategy implements IRequestGroupingStrategy {
    @Override
    public String getRequestGroupName(IScope scope, Object request, String name, int level) {
        level++;
        for (int i = name.length() - 1; i >= 0; i--) {
            char ch = name.charAt(i);
            if (ch == '#' || ch == '&' || ch == '?' || ch == '/')
                level--;

            if (level == 0) {
                String groupName = name.substring(0, i);
                char last = groupName.charAt(groupName.length() - 1);
                if (!groupName.isEmpty() && last != '/')
                    return groupName;

                break;
            }
        }

        return null;
    }
}
