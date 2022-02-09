/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.config.common.RuntimeMode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link InstrumentationConfiguration} represents a root instrumentation configuration object.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstrumentationConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.instrumentation-1.0";

    private static final IMessages messages = Messages.get(IMessages.class);
    private final RuntimeMode runtimeMode;
    private final Set<Pointcut> pointcuts;
    private final boolean debug;
    private final File debugPath;
    private final int maxJoinPointCount;

    /**
     * Creates an object.
     *
     * @param runtimeMode       runtime mode
     * @param pointcuts         list of pointcuts
     * @param debug             indicates that instrumentation runtime has to save instrumented classes at specified path
     * @param debugPath         path to save instrumented classes. Can be null, if debug is false
     * @param maxJoinPointCount maximum number of join points
     */
    public InstrumentationConfiguration(RuntimeMode runtimeMode, Set<? extends Pointcut> pointcuts, boolean debug, File debugPath,
                                        int maxJoinPointCount) {
        Assert.notNull(runtimeMode);
        Assert.notNull(pointcuts);
        Assert.isTrue(!debug || debugPath != null);

        this.runtimeMode = runtimeMode;

        Set<String> set = new HashSet<String>();
        for (Pointcut pointcut : pointcuts)
            Assert.isTrue(set.add(pointcut.getName()));

        this.pointcuts = Immutables.wrap(pointcuts);
        this.debug = debug;
        this.debugPath = debugPath;
        this.maxJoinPointCount = maxJoinPointCount;
    }

    public RuntimeMode getRuntimeMode() {
        return runtimeMode;
    }

    public Set<Pointcut> getPointcuts() {
        return pointcuts;
    }

    public boolean isDebug() {
        return debug;
    }

    public File getDebugPath() {
        return debugPath;
    }

    public int getMaxJoinPointCount() {
        return maxJoinPointCount;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstrumentationConfiguration))
            return false;

        InstrumentationConfiguration configuration = (InstrumentationConfiguration) o;
        return runtimeMode.equals(configuration.runtimeMode) && pointcuts.equals(configuration.pointcuts) &&
                debug == configuration.debug && Objects.equals(debugPath, configuration.debugPath) &&
                maxJoinPointCount == configuration.maxJoinPointCount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(runtimeMode, pointcuts, debug, debugPath, maxJoinPointCount);
    }

    @Override
    public String toString() {
        return messages.toString(Strings.toString(pointcuts, true), debug, debugPath).toString();
    }

    private interface IMessages {
        @DefaultMessage("pointcuts: \n{0}\ndebug: {1}, debug path: {2}")
        ILocalizedMessage toString(String pointcuts, boolean debug, File debugPath);
    }
}
