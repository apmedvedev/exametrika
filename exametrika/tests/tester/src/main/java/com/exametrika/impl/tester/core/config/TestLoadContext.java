/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.config;

import java.util.Map;
import java.util.Set;

import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.api.tester.config.TestConfiguration;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.spi.tester.config.TestReporterConfiguration;


/**
 * The {@link TestLoadContext} is a helper class that is used to load {@link TestConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TestLoadContext implements IContextFactory, IConfigurationFactory {
    private String installationPath;
    private Set<String> roles;
    private String resultsPath;
    private Map<String, TestCaseConfiguration> testCases;
    private Map<String, TestReporterConfiguration> reporters;

    public void setInstallationPath(String value) {
        this.installationPath = value;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setResultsPath(String value) {
        this.resultsPath = value;
    }

    public void setTestCases(Map<String, TestCaseConfiguration> testCases) {
        this.testCases = testCases;
    }

    public void setReporters(Map<String, TestReporterConfiguration> reporters) {
        this.reporters = reporters;
    }

    @Override
    public Object createConfiguration(ILoadContext context) {
        if (testCases != null)
            return new TestConfiguration(installationPath, roles, resultsPath, testCases, reporters);
        else
            return null;
    }

    @Override
    public IConfigurationFactory createContext() {
        return new TestLoadContext();
    }
}
