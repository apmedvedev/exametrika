/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;


/**
 * The {@link BasePrefixCheckPermissionStrategy} is a base check permission strategy based on security labels as prefixes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BasePrefixCheckPermissionStrategy implements ICheckPermissionStrategy {
    @Override
    public boolean check(IPermission permission, Object object, ISubject subject) {
        String objectLabel = getObjectLabel(object);
        if (objectLabel == null)
            return true;

        return checkSubject(subject, objectLabel);
    }

    protected abstract String getObjectLabel(Object object);

    private boolean checkSubject(ISubject subject, String objectLabel) {
        if (subject.getLabels() != null) {
            for (String subjectLabel : subject.getLabels()) {
                if (objectLabel.startsWith(subjectLabel))
                    return true;
            }
        }

        if (subject instanceof IUser) {
            for (IUserGroup group : ((IUser) subject).getGroups()) {
                if (checkSubject(group, objectLabel))
                    return true;
            }
        } else {
            IUserGroup parent = ((IUserGroup) subject).getParent();
            if (parent != null && checkSubject(parent, objectLabel))
                return true;
        }

        return false;
    }
}
