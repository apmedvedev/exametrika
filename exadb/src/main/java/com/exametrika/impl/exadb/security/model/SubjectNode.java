/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.model;

import java.util.List;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.security.IRole;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.security.SecurityService;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link SubjectNode} is a user.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SubjectNode extends ObjectNodeObject implements ISubject {
    protected static final int NAME_FIELD = 0;
    private static final int DESCRIPTION_FIELD = 1;
    private static final int METADATA_FIELD = 2;
    private static final int ROLES_FIELD = 3;
    private static final int LABELS_FIELD = 4;
    private Object data;

    public SubjectNode(INode node) {
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
    public String getDescription() {
        IStringField field = getField(DESCRIPTION_FIELD);
        return field.get();
    }

    @Override
    public void setDescription(String description) {
        IStringField field = getField(DESCRIPTION_FIELD);
        field.set(description);
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
    public Iterable<IRole> getRoles() {
        IReferenceField<IRole> field = getField(ROLES_FIELD);
        return field;
    }

    @Override
    public RoleNode findRole(String name) {
        IReferenceField<RoleNode> field = getField(ROLES_FIELD);
        for (RoleNode role : field) {
            if (role.getName().equals(name))
                return role;
        }

        return null;
    }

    @Override
    public IRole addRole(String name) {
        Assert.isTrue(!Strings.isEmpty(name));

        RoleNode role = findRole(name);
        if (role != null)
            return role;

        role = getSpace().createNode(null, getSpace().getSchema().findNode("Role"), name);

        IReferenceField<RoleNode> field = getField(ROLES_FIELD);
        field.add(role);

        role.setSubject(this);

        updatePermissions();

        return role;
    }

    @Override
    public void removeRole(String name) {
        IRole role = findRole(name);
        if (role == null)
            return;

        IReferenceField<IRole> field = getField(ROLES_FIELD);
        field.remove(role);
        role.delete();

        updatePermissions();
    }

    @Override
    public void removeAllRoles() {
        IReferenceField<IRole> field = getField(ROLES_FIELD);
        for (IRole role : field)
            role.delete();

        field.clear();

        updatePermissions();
    }

    @Override
    public List<String> getLabels() {
        ISerializableField<List<String>> field = getField(LABELS_FIELD);
        return field.get();
    }

    @Override
    public void setLabels(List<String> labels) {
        ISerializableField<List<String>> field = getField(LABELS_FIELD);
        field.set(labels);

        data = null;

        updatePermissions();
    }

    @Override
    public void delete() {
        for (IRole role : getRoles())
            role.delete();

        super.delete();
    }

    @Override
    public void onUnloaded() {
        data = null;
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        if (getDescription() != null) {
            json.key("description");
            json.value(getDescription());
        }
        if (getMetadata() != null) {
            json.key("metadata");
            JsonSerializers.write(json, getMetadata());
        }
        if (getLabels() != null) {
            json.key("labels");
            JsonSerializers.write(json, JsonUtils.toJson(getLabels()));
        }

        boolean jsonRoles = false;
        for (IRole role : getRoles()) {
            if (!jsonRoles) {
                json.key("roles");
                json.startObject();
                jsonRoles = true;
            }

            json.key(role.getName());
            json.startObject();
            ((INodeObject) role).dump(json, context);
            json.endObject();
        }

        if (jsonRoles)
            json.endObject();
    }

    @Override
    public String toString() {
        return getName() + "-" + "(" + getId() + "@" + getSpace().toString() + ")";
    }

    protected void updatePermissions() {
        SecurityService securityService = getTransaction().findDomainService(ISecurityService.NAME);
        if (securityService != null)
            securityService.setUpdatePermissions();
    }
}