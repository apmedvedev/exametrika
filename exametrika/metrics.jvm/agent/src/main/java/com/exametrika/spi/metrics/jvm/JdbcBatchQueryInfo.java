/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.exametrika.common.json.Json;


/**
 * The {@link JdbcBatchQueryInfo} represents an information about JDBC query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JdbcBatchQueryInfo extends JdbcQueryInfo {
    private final List<JdbcQueryInfo> batch = new ArrayList<JdbcQueryInfo>();

    @Override
    public String getText() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        String text = super.getText();
        if (!text.isEmpty()) {
            builder.append(text);
            first = false;
        }

        if (!batch.isEmpty()) {
            for (JdbcQueryInfo query : batch) {
                if (query.getText().isEmpty())
                    continue;

                if (first)
                    first = false;
                else
                    builder.append(';');

                builder.append(query.getText());
            }
        }

        return builder.toString();
    }

    @Override
    public String getTextWithParameters() {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        String text = super.getTextWithParameters();
        if (!text.isEmpty()) {
            builder.append(text);
            first = false;
        }

        if (!batch.isEmpty()) {
            for (JdbcQueryInfo query : batch) {
                if (first)
                    first = false;
                else
                    builder.append(';');

                builder.append(query.getTextWithParameters());
            }
        }

        return builder.toString();
    }

    @Override
    public String buildTextWithParameters(int parametersCount) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        String text = super.buildTextWithParameters(parametersCount);
        if (!text.isEmpty()) {
            builder.append(text);
            first = false;
        }

        if (!batch.isEmpty()) {
            for (JdbcQueryInfo query : batch) {
                if (first)
                    first = false;
                else
                    builder.append(';');

                builder.append(query.buildTextWithParameters(parametersCount));
            }
        }

        return builder.toString();
    }

    public List<JdbcQueryInfo> getBatch() {
        return batch;
    }

    public void addBatch(String queryText) {
        batch.add(new JdbcQueryInfo(queryText, new LinkedHashMap<String, JdbcParameterInfo>()));
    }

    public void addBatch() {
        batch.add(new JdbcQueryInfo("", new LinkedHashMap<String, JdbcParameterInfo>(parameters)));
        clearParameters();
    }

    public void clearBatch() {
        batch.clear();
    }

    @Override
    public void clear() {
        super.clear();
        clearBatch();
    }

    @Override
    public String toString() {
        return getTextWithParameters();
    }

    @Override
    protected void toJson(Json json) {
        super.toJson(json);

        if (!batch.isEmpty()) {
            json = json.putArray("batch");

            for (JdbcQueryInfo query : batch)
                json.add(query.toJson());

            json = json.end();
        }
    }
}
