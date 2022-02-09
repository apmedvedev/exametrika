/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.tester.core.agent;

import org.hyperic.sigar.Sigar;


/**
 * The {@link SigarHolder} is a holder of {@link Sigar}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SigarHolder {
    public static final Sigar instance = new Sigar();
}
