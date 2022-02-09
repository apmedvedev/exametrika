/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.support;

import com.exametrika.spi.profiler.IProbeCollector;

public class TestProbeCollector2 implements IProbeCollector {
    public boolean extractionRequired;
    public boolean extracted;
    public boolean activated;
    public boolean deactivated;

    @Override
    public boolean isExtractionRequired() {
        return extractionRequired;
    }

    @Override
    public void extract() {
        extracted = true;
    }

    @Override
    public void begin() {
        activated = true;
    }

    @Override
    public void end() {
        deactivated = true;
    }
}