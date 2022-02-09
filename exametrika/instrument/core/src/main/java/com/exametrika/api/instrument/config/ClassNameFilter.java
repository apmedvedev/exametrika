/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link ClassNameFilter} represents a class name filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ClassNameFilter extends Configuration {
    private final String classNameExpression;
    private final Pattern pattern;
    private final List<ClassNameFilter> includeClasses;
    private final List<ClassNameFilter> excludeClasses;

    /**
     * Creates a filter.
     *
     * @param classNameExpression class name expression. Class name expression has the following format:
     *                            #reg_exp_pattern | class_prefix_pattern* | exact _class_name. Where:
     *                            <li> reg_exp-pattern - any valid regular expression pattern
     *                            <li> class_name_prefix - class name pattern ending with '*' (foo.bar.*), that matches all class names starting with specified class name prefix
     *                            <li> exact_class_name - this filter matches if class name equals to exact_class_name pattern
     */
    public ClassNameFilter(String classNameExpression) {
        this(classNameExpression != null ? classNameExpression : "*", null, null);
    }

    /**
     * Creates a filter.
     *
     * @param clazz class whose name this filter have to match against
     */
    public ClassNameFilter(Class clazz) {
        this(clazz.getName(), null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeClasses classes to include. Can be null if not used
     * @param excludeClasses classes to exclude. Can be null if not used
     */
    public ClassNameFilter(List<String> includeClasses, List<String> excludeClasses) {
        this(null, buildFilter(includeClasses), buildFilter(excludeClasses));
    }

    /**
     * Creates a filter.
     *
     * @param classNameExpression class name expression. Can be null if not used. Class name expression has the following format:
     *                            #reg_exp_pattern | class_prefix_pattern* | exact _class_name. Where:
     *                            <li> reg_exp-pattern - any valid regular expression pattern
     *                            <li> class_name_prefix - class name pattern ending with '*' (foo.bar.*), that matches all class names starting with specified class name prefix
     *                            <li> exact_class_name - this filter matches if class name equals to exact_class_name pattern
     * @param includeClasses      classes to include. Can be null if not used
     * @param excludeClasses      classes to exclude. Can be null if not used
     */
    public ClassNameFilter(String classNameExpression, List<ClassNameFilter> includeClasses, List<ClassNameFilter> excludeClasses) {
        this.classNameExpression = classNameExpression;

        if (classNameExpression != null)
            pattern = Strings.createFilterPattern(classNameExpression, true);
        else
            pattern = null;

        this.includeClasses = Immutables.wrap(includeClasses);
        this.excludeClasses = Immutables.wrap(excludeClasses);
    }

    public String getClassNameExpression() {
        return classNameExpression;
    }

    public List<ClassNameFilter> getIncludeClasses() {
        return includeClasses;
    }

    public List<ClassNameFilter> getExcludeClasses() {
        return excludeClasses;
    }

    /**
     * Matches specified class name against this filter.
     *
     * @param className class name to match
     * @return true if class name matches the filter
     */
    public boolean matchClass(String className) {
        Assert.notNull(className);

        boolean res = matchClassName(className);

        if (!res && includeClasses != null && !includeClasses.isEmpty()) {
            for (ClassNameFilter classFilter : includeClasses) {
                if (classFilter.matchClass(className)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeClasses != null && !excludeClasses.isEmpty()) {
            for (ClassNameFilter classFilter : excludeClasses) {
                if (classFilter.matchClass(className)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ClassNameFilter))
            return false;

        ClassNameFilter classFilter = (ClassNameFilter) o;
        return Objects.equals(classNameExpression, classFilter.classNameExpression) && Objects.equals(includeClasses, classFilter.includeClasses) &&
                Objects.equals(excludeClasses, classFilter.excludeClasses);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(classNameExpression, includeClasses, excludeClasses);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (classNameExpression != null) {
            builder.append("class: ");
            builder.append(classNameExpression);
            appended = true;
        }

        if (includeClasses != null) {
            if (appended)
                builder.append(", ");

            builder.append("include: ");
            builder.append(includeClasses);
            appended = true;
        }

        if (excludeClasses != null) {
            if (appended)
                builder.append(", ");

            builder.append("exclude: ");
            builder.append(excludeClasses);
            appended = true;
        }

        return builder.toString();
    }

    private boolean matchClassName(String className) {
        if (pattern != null)
            return pattern.matcher(className).matches();
        else
            return false;
    }

    private static List<ClassNameFilter> buildFilter(List<String> classes) {
        if (classes == null)
            return null;

        List<ClassNameFilter> filter = new ArrayList<ClassNameFilter>(classes.size());
        for (String clazz : classes)
            filter.add(new ClassNameFilter(clazz));

        return filter;
    }
}
