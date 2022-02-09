/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TestCaseFilterConfiguration} is a configuration for test case filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestCaseFilterConfiguration extends Configuration {
    private final NameFilter name;
    private final NameFilter tag;

    public TestCaseFilterConfiguration(NameFilter name, NameFilter tag) {
        this.name = name;
        this.tag = tag;
    }

    public NameFilter getName() {
        return name;
    }

    public NameFilter getTag() {
        return tag;
    }

    public boolean match(TestCaseConfiguration testCase) {
        if (name != null && !name.match(testCase.getName()))
            return false;
        if (tag != null) {
            for (String tag : testCase.getTags()) {
                if (this.tag.match(tag))
                    return true;
            }

            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestCaseFilterConfiguration))
            return false;

        TestCaseFilterConfiguration configuration = (TestCaseFilterConfiguration) o;
        return Objects.equals(name, configuration.name) && Objects.equals(tag, configuration.tag);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(name, tag);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
