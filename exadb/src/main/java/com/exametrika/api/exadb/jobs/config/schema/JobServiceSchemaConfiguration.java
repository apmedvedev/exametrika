/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.jobs.config.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.jobs.JobService;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;

/**
 * The {@link JobServiceSchemaConfiguration} represents a configuration of job service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JobServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.jobs-1.0";
    public static final String NAME = "JobService";

    private Map<String, JobSchemaConfiguration> predefinedJobs;
    private boolean freezed;

    public JobServiceSchemaConfiguration() {
        this(Collections.<String, JobSchemaConfiguration>emptyMap());
    }

    public JobServiceSchemaConfiguration(Map<String, ? extends JobSchemaConfiguration> predefinedJobs) {
        this(NAME, null, predefinedJobs);
    }

    public JobServiceSchemaConfiguration(String alias, String description,
                                         Map<String, ? extends JobSchemaConfiguration> predefinedJobs) {
        this(alias, description, predefinedJobs, true);
    }

    public JobServiceSchemaConfiguration(String alias, String description,
                                         Map<String, ? extends JobSchemaConfiguration> predefinedJobs, boolean freezed) {
        super(NAME, alias, description);

        Assert.notNull(predefinedJobs);

        if (freezed)
            this.predefinedJobs = Immutables.wrap(predefinedJobs);
        else
            this.predefinedJobs = new LinkedHashMap<String, JobSchemaConfiguration>(predefinedJobs);
    }

    public Map<String, JobSchemaConfiguration> getPredefinedJobs() {
        return predefinedJobs;
    }

    public void addPredefinedJob(String name, JobSchemaConfiguration predefinedJob) {
        Assert.notNull(predefinedJob);
        Assert.checkState(!freezed);
        Assert.isTrue(!predefinedJobs.containsKey(name));

        predefinedJobs.put(name, predefinedJob);
    }

    @Override
    public void freeze() {
        if (freezed)
            return;

        freezed = true;
        predefinedJobs = Immutables.wrap(predefinedJobs);
    }

    @Override
    public IDomainService createService() {
        return new JobService();
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        Map<String, JobSchemaConfiguration> predefinedJobs = new LinkedHashMap<String, JobSchemaConfiguration>(this.predefinedJobs);
        for (Map.Entry<String, JobSchemaConfiguration> entry : ((JobServiceSchemaConfiguration) schema).getPredefinedJobs().entrySet()) {
            Assert.isTrue(!predefinedJobs.containsKey(entry.getKey()));
            predefinedJobs.put(entry.getKey(), entry.getValue());
        }

        return (T) new JobServiceSchemaConfiguration(combine(getAlias(), schema.getAlias()),
                combine(getDescription(), schema.getDescription()), predefinedJobs);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JobServiceSchemaConfiguration))
            return false;

        JobServiceSchemaConfiguration configuration = (JobServiceSchemaConfiguration) o;
        return super.equals(o) && predefinedJobs.equals(configuration.predefinedJobs);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(predefinedJobs);
    }
}