/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.List;

import com.exametrika.api.component.nodes.IBehaviorType;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.fields.ITagField;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.schema.BehaviorTypeNodeSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;


/**
 * The {@link BehaviorTypeNode} is a behavior type node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BehaviorTypeNode extends ObjectNodeObject implements IBehaviorType {
    private static final int NAME_FIELD = 1;
    private static final int METADATA_FIELD = 2;
    private static final int TAGS_FIELD = 3;

    public BehaviorTypeNode(INode node) {
        super(node);
    }

    @Override
    public BehaviorTypeNodeSchema getSchema() {
        return (BehaviorTypeNodeSchema) super.getSchema();
    }

    public final boolean isAccessAlowed() {
        return getSchema().getViewPermission().isAccessAllowed(this);
    }

    @Override
    public int getTypeId() {
        return (Integer) getKey();
    }

    @Override
    public String getName() {
        IStringField field = getField(NAME_FIELD);
        return field.get();
    }

    @Override
    public void setName(String value) {
        IPermission permission = getSchema().getEditPermission();
        permission.beginCheck(this);

        IStringField field = getField(NAME_FIELD);
        field.set(value);

        permission.endCheck();
    }

    @Override
    public JsonObject getMetadata() {
        IJsonField field = getField(METADATA_FIELD);
        return field.get();
    }

    @Override
    public void setMetadata(JsonObject value) {
        IPermission permission = getSchema().getEditPermission();
        permission.beginCheck(this);

        IJsonField field = getField(METADATA_FIELD);
        field.set(value);

        permission.endCheck();
    }

    @Override
    public List<String> getTags() {
        ITagField field = getField(TAGS_FIELD);
        return field.get();
    }

    @Override
    public void setTags(List<String> tags) {
        IPermission permission = getSchema().getEditPermission();
        permission.beginCheck(this);

        ITagField field = getField(TAGS_FIELD);
        field.set(tags);

        permission.endCheck();
    }

    @Override
    public void delete() {
        IPermission permission = getSchema().getDeletePermission();
        permission.beginCheck(this);

        super.delete();

        permission.endCheck();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("type");
        json.value("behaviorType");
        json.key("typeId");
        json.value(getTypeId());
        if (getName() != null) {
            json.key("name");
            json.value(getName());
        }

        JsonObject metadata = getMetadata();
        if (metadata != null) {
            json.key("metadata");
            JsonSerializers.write(json, metadata);
        }

        List<String> tags = getTags();
        if (tags != null) {
            json.key("tags");
            JsonSerializers.write(json, JsonUtils.toJson(tags));
        }
    }
}