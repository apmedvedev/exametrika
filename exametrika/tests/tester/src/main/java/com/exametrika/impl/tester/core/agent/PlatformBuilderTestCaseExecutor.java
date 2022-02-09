/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.utils.Files;


/**
 * The {@link PlatformBuilderTestCaseExecutor} represents a platform builder test case executor.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PlatformBuilderTestCaseExecutor extends BaseTestCaseExecutor {
    public PlatformBuilderTestCaseExecutor(String path, Map<String, Object> parameters, ICompartment compartment) {
        super(path, parameters, compartment);
    }

    @Override
    protected List<String> buildCommand(Map<String, Object> parameters) {
        return Arrays.asList("ant.bat", (String) parameters.get("buildConfig"));
    }

    @Override
    protected File getWorkingDir() {
        return new File(path, "build" + File.separator + "components" + File.separator + "natives");
    }

    @Override
    protected void doDestroy(String path) {
        File outDir = new File(this.path, "build" + File.separator + "components" + File.separator + "natives" +
                File.separator + parameters.get("buildConfig") + File.separator + "out" + File.separator + "lib");
        if (outDir.exists())
            Files.copy(outDir, new File(path));
    }
}
