/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.tester.core.coordinator.PlatformTestCaseBuilder;
import com.exametrika.spi.tester.ITestCaseBuilder;
import com.exametrika.spi.tester.config.TestCaseBuilderConfiguration;


/**
 * The {@link PlatformTestCaseBuilderConfiguration} is a configuration for platform test case builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformTestCaseBuilderConfiguration extends TestCaseBuilderConfiguration {
    private final Format format;

    public enum Format {
        PLAIN,
        JSON
    }

    public PlatformTestCaseBuilderConfiguration(Format format) {
        Assert.notNull(format);

        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    @Override
    public ITestCaseBuilder createBuilder() {
        return new PlatformTestCaseBuilder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PlatformTestCaseBuilderConfiguration))
            return false;

        PlatformTestCaseBuilderConfiguration configuration = (PlatformTestCaseBuilderConfiguration) o;
        return format == configuration.format;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode() + Objects.hashCode(format);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
