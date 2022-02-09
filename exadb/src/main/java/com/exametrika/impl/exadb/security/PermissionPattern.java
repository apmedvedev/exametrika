/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link Permission} is a permission pattern.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class PermissionPattern {
    private final String name;
    private final List<LevelPattern> patterns;

    public static PermissionPattern parse(String permissionPattern) {
        List<LevelPattern> levelPatterns = new ArrayList<LevelPattern>();
        String[] levelParts = permissionPattern.split("[:]");
        for (int i = 0; i < levelParts.length; i++) {
            List<Pattern> patterns = new ArrayList<Pattern>();
            String levelPart = levelParts[i];
            String[] parts = levelPart.split("[,]");
            for (int k = 0; k < parts.length; k++) {
                Pattern pattern = Strings.createFilterPattern(parts[k].trim(), true);
                patterns.add(pattern);
            }

            levelPatterns.add(new LevelPattern(patterns));
        }

        return new PermissionPattern(permissionPattern, levelPatterns);
    }

    public boolean match(List<String> levels) {
        if (patterns != null) {
            if (levels.size() < patterns.size())
                return false;

            for (int i = 0; i < patterns.size(); i++) {
                LevelPattern pattern = patterns.get(i);
                String value = levels.get(i);
                if (!pattern.match(value))
                    return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    private PermissionPattern(String name, List<LevelPattern> patterns) {
        Assert.notNull(name);
        Assert.notNull(patterns);

        this.name = name;
        this.patterns = patterns;
    }

    private static class LevelPattern {
        private final List<Pattern> patterns;

        private LevelPattern(List<Pattern> patterns) {
            Assert.notNull(patterns);

            this.patterns = patterns;
        }

        public boolean match(String value) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(value).matches())
                    return true;
            }

            return false;
        }
    }
}