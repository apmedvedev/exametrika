/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link Spaces} contains various spaces utils.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Spaces {
    private static final ILogger logger = Loggers.get(Spaces.class);

    public static final String SCHEMA_SPACE_FILE_NAME = "schema-0.dt";
    public static final int SCHEMA_SPACE_FILE_INDEX = 0;
    public static final int RESERVED_FILE_COUNT = 10;

    public static List<String> getSystemFiles(String path) {
        return Arrays.asList(new File(path, SCHEMA_SPACE_FILE_NAME).getPath());
    }

    public static void bindSystemFiles(IDatabaseContext context, IRawTransaction transaction) {
        if (transaction.isFileBound(SCHEMA_SPACE_FILE_INDEX))
            return;

        RawBindInfo bindInfo = new RawBindInfo();

        bindInfo.setName(SCHEMA_SPACE_FILE_NAME);
        bindInfo.setFlags(RawBindInfo.PRELOAD);
        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.system.schema")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(SCHEMA_SPACE_FILE_INDEX, bindInfo);
    }

    public static String getSpaceDataFileName(String prefix, int fileIndex) {
        return prefix + "/data-" + fileIndex + ".dt";
    }

    public static String getSpaceFilesDirName(String prefix) {
        return prefix + "/files";
    }

    public static String getSpaceIndexesDirName(String prefix) {
        return prefix + "/indexes";
    }

    public static String getSpaceIndexFileName(String prefix, int indexFileIndex) {
        return prefix + "-" + indexFileIndex + ".ix";
    }

    public static String getSpaceFileName(String spacesFilePath, int fileIndex) {
        return spacesFilePath + "/db-" + fileIndex + ".dt";
    }

    public static void getSpaceFiles(File directory, String baseDir, List<Pair<String, Integer>> files) {
        if (!directory.isDirectory())
            return;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith("fulltext"))
                    getSpaceFiles(file, baseDir, files);
            } else {
                String fileName = file.getName();
                int pos = fileName.lastIndexOf('-');
                int pos2 = fileName.lastIndexOf('.');
                if (pos2 == -1)
                    pos2 = fileName.length();
                try {
                    int fileIndex = Integer.valueOf(fileName.substring(pos + 1, pos2));
                    files.add(new Pair<String, Integer>(file.getPath().substring(baseDir.length() + 1), fileIndex));
                } catch (Exception e) {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                }
            }
        }
    }

    public static void getSpaceFiles(File directory, List<String> files) {
        if (!directory.isDirectory())
            return;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                if (!file.getName().startsWith("fulltext"))
                    getSpaceFiles(file, files);
            } else
                files.add(file.getPath());
        }
    }

    private Spaces() {
    }
}
