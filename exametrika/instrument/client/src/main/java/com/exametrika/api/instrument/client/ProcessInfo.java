/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.client;


/**
 * The {@link ProcessInfo} represents an information about process that can be instrumented.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ProcessInfo {
    private final String id;
    private final String description;

    /**
     * Creates an object.
     *
     * @param id          process identifier
     * @param description process description
     */
    public ProcessInfo(String id, String description) {
        if (id == null || description == null)
            throw new IllegalArgumentException();

        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return id + " " + description;
    }
}
