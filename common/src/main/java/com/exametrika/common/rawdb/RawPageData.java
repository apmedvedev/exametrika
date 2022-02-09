/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.rawdb;


/**
 * The {@link RawPageData} is an implementation of {@link IRawPageData}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class RawPageData implements IRawPageData {
    @Override
    public void onBeforeCommitted() {
    }

    @Override
    public void onCommitted() {
    }

    @Override
    public void onRolledBack() {
    }

    @Override
    public void onUnloaded() {
    }
}
