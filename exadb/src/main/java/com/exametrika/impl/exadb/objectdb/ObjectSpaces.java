/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.impl.exadb.core.Spaces;


/**
 * The {@link ObjectSpaces} contains various spaces utils.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSpaces {
    public static String getObjectSpacePrefix(String domainName, ObjectSpaceSchemaConfiguration configuration, int fileIndex) {
        return domainName + "-" + configuration.getName() + "-" + fileIndex;
    }

    public static List<String> getObjectSpaceFileNames(List<String> paths, int pathIndex, String domainName, ObjectSpaceSchemaConfiguration configuration, int fileIndex) {
        List<String> files = new ArrayList<String>();

        String prefix = getObjectSpacePrefix(domainName, configuration, fileIndex);

        files.add(Spaces.getSpaceDataFileName(new File(paths.get(pathIndex), prefix).getPath(), fileIndex));

        for (String path : paths) {
            files.add(Spaces.getSpaceFilesDirName(new File(path, prefix).getPath()));
            files.add(Spaces.getSpaceIndexesDirName(new File(path, prefix).getPath()));
        }

        return files;
    }

    private ObjectSpaces() {
    }
}
