/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.api.exadb.security.IAuditRecord;
import com.exametrika.common.utils.Assert;


/**
 * The {@link AuditRecord} is an audit record.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AuditRecord implements IAuditRecord {
    private String user;
    private String permission;
    private String object;
    private long time;
    private boolean succeeded;

    public AuditRecord(String user, String permission, String object, long time, boolean succeeded) {
        Assert.notNull(user);
        Assert.notNull(permission);

        this.user = user;
        this.permission = permission;
        this.object = object;
        this.time = time;
        this.succeeded = succeeded;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public String getObject() {
        return object;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public boolean isSucceeded() {
        return succeeded;
    }
}
