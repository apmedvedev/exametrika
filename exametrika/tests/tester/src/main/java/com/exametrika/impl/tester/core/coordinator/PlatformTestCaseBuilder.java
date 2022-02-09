/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.coordinator;

import java.io.File;
import java.util.Map;

import com.exametrika.api.tester.config.GenericTestNodeConfiguration;
import com.exametrika.api.tester.config.PlatformTestCaseBuilderConfiguration;
import com.exametrika.api.tester.config.PlatformTestCaseBuilderConfiguration.Format;
import com.exametrika.api.tester.config.TestCaseConfiguration;
import com.exametrika.common.config.property.IPropertyResolver;
import com.exametrika.common.config.property.Properties;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.tester.ITestCaseBuilder;
import com.exametrika.spi.tester.config.TestNodeConfiguration;


/**
 * The {@link PlatformTestCaseBuilder} is a platform test case builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformTestCaseBuilder implements ITestCaseBuilder {
    private final PlatformTestCaseBuilderConfiguration configuration;

    public PlatformTestCaseBuilder(PlatformTestCaseBuilderConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public void build(String installationPath, String buildPath, TestCaseConfiguration testCase, TestNodeConfiguration n) {
        GenericTestNodeConfiguration node = (GenericTestNodeConfiguration) n;
        Assert.checkState(node.getRole() != null);
        File build = new File(buildPath);

        File roleDir = new File(installationPath, "roles" + File.separator + node.getRole());

        File testCaseRoleDir = new File(installationPath, "testCases" + File.separator + testCase.getName() + File.separator + node.getRole());
        if (testCaseRoleDir.exists())
            Files.copy(testCaseRoleDir, build);

        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            String[] confFiles = entry.getKey().split("[,]");
            for (String confFile : confFiles) {
                File buildConfFile = new File(build, confFile);
                if (!buildConfFile.exists()) {
                    File roleConfFile = new File(roleDir, confFile);
                    if (roleConfFile.exists())
                        Files.copy(roleConfFile, buildConfFile);
                }

                parameterize(buildConfFile, buildConfFile, (JsonObject) entry.getValue());
            }
        }
    }

    private void parameterize(File sourceFile, File targetFile, final JsonObject confElement) {
        if (!sourceFile.exists())
            return;

        if (configuration.getFormat() == Format.JSON) {
            JsonObjectBuilder element = JsonSerializers.read(Files.read(sourceFile), true);

            for (Map.Entry<String, Object> entry : confElement)
                element.update(entry.getKey(), entry.getValue());

            JsonSerializers.write(targetFile, element, true);
        } else if (configuration.getFormat() == Format.PLAIN) {
            String element = Files.read(sourceFile);
            element = Properties.expandProperties(null, new IPropertyResolver() {
                @Override
                public String resolveProperty(String propertyName) {
                    return confElement.get(propertyName, null);
                }
            }, element, false, false);
            Files.write(targetFile, element);
        } else
            Assert.error();
    }
}
