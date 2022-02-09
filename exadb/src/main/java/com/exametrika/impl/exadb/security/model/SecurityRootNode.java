/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.api.exadb.security.IAuditRecord;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link SecurityRootNode} is a component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SecurityRootNode extends ObjectNodeObject {
    private static final int ROOT_GROUP_FIELD = 0;
    protected static final int BLOB_STORE_FIELD = 1;
    private static final int AUDIT_LOG_FIELD = 2;

    public SecurityRootNode(INode node) {
        super(node);
    }

    public IUserGroup getRootGroup() {
        ISingleReferenceField<IUserGroup> field = getField(ROOT_GROUP_FIELD);
        return field.get();
    }

    public IStructuredBlobField<IAuditRecord> getAuditLogField() {
        return getField(AUDIT_LOG_FIELD);
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        IUserGroup rootGroup = getSpace().createNode("", getSpace().getSchema().findNode("UserGroup"), "root");

        ISingleReferenceField<IUserGroup> field = getField(ROOT_GROUP_FIELD);
        field.set(rootGroup);

        IUser adminUser = getSpace().createNode("Admin", getSpace().getSchema().findNode("User"));
        adminUser.setPassword("adminadmin");
        adminUser.addRole("administrator");
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("rootGroup");
        json.startObject();
        ((INodeObject) getRootGroup()).dump(json, context);
        json.endObject();

        ISecurityService securityService = getTransaction().findDomainService(ISecurityService.NAME);
        boolean jsonUsers = false;
        for (IUser user : securityService.getUsers()) {
            if (!jsonUsers) {
                json.key("users");
                json.startObject();
                jsonUsers = true;
            }

            json.key(user.getName());
            json.startObject();
            ((INodeObject) user).dump(json, context);
            json.endObject();
        }

        if (jsonUsers)
            json.endObject();

        boolean jsonAuditLog = false;
        for (IAuditRecord record : getAuditLogField().getRecords()) {
            if (!jsonAuditLog) {
                json.key("auditLog");
                json.startArray();
                jsonAuditLog = true;
            }

            json.startObject();

            json.key("user");
            json.value(record.getUser());
            json.key("permission");
            json.value(record.getPermission());
            json.key("object");
            json.value(record.getObject());
            json.key("succeeded");
            json.value(record.isSucceeded());

            if ((context.getFlags() & IDumpContext.DUMP_TIMES) != 0) {
                json.key("time");
                json.value(record.getTime());
            }

            json.endObject();
        }

        if (jsonAuditLog)
            json.endArray();
    }
}