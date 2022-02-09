/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.tester.config;

import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.tester.config.TestNodeConfiguration;
import com.exametrika.spi.tester.config.TestReporterConfiguration;


/**
 * The {@link TestConfiguration} is a configuration for test.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TestConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.tester.test-1.0";
    private final String installationPath;
    private final Set<String> roles;
    private final String resultsPath;
    private final Map<String, TestCaseConfiguration> testCases;
    private final Map<String, TestReporterConfiguration> reporters;

    public TestConfiguration(String installationPath, Set<String> roles, String resultsPath, Map<String, TestCaseConfiguration> testCases, Map<String, TestReporterConfiguration> reporters) {
        Assert.notNull(installationPath);
        Assert.notNull(roles);
        Assert.notNull(resultsPath);
        Assert.notNull(testCases);
        Assert.notNull(reporters);

        for (TestCaseConfiguration testCase : testCases.values()) {
            for (TestNodeConfiguration node : testCase.getNodes().values()) {
                if (node.getRole() != null)
                    Assert.isTrue(roles.contains(node.getRole()));
            }
        }

        this.installationPath = installationPath;
        this.roles = Immutables.wrap(roles);
        this.resultsPath = resultsPath;
        this.testCases = Immutables.wrap(testCases);
        this.reporters = Immutables.wrap(reporters);
    }

    public String getInstallationPath() {
        return installationPath;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getResultsPath() {
        return resultsPath;
    }

    public Map<String, TestCaseConfiguration> getTestCases() {
        return testCases;
    }

    public Map<String, TestReporterConfiguration> getReporters() {
        return reporters;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TestConfiguration))
            return false;

        TestConfiguration configuration = (TestConfiguration) o;
        return installationPath.equals(configuration.installationPath) && roles.equals(configuration.roles) &&
                resultsPath.equals(configuration.resultsPath) &&
                testCases.equals(configuration.testCases) && reporters.equals(configuration.reporters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(installationPath, roles, resultsPath, testCases, reporters);
    }

    @Override
    public String toString() {
        return testCases.toString();
    }
}
