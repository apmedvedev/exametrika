/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link JdbcQueryInfo} represents an information about JDBC query which is part of batch.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JdbcQueryInfo {
    protected String queryText;
    protected final Map<String, JdbcParameterInfo> parameters;

    public JdbcQueryInfo() {
        this("", new LinkedHashMap<String, JdbcParameterInfo>());
    }

    public JdbcQueryInfo(String queryText, Map<String, JdbcParameterInfo> parameters) {
        Assert.notNull(queryText);
        Assert.notNull(parameters);

        this.queryText = queryText;
        this.parameters = parameters;
    }

    public String getText() {
        return queryText;
    }

    public String getTextWithParameters() {
        if (parameters.isEmpty())
            return queryText;
        else if (queryText.isEmpty())
            return "(" + getParametersText() + ")";
        else
            return queryText + " (" + getParametersText() + ")";
    }

    public String getParametersText() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (JdbcParameterInfo parameter : parameters.values()) {
            if (first)
                first = false;
            else
                builder.append(", ");

            builder.append(parameter.toString());
        }

        return builder.toString();
    }

    public String buildTextWithParameters(int parametersCount) {
        if (parameters.isEmpty())
            return queryText;
        else if (queryText.isEmpty())
            return "(" + buildParametersText(parametersCount) + ")";
        else
            return queryText + " (" + buildParametersText(parametersCount) + ")";
    }

    public String buildParametersText(int parametersCount) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (JdbcParameterInfo parameter : parameters.values()) {
            if (first)
                first = false;
            else
                builder.append(", ");

            if (parametersCount > 0)
                builder.append(parameter.toString());
            else
                builder.append('?');
            parametersCount--;
        }

        return builder.toString();
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        Assert.notNull(queryText);

        this.queryText = queryText;
    }

    public Collection<JdbcParameterInfo> getParameters() {
        return parameters.values();
    }

    public JdbcParameterInfo getParameter(int index) {
        return getParameter(Integer.toString(index));
    }

    public JdbcParameterInfo getParameter(String name) {
        return parameters.get(name);
    }

    public void setParameter(int index, Object value) {
        setParameter(Integer.toString(index), value);
    }

    public void setParameter(String name, Object value) {
        Assert.notNull(name);

        parameters.put(name, new JdbcParameterInfo(name, value));
    }

    public void clearParameters() {
        parameters.clear();
    }

    public void clear() {
        clearParameters();
        queryText = "";
    }

    public JsonObject toJson() {
        Json json = Json.object();
        toJson(json);
        return json.toObject();
    }

    @Override
    public String toString() {
        return getTextWithParameters();
    }

    protected void toJson(Json json) {
        if (!queryText.isEmpty())
            json.put("query", queryText);

        if (!parameters.isEmpty()) {
            json = json.putObject("parameters");

            for (JdbcParameterInfo parameter : parameters.values())
                json.put(parameter.getName(), parameter.getValue());

            json = json.end();
        }
    }
}
