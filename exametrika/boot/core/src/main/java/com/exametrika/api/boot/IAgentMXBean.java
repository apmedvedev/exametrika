/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.boot;


/**
 * The {@link IAgentMXBean} represents an agent MBean.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IAgentMXBean {
    /**
     * Name of MXBean of agent.
     */
    final String MBEAN_NAME = "com.exametrika.agent:type=Agent";

    /**
     * Name of system property indicating that agent is loaded.
     */
    final String AGENT_PROPERTY_NAME = "com.exametrika.agent";

    /**
     * Name of system property containing path to home directory.
     */
    final String HOME_PROPERTY_NAME = "com.exametrika.home";

    /**
     * Name of system property containing path to work directory.
     */
    final String WORKPATH_PROPERTY_NAME = "com.exametrika.workPath";

    /**
     * Name of system property containing version of platform.
     */
    final String VERSION_PROPERTY_NAME = "com.exametrika.version";

    /**
     * Starts agent.
     *
     * @param configurationPath path to agent configuration. Can be null if default configuration is used
     * @throws IllegalStateException if agent is already started
     */
    void start(String configurationPath);

    /**
     * Is agent started?
     *
     * @return true if agent is started
     */
    boolean isStarted();

    /**
     * Sets new agent configuration.
     *
     * @param configurationPath path to agent configuration
     * @throws IllegalStateException if agent is not started
     */
    void setConfiguration(String configurationPath);

    /**
     * Stops agent.
     */
    void stop();
}
