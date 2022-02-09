/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.impl.exadb.security.model.SubjectNode;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;


/**
 * The {@link BasePatternCheckPermissionStrategy} is a base check permission strategy based on security labels as permission patterns.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BasePatternCheckPermissionStrategy implements ICheckPermissionStrategy {
    @Override
    public boolean check(IPermission permission, Object object, ISubject subject) {
        String objectLabel = getObjectLabel(object);
        if (objectLabel == null)
            return true;

        List<String> levels = new ArrayList<String>(((Permission) permission).getLevels());
        levels.add(objectLabel);

        return checkSubject(levels, subject, objectLabel);
    }

    protected abstract String getObjectLabel(Object object);

    private boolean checkSubject(List<String> levels, ISubject subject, String objectLabel) {
        if (subject.getLabels() != null) {
            List<PermissionPattern> patterns = ((SubjectNode) subject).getData();
            if (patterns == null) {
                patterns = new ArrayList<PermissionPattern>();
                for (String label : subject.getLabels())
                    patterns.add(PermissionPattern.parse(label));

                ((SubjectNode) subject).setData(patterns);
            }

            for (PermissionPattern pattern : patterns) {
                if (pattern.match(levels))
                    return true;
            }
        }

        if (subject instanceof IUser) {
            for (IUserGroup group : ((IUser) subject).getGroups()) {
                if (checkSubject(levels, group, objectLabel))
                    return true;
            }
        } else {
            IUserGroup parent = ((IUserGroup) subject).getParent();
            if (parent != null && checkSubject(levels, parent, objectLabel))
                return true;
        }

        return false;
    }
}
