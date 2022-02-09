/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;


/**
 * The {@link Permission} is a permission implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Permission implements IPermission {
    private final String name;
    private final int index;
    private final boolean auditable;
    private final List<String> levels;
    private SecurityService securityService;

    public Permission(String name, int index, boolean auditable) {
        Assert.notNull(name);

        this.name = name;
        this.index = index;
        this.auditable = auditable;

        String[] parts = name.split("[:]");
        List<String> levels = new ArrayList<String>();
        for (int i = 0; i < parts.length; i++)
            levels.add(parts[i].trim());

        this.levels = levels;
    }

    public void init(SecurityService securityService) {
        Assert.notNull(securityService);
        Assert.isNull(this.securityService);

        this.securityService = securityService;
    }

    public List<String> getLevels() {
        return levels;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isAccessAllowed(Object object) {
        if (securityService == null)
            return true;

        Session session = securityService.getSessionManager().getCurrentSession();
        if (session == null)
            return true;

        SecuredTransaction transaction = session.getTransaction();
        if (transaction == null || transaction.isPrivileged())
            return true;

        Principal principal = session.getPrincipal();
        if (principal.isAdministrator())
            return true;

        if (!principal.hasPermission(index))
            return false;

        ICheckPermissionStrategy checkPermissionStrategy = securityService.getSchema().getCheckPermissionStrategy();
        if (checkPermissionStrategy != null && object != null)
            return checkPermissionStrategy.check(this, object, principal.getUser());
        else
            return true;
    }

    @Override
    public void check(Object object) {
        if (securityService == null)
            return;

        boolean result = isAccessAllowed(object);
        if (auditable && securityService.isAuditEnabled())
            log(object, result);

        Assert.checkState(result);
    }

    @Override
    public void beginCheck(Object object) {
        check(object);

        if (securityService == null)
            return;
        Session session = securityService.getSessionManager().getCurrentSession();
        if (session == null)
            return;

        SecuredTransaction transaction = session.getTransaction();
        if (transaction != null)
            transaction.beginEntry();
    }

    @Override
    public void endCheck() {
        if (securityService == null)
            return;
        Session session = securityService.getSessionManager().getCurrentSession();
        if (session == null)
            return;

        SecuredTransaction transaction = session.getTransaction();
        if (transaction != null)
            transaction.endEntry();
    }

    @Override
    public String toString() {
        return name;
    }

    private void log(Object object, boolean result) {
        if (securityService == null)
            return;
        Session session = securityService.getSessionManager().getCurrentSession();
        if (session == null)
            return;

        IUser user = session.getPrincipal().getUser();
        securityService.addAuditRecord(new AuditRecord(user.getName(), name, object != null ? object.toString() : "",
                Times.getCurrentTime(), result));
    }
}
