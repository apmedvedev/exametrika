/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;


/**
 * The {@link Blobs} is a blob utilities.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class Blobs {
    public static long getPosition(int pageSize, long pageIndex, int pageOffset) {
        return pageSize * pageIndex + pageOffset;
    }

    public static long getPageIndex(int pageSize, long position) {
        return position / pageSize;
    }

    public static int getPageOffset(int pageSize, long position) {
        return (int) (position % pageSize);
    }

    private Blobs() {
    }
}
