/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link QualifiedMethodFilter} represents a qualified method filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class QualifiedMethodFilter extends Configuration {
    private final ClassFilter classFilter;
    private final MemberFilter methodFilter;
    private final List<QualifiedMethodFilter> includeMethods;
    private final List<QualifiedMethodFilter> excludeMethods;
    private final int minInstruction;
    private final int maxInstruction;

    /**
     * Creates a filter.
     *
     * @param classFilter  class name filter. Can be null if not used
     * @param methodFilter method name filter. Can be null if not used
     */
    public QualifiedMethodFilter(ClassFilter classFilter, MemberFilter methodFilter) {
        this(classFilter != null ? classFilter : new ClassFilter("*"), methodFilter != null ? methodFilter : new MemberFilter("*"),
                null, null, 0, Integer.MAX_VALUE);
    }

    /**
     * Creates a filter.
     *
     * @param includeMembers members to include. Can be null if not used
     * @param excludeMembers members to exclude. Can be null if not used
     */
    public QualifiedMethodFilter(List<QualifiedMethodFilter> includeMembers, List<QualifiedMethodFilter> excludeMembers) {
        this(null, null, includeMembers, excludeMembers, 0, Integer.MAX_VALUE);
    }

    /**
     * Creates a filter.
     *
     * @param classFilter    class name filter. Can be null if not used
     * @param methodFilter   method name filter. Can be null if not used
     * @param includeMethods methods to include. Can be null if not used
     * @param excludeMethods methods to exclude. Can be null if not used
     * @param minInstruction minimal instruction number
     * @param maxInstruction maximal instruction number
     */
    public QualifiedMethodFilter(ClassFilter classFilter, MemberFilter methodFilter,
                                 List<QualifiedMethodFilter> includeMethods, List<QualifiedMethodFilter> excludeMethods,
                                 int minInstruction, int maxInstruction) {
        this.classFilter = classFilter;
        this.methodFilter = methodFilter;
        this.includeMethods = Immutables.wrap(includeMethods);
        this.excludeMethods = Immutables.wrap(excludeMethods);
        this.minInstruction = minInstruction;
        this.maxInstruction = maxInstruction;
    }

    public ClassFilter getClassFilter() {
        return classFilter;
    }

    public MemberFilter getMethodFilter() {
        return methodFilter;
    }

    public List<QualifiedMethodFilter> getIncludeMethods() {
        return includeMethods;
    }

    public List<QualifiedMethodFilter> getExcludeMethods() {
        return excludeMethods;
    }

    public int getMinInstruction() {
        return minInstruction;

    }

    public int getMaxInstruction() {
        return maxInstruction;

    }

    /**
     * Matches specified class name against this filter.
     *
     * @param className class name to match
     * @return true if class name does not match the filter, false if result of matching is unknown
     */
    public boolean notMatchClass(String className) {
        Assert.notNull(className);

        boolean res = classFilter == null || classFilter.notMatchClass(className);

        if (res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (!methodFilter.notMatchClass(className)) {
                    res = false;
                    break;
                }
            }
        }

        if (!res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (methodFilter.matchClass(className)) {
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
     * @return true if class name matches the filter, false if result of matching is unknown
     */
    public boolean matchClass(String className) {
        Assert.notNull(className);

        if (methodFilter != null)
            return false;

        boolean res = classFilter != null && classFilter.matchClass(className);

        if (!res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (methodFilter.matchClass(className)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (!methodFilter.notMatchClass(className)) {
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
     * @param className   class name to match
     * @param superTypes  class superTypes to match
     * @param annotations class annotations to match
     * @return true if class does not match the filter, false if result of matching is unknown
     */
    public boolean notMatchClass(String className, Set<String> superTypes, Set<String> annotations) {
        Assert.notNull(className);
        Assert.notNull(superTypes);
        Assert.notNull(annotations);

        boolean res = !(classFilter != null && classFilter.matchClass(className, superTypes, annotations));

        if (res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (!methodFilter.notMatchClass(className, superTypes, annotations)) {
                    res = false;
                    break;
                }
            }
        }

        if (!res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (methodFilter.matchClass(className, superTypes, annotations)) {
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified class against this filter.
     *
     * @param className   class name to match
     * @param superTypes  class superTypes to match
     * @param annotations class annotations to match
     * @return true if class matches the filter, false if result of matching is unknown
     */
    public boolean matchClass(String className, Set<String> superTypes, Set<String> annotations) {
        Assert.notNull(className);
        Assert.notNull(superTypes);
        Assert.notNull(annotations);

        if (methodFilter != null)
            return false;

        boolean res = classFilter != null && classFilter.matchClass(className, superTypes, annotations);

        if (!res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (methodFilter.matchClass(className, superTypes, annotations)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (!methodFilter.notMatchClass(className, superTypes, annotations)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified method against this filter.
     *
     * @param className   class name to match
     * @param superTypes  class superTypes to match
     * @param annotations class annotations to match
     * @param methodName  method name to match
     * @return true if method does not match the filter, false if result of matching is unknown
     */
    public boolean notMatchMethod(String className, Set<String> superTypes, Set<String> annotations, String methodName) {
        Assert.notNull(className);
        Assert.notNull(superTypes);
        Assert.notNull(annotations);
        Assert.notNull(methodName);

        boolean res = !(classFilter != null && classFilter.matchClass(className, superTypes, annotations)) ||
                (methodFilter == null || methodFilter.notMatchMember(methodName));

        if (res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (!methodFilter.notMatchMethod(className, superTypes, annotations, methodName)) {
                    res = false;
                    break;
                }
            }
        }

        if (!res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (methodFilter.matchMethod(className, superTypes, annotations, methodName)) {
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified method against this filter.
     *
     * @param className   class name to match
     * @param superTypes  class superTypes to match
     * @param annotations class annotations to match
     * @param methodName  method name to match
     * @return true if method matches the filter, false if result of matching is unknown
     */
    public boolean matchMethod(String className, Set<String> superTypes, Set<String> annotations, String methodName) {
        Assert.notNull(className);
        Assert.notNull(superTypes);
        Assert.notNull(annotations);
        Assert.notNull(methodName);

        boolean res = (classFilter != null && classFilter.matchClass(className, superTypes, annotations)) &&
                (methodFilter != null && methodFilter.matchMember(methodName));

        if (!res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : includeMethods) {
                if (methodFilter.matchMethod(className, superTypes, annotations, methodName)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter methodFilter : excludeMethods) {
                if (!methodFilter.notMatchMethod(className, superTypes, annotations, methodName)) {
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

        for (Method method : clazz.getDeclaredMethods()) {
            if (matchMethod(method))
                return true;
        }

        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            if (matchMethod(constructor))
                return true;
        }

        return false;
    }


    /**
     * Matches specified method against this filter.
     *
     * @param method method to match. Can be null
     * @return true if method matches the filter, false if method does not match the filter
     */
    public boolean matchMethod(Member method) {
        if (method == null)
            return false;

        boolean res = (classFilter != null && classFilter.matchClass(method.getDeclaringClass())) &&
                (methodFilter != null && methodFilter.matchMember(method));

        if (!res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter memberFilter : includeMethods) {
                if (memberFilter.matchMethod(method)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter memberFilter : excludeMethods) {
                if (memberFilter.matchMethod(method)) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Matches specified method against this filter.
     *
     * @param className         class name to match
     * @param superTypes        class superTypes to match
     * @param classAnnotations  class annotations to match
     * @param methodName        method name to match
     * @param methodAnnotations method annotations to match
     * @return true if method matches the filter, false if method does not match the filter
     */
    public boolean matchMethod(String className, Set<String> superTypes, Set<String> classAnnotations,
                               String methodName, Set<String> methodAnnotations) {
        boolean res = (classFilter != null && classFilter.matchClass(className, superTypes, classAnnotations)) &&
                (methodFilter != null && methodFilter.matchMember(methodName, methodAnnotations));

        if (!res && includeMethods != null && !includeMethods.isEmpty()) {
            for (QualifiedMethodFilter memberFilter : includeMethods) {
                if (memberFilter.matchMethod(className, superTypes, classAnnotations, methodName, methodAnnotations)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMethods != null && !excludeMethods.isEmpty()) {
            for (QualifiedMethodFilter memberFilter : excludeMethods) {
                if (memberFilter.matchMethod(className, superTypes, classAnnotations, methodName, methodAnnotations)) {
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
        if (!(o instanceof QualifiedMethodFilter))
            return false;

        QualifiedMethodFilter methodFilter = (QualifiedMethodFilter) o;
        return Objects.equals(classFilter, methodFilter.classFilter) &&
                Objects.equals(this.methodFilter, methodFilter.methodFilter) &&
                Objects.equals(includeMethods, methodFilter.includeMethods) &&
                Objects.equals(excludeMethods, methodFilter.excludeMethods) &&
                minInstruction == methodFilter.minInstruction &&
                maxInstruction == methodFilter.maxInstruction;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(classFilter, methodFilter, includeMethods, excludeMethods, minInstruction, maxInstruction);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (classFilter != null) {
            builder.append(classFilter);
            appended = true;
        }

        if (methodFilter != null) {
            if (appended)
                builder.append(", ");

            builder.append(methodFilter);
            appended = true;
        }

        if (includeMethods != null) {
            if (appended)
                builder.append(", ");

            builder.append("include: ");
            builder.append(includeMethods);
            appended = true;
        }

        if (excludeMethods != null) {
            if (appended)
                builder.append(", ");

            builder.append("exclude: ");
            builder.append(excludeMethods);
            appended = true;
        }

        if (minInstruction > 0 || maxInstruction != Integer.MAX_VALUE) {
            builder.append("[");
            builder.append(minInstruction);
            builder.append("-");
            if (maxInstruction != Integer.MAX_VALUE) {
                builder.append(maxInstruction);
                builder.append("]");
            } else
                builder.append(")");
        }

        return builder.toString();
    }
}
