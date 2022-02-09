/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.model;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.security.SecurityService;


/**
 * The {@link RoleNode} is a role.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class RoleNode extends ObjectNodeObject implements IRole {
    private static final int NAME_FIELD = 0;
    private static final int SUBJECT_FIELD = 1;
    private static final int METADATA_FIELD = 2;
    private Object data;

    public RoleNode(INode node) {
        super(node);
    }

    public <T> T getData() {
        return (T) data;
    }

    public void setData(Object value) {
        this.data = value;
    }

    @Override
    public String getName() {
        IStringField field = getField(NAME_FIELD);
        return field.get();
    }

    @Override
    public ISubject getSubject() {
        ISingleReferenceField<ISubject> field = getField(SUBJECT_FIELD);
        return field.get();
    }

    public void setSubject(ISubject value) {
        ISingleReferenceField<ISubject> field = getField(SUBJECT_FIELD);
        field.set(value);
    }

    @Override
    public JsonObject getMetadata() {
        IJsonField field = getField(METADATA_FIELD);
        return field.get();
    }

    @Override
    public void setMetadata(JsonObject metadata) {
        IJsonField field = getField(METADATA_FIELD);
        field.set(metadata);
        data = null;
        updatePermissions();
    }

    @Override
    public void delete() {
        super.delete();

        updatePermissions();
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        IStringField field = getField(NAME_FIELD);
        field.set((String) args[0]);
    }

    @Override
    public void onUnloaded() {
        data = null;
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        if (getMetadata() != null) {
            json.key("metadata");
            JsonSerializers.write(json, getMetadata());
        }
    }

    @Override
    public String toString() {
        return getName() + "-" + super.toString();
    }

    protected void updatePermissions() {
        SecurityService securityService = getTransaction().findDomainService(ISecurityService.NAME);
        securityService.setUpdatePermissions();
    }
}