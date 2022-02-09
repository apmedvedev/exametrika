/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument.config;

import java.util.List;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link QualifiedMemberNameFilter} represents a qualified member (method or field) name filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class QualifiedMemberNameFilter extends Configuration {
    private final ClassNameFilter classNameFilter;
    private final MemberNameFilter memberNameFilter;
    private final List<QualifiedMemberNameFilter> includeMembers;
    private final List<QualifiedMemberNameFilter> excludeMembers;

    /**
     * Creates a filter.
     *
     * @param classNameExpression  class name expression. Can be null if not used. Class name expression has the following format:
     *                             #reg_exp_pattern | class_prefix_pattern* | exact _class_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> class_name_prefix - class name pattern ending with '*' (foo.bar.*), that matches all class names starting with specified class name prefix
     *                             <li> exact_class_name - this filter matches if class name equals to exact_class_name pattern
     * @param memberNameExpression member name expression. Can be null if not used. Member name expression has the following format:
     *                             #reg_exp_pattern | exact _member_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> exact_member_name - this filter matches if member name equals to exact_member_name pattern
     */
    public QualifiedMemberNameFilter(String classNameExpression, String memberNameExpression) {
        this(new ClassNameFilter(classNameExpression), new MemberNameFilter(memberNameExpression));
    }

    /**
     * Creates a filter.
     *
     * @param classNameExpression  class name expression. Can be null if not used. Class name expression has the following format:
     *                             #reg_exp_pattern | class_prefix_pattern* | exact _class_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> class_name_prefix - class name pattern ending with '*' (foo.bar.*), that matches all class names starting with specified class name prefix
     *                             <li> exact_class_name - this filter matches if class name equals to exact_class_name pattern
     * @param memberNameExpression member name expression. Can be null if not used. Member name expression has the following format:
     *                             #reg_exp_pattern | exact _member_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> exact_member_name - this filter matches if member name equals to exact_member_name pattern
     * @param includeMembers       members to include. Can be null if not used
     * @param excludeMembers       members to exclude. Can be null if not used
     */
    public QualifiedMemberNameFilter(String classNameExpression, String memberNameExpression,
                                     List<QualifiedMemberNameFilter> includeMembers, List<QualifiedMemberNameFilter> excludeMembers) {
        this(classNameExpression != null ? new ClassNameFilter(classNameExpression) : null,
                memberNameExpression != null ? new MemberNameFilter(memberNameExpression) : null,
                includeMembers, excludeMembers);
    }

    /**
     * Creates a filter.
     *
     * @param classNameFilter  class name filter. Can be null if not used
     * @param memberNameFilter member name filter. Can be null if not used
     */
    public QualifiedMemberNameFilter(ClassNameFilter classNameFilter, MemberNameFilter memberNameFilter) {
        this(classNameFilter != null ? classNameFilter : new ClassNameFilter("*"),
                memberNameFilter != null ? memberNameFilter : new MemberNameFilter("*"), null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeMembers members to include. Can be null if not used
     * @param excludeMembers members to exclude. Can be null if not used
     */
    public QualifiedMemberNameFilter(List<QualifiedMemberNameFilter> includeMembers, List<QualifiedMemberNameFilter> excludeMembers) {
        this((ClassNameFilter) null, null, includeMembers, excludeMembers);
    }

    /**
     * Creates a filter.
     *
     * @param classNameFilter  class name filter. Can be null if not used
     * @param memberNameFilter member name filter. Can be null if not used
     * @param includeMembers   members to include. Can be null if not used
     * @param excludeMembers   members to exclude. Can be null if not used
     */
    public QualifiedMemberNameFilter(ClassNameFilter classNameFilter, MemberNameFilter memberNameFilter,
                                     List<QualifiedMemberNameFilter> includeMembers, List<QualifiedMemberNameFilter> excludeMembers) {
        this.classNameFilter = classNameFilter;
        this.memberNameFilter = memberNameFilter;
        this.includeMembers = Immutables.wrap(includeMembers);
        this.excludeMembers = Immutables.wrap(excludeMembers);
    }

    public ClassNameFilter getClassNameFilter() {
        return classNameFilter;
    }

    public MemberNameFilter getMemberNameFilter() {
        return memberNameFilter;
    }

    public List<QualifiedMemberNameFilter> getIncludeMembers() {
        return includeMembers;
    }

    public List<QualifiedMemberNameFilter> getExcludeMembers() {
        return excludeMembers;
    }

    /**
     * Matches specified qialified member name against this filter.
     *
     * @param className  class name to match
     * @param memberName member name to match
     * @return true if qualified member name matches the filter, false if qualified member name does not match the filter
     */
    public boolean matchMember(String className, String memberName) {
        Assert.notNull(className);
        Assert.notNull(memberName);

        boolean res = matchClassName(className) && matchMemberName(memberName);

        if (!res && includeMembers != null && !includeMembers.isEmpty()) {
            for (QualifiedMemberNameFilter memberFilter : includeMembers) {
                if (memberFilter.matchMember(className, memberName)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (QualifiedMemberNameFilter memberFilter : excludeMembers) {
                if (memberFilter.matchMember(className, memberName)) {
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
        if (!(o instanceof QualifiedMemberNameFilter))
            return false;

        QualifiedMemberNameFilter memberFilter = (QualifiedMemberNameFilter) o;
        return Objects.equals(classNameFilter, memberFilter.classNameFilter) &&
                Objects.equals(memberNameFilter, memberFilter.memberNameFilter) &&
                Objects.equals(includeMembers, memberFilter.includeMembers) &&
                Objects.equals(excludeMembers, memberFilter.excludeMembers);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(classNameFilter, memberNameFilter, includeMembers, excludeMembers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (classNameFilter != null) {
            builder.append(classNameFilter);
            appended = true;
        }

        if (memberNameFilter != null) {
            if (appended)
                builder.append(", ");

            builder.append(memberNameFilter);
            appended = true;
        }

        if (includeMembers != null) {
            if (appended)
                builder.append(", ");

            builder.append("include: ");
            builder.append(includeMembers);
            appended = true;
        }

        if (excludeMembers != null) {
            if (appended)
                builder.append(", ");

            builder.append("exclude: ");
            builder.append(excludeMembers);
            appended = true;
        }

        return builder.toString();
    }

    private boolean matchClassName(String className) {
        if (classNameFilter != null)
            return classNameFilter.matchClass(className);
        else
            return false;
    }

    private boolean matchMemberName(String memberName) {
        if (memberNameFilter != null)
            return memberNameFilter.matchMember(memberName);
        else
            return false;
    }
}
