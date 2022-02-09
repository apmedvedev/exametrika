/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSpaceSchemaConfiguration;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.core.Spaces;


/**
 * The {@link PeriodSpaces} contains various spaces utils.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSpaces {
    public static String getCycleSpaceFileName(String prefix, int fileIndex) {
        return prefix + "/data-" + fileIndex + ".cy";
    }

    public static String getForecastSpaceFileName(String prefix, int fileIndex) {
        return prefix + "-" + fileIndex + ".fc";
    }

    public static String getForecastSpacePrefix(String domainName, PeriodSpaceSchemaConfiguration spaceConfiguration,
                                                PeriodSchemaConfiguration periodConfiguration) {
        return "forecast/" + domainName + "-" + spaceConfiguration.getName() + "-" + periodConfiguration.getName();
    }

    public static String getPeriodSpacePrefix(String domainName, PeriodSpaceSchemaConfiguration spaceConfiguration,
                                              PeriodSchemaConfiguration periodConfiguration, int fileIndex) {
        return domainName + "-" + spaceConfiguration.getName() + "-" + periodConfiguration.getName() + "-" + fileIndex;
    }

    public static List<String> getPeriodSpaceFileNames(List<String> paths, int pathIndex, String domainName,
                                                       PeriodSpaceSchemaConfiguration spaceConfiguration,
                                                       PeriodSchemaConfiguration periodConfiguration, int dataFileIndex, int cycleSpaceFileIndex, int forecastSpaceFileIndex,
                                                       int anomalyDetectorSpaceFileIndex, int fastAnomalyDetectorSpaceFileIndex) {
        List<String> files = new ArrayList<String>();

        String prefix = getPeriodSpacePrefix(domainName, spaceConfiguration, periodConfiguration, dataFileIndex);

        files.add(Spaces.getSpaceDataFileName(new File(paths.get(pathIndex), prefix).getPath(), dataFileIndex));
        files.add(getCycleSpaceFileName(new File(paths.get(pathIndex), prefix).getPath(), cycleSpaceFileIndex));

        if (forecastSpaceFileIndex != 0) {
            String forecastPrefix = getForecastSpacePrefix(domainName, spaceConfiguration, periodConfiguration);
            files.add(getForecastSpaceFileName(new File(paths.get(pathIndex), forecastPrefix).getPath(), forecastSpaceFileIndex));
            files.add(getForecastSpaceFileName(new File(paths.get(pathIndex), forecastPrefix).getPath(), anomalyDetectorSpaceFileIndex));
            files.add(getForecastSpaceFileName(new File(paths.get(pathIndex), forecastPrefix).getPath(), fastAnomalyDetectorSpaceFileIndex));
        }

        for (String path : paths) {
            files.add(Spaces.getSpaceFilesDirName(new File(path, prefix).getPath()));
            files.add(Spaces.getSpaceIndexesDirName(new File(path, prefix).getPath()));
        }

        return files;
    }

    public static IKeyNormalizer<Location> createLocationKeyNormalizer() {
        return new LocationKeyNormalizer();
    }

    private PeriodSpaces() {
    }

    private static class LocationKeyNormalizer implements IKeyNormalizer<Location> {
        @Override
        public ByteArray normalize(Location key) {
            byte[] buffer = new byte[16];
            Indexes.normalizeLong(buffer, 0, key.getScopeId());
            Indexes.normalizeLong(buffer, 8, key.getMetricId());
            return new ByteArray(buffer);
        }
    }
}
