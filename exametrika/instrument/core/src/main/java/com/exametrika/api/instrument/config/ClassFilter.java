/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ClassFilter} represents a class filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ClassFilter extends Configuration {
    private final ClassNameFilter classNameFilter;
    private final boolean includeSubClasses;
    private final List<ClassNameFilter> annotations;
    private final List<ClassFilter> includeClasses;
    private final List<ClassFilter> excludeClasses;

    /**
     * Creates a filter.
     *
     * @param classNameExpression class name expression. Class name expression has the following format:
     *                            #reg_exp_pattern | class_prefix_pattern* | exact _class_name. Where:
     *                            <li> reg_exp-pattern - any valid regular expression pattern
     *                            <li> class_name_prefix - class name pattern ending with '*' (foo.bar.*), that matches all class names starting with specified class name prefix
     *                            <li> exact_class_name - this filter matches if class name equals to exact_class_name pattern
     */
    public ClassFilter(String classNameExpression) {
        this(new ClassNameFilter(classNameExpression), false, null, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param classNameFilter   class name filter. Can be null if not used
     * @param includeSubclasses if true, subclasses are included in this filter
     * @param annotations       list of annotation filters. Can be null if not used
     */
    public ClassFilter(ClassNameFilter classNameFilter, boolean includeSubclasses, List<ClassNameFilter> annotations) {
        this(classNameFilter != null ? classNameFilter : new ClassNameFilter("*"), includeSubclasses, annotations, null, null);
    }

    /**
     * Creates a filter.
     *
     * @param classNameFilter   class name filter. Can be null if not used
     * @param includeSubclasses if true, subclasses are included in this filter
     * @param annotations       list of annotation filters. Can be null if not used
     * @param includeClasses    classes to include. Can be null if not used
     * @param excludeClasses    classes to exclude. Can be null if not used
     */
    public ClassFilter(ClassNameFilter classNameFilter, boolean includeSubclasses, List<ClassNameFilter> annotations, List<ClassFilter> includeClasses,
                       List<ClassFilter> excludeClasses) {
        this.classNameFilter = classNameFilter;
        this.includeSubClasses = includeSubclasses;
        this.annotations = Immutables.wrap(annotations);
        this.includeClasses = Immutables.wrap(includeClasses);
        this.excludeClasses = Immutables.wrap(excludeClasses);
    }

    public ClassNameFilter getClassNameFilter() {
        return classNameFilter;
    }

    public boolean isIncludeSubclasses() {
        return includeSubClasses;
    }

    public List<ClassNameFilter> getAnnotations() {
        return annotations;
    }

    public List<ClassFilter> getIncludeClasses() {
        return includeClasses;
    }

    public List<ClassFilter> getExcludeClasses() {
        return excludeClasses;
    }

    /**
     * Matches specified class name against this filter.
     *
     * @param className class name to match
     * @return true if class name does not match the filter, false if result of matching is unknown
     */
    public boolean notMatchClass(String className) {
        Assert.notNull(className);

        if (includeSubClasses)
            return false;

        boolean res = !matchClassName(className);

        if (res && includeClasses != null && !includeClasses.isEmpty()) {
            for (ClassFilter classFilter : includeClasses) {
                if (!classFilter.notMatchClass(className)) {
                    res = false;
                    break;
                }
            }
        }

        if (!res && excludeClasses != null && !excludeClasses.isEmpty()) {
            for (ClassFilter classFilter : excludeClasses) {
                if (classFilter.matchClass(className)) {
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified class name against this filter.
     *
     * @param className class name to match
     * @return false if class name matches the filter, false if result of matching is unknown
     */
    public boolean matchClass(String className) {
        Assert.notNull(className);

        if (annotations != null)
            return false;

        if (includeSubClasses)
            return false;

        boolean res = matchClassName(className);

        if (!res && includeClasses != null && !includeClasses.isEmpty()) {
            for (ClassFilter classFilter : includeClasses) {
                if (classFilter.matchClass(className)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeClasses != null && !excludeClasses.isEmpty()) {
            for (ClassFilter classFilter : excludeClasses) {
                if (!classFilter.notMatchClass(className)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified class name and supertypes against this filter.
     *
     * @param className   class name to match
     * @param superTypes  class superTypes to match
     * @param annotations class annotations to match
     * @return true if class matches the filter, false if class does not match the filter
     */
    public boolean matchClass(String className, Set<String> superTypes, Set<String> annotations) {
        Assert.notNull(className);
        Assert.notNull(superTypes);
        Assert.notNull(annotations);

        boolean res = (matchClassName(className) || matchSuperTypes(superTypes)) && matchAnnotations(annotations);

        if (!res && includeClasses != null && !includeClasses.isEmpty()) {
            for (ClassFilter classFilter : includeClasses) {
                if (classFilter.matchClass(className, superTypes, annotations)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeClasses != null && !excludeClasses.isEmpty()) {
            for (ClassFilter classFilter : excludeClasses) {
                if (classFilter.matchClass(className, superTypes, annotations)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified class against this filter.
     *
     * @param clazz class to match. Can be null
     * @return true if class matches the filter, false if class does not match the filter
     */
    public boolean matchClass(Class clazz) {
        if (clazz == null)
            return false;

        boolean res = matchClassName(clazz) && matchAnnotations(clazz);

        if (!res && includeClasses != null && !includeClasses.isEmpty()) {
            for (ClassFilter classFilter : includeClasses) {
                if (classFilter.matchClass(clazz)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeClasses != null && !excludeClasses.isEmpty()) {
            for (ClassFilter classFilter : excludeClasses) {
                if (classFilter.matchClass(clazz)) {
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
        if (!(o instanceof ClassFilter))
            return false;

        ClassFilter classFilter = (ClassFilter) o;
        return Objects.equals(classNameFilter, classFilter.classNameFilter) && Objects.equals(includeSubClasses, classFilter.includeSubClasses) &&
                Objects.equals(annotations, classFilter.annotations) && Objects.equals(includeClasses, classFilter.includeClasses) &&
                Objects.equals(excludeClasses, classFilter.excludeClasses);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(classNameFilter, includeSubClasses, annotations, includeClasses, excludeClasses);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (classNameFilter != null) {
            if (includeSubClasses)
                builder.append("[include subclasses] ");

            builder.append(classNameFilter);
            appended = true;
        }

        if (annotations != null) {
            if (appended)
                builder.append(", ");

            builder.append("annotations: ");
            builder.append(annotations);
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

    private boolean matchAnnotations(Class clazz) {
        if (annotations == null || annotations.isEmpty())
            return true;

        for (Annotation annotation : clazz.getAnnotations()) {
            if (matchAnnotation(annotation.annotationType().getName()))
                return true;
        }

        return false;
    }

    private boolean matchAnnotations(Set<String> annotations) {
        if (this.annotations == null || this.annotations.isEmpty())
            return true;

        for (String annotation : annotations) {
            if (matchAnnotation(annotation))
                return true;
        }

        return false;
    }

    private boolean matchAnnotation(String annotationName) {
        for (ClassNameFilter annotation : annotations) {
            if (annotation.matchClass(annotationName))
                return true;
        }

        return false;
    }

    private boolean matchClassName(Class clazz) {
        if (clazz == null)
            return false;

        if (matchClassName(clazz.getName()))
            return true;

        if (!includeSubClasses)
            return false;

        if (matchClass(clazz.getSuperclass()))
            return true;

        for (Class interfaceClass : clazz.getInterfaces()) {
            if (matchClass(interfaceClass))
                return true;
        }

        return false;
    }

    private boolean matchClassName(String className) {
        if (classNameFilter != null)
            return classNameFilter.matchClass(className);
        else
            return false;
    }

    private boolean matchSuperTypes(Set<String> superTypes) {
        if (!includeSubClasses)
            return false;

        for (String superType : superTypes) {
            if (matchClassName(superType))
                return true;
        }

        return false;
    }
}
