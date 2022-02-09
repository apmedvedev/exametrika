/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.schema;

import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.RoleNodeSchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.SecurityRootNodeSchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.SecurityServiceSchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.UserGroupNodeSchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.UserNodeSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;


/**
 * The {@link SecuritySchemaBuilder} is a schema builder for security module.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SecuritySchemaBuilder {
    public void buildSchema(SecuritySchemaConfiguration securityModel, ModuleSchemaConfiguration module) {
        DomainSchemaConfiguration systemDomain = module.getSchema().findDomain("system");
        if (systemDomain == null) {
            systemDomain = new DomainSchemaConfiguration("system", false);
            module.getSchema().addDomain(systemDomain);
        }

        Assert.checkState(systemDomain.findSpace("security") == null);

        ObjectSpaceSchemaConfiguration securitySpace = new ObjectSpaceSchemaConfiguration("security",
                Collections.asSet(new UserNodeSchemaConfiguration(), new UserGroupNodeSchemaConfiguration(),
                        new RoleNodeSchemaConfiguration(), new SecurityRootNodeSchemaConfiguration()), "root");
        DomainServiceSchemaConfiguration securityService = new SecurityServiceSchemaConfiguration(securityModel);

        systemDomain.addSpace(securitySpace);
        systemDomain.addDomainService(securityService);
    }
}