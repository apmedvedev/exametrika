/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config;

import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;

/**
 * The {@link SecurityServiceConfiguration} represents a configuration of security service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityServiceConfiguration extends DomainServiceConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.security-1.0";
    private static final String NAME = "system.SecurityService";
    private final long sessionTimeoutPeriod;
    private final long roleMappingUpdatePeriod;

    public SecurityServiceConfiguration() {
        this(1800000, 60000);
    }

    public SecurityServiceConfiguration(long sessionTimeoutPeriod, long roleMappingUpdatePeriod) {
        super(NAME);

        this.sessionTimeoutPeriod = sessionTimeoutPeriod;
        this.roleMappingUpdatePeriod = roleMappingUpdatePeriod;
    }

    public long getSessionTimeoutPeriod() {
        return sessionTimeoutPeriod;
    }

    public long getRoleMappingUpdatePeriod() {
        return roleMappingUpdatePeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecurityServiceConfiguration))
            return false;

        SecurityServiceConfiguration configuration = (SecurityServiceConfiguration) o;
        return sessionTimeoutPeriod == configuration.sessionTimeoutPeriod && roleMappingUpdatePeriod == configuration.roleMappingUpdatePeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sessionTimeoutPeriod, roleMappingUpdatePeriod);
    }
}