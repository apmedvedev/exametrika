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
 * The {@link MemberNameFilter} represents a member (method or field) name filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MemberNameFilter extends Configuration {
    private final String memberNameExpression;
    private final Pattern pattern;
    private final List<MemberNameFilter> includeMembers;
    private final List<MemberNameFilter> excludeMembers;

    /**
     * Creates a filter.
     *
     * @param memberNameExpression member name expression. Member name expression has the following format:
     *                             #reg_exp_pattern | exact _member_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> exact_member_name - this filter matches if member name equals to exact_member_name pattern
     */
    public MemberNameFilter(String memberNameExpression) {
        this(memberNameExpression != null ? memberNameExpression : "*", null, null);
    }

    /**
     * Creates a filter.
     *
     * @param includeMembers members to include. Can be null if not used
     * @param excludeMembers members to exclude. Can be null if not used
     */
    public MemberNameFilter(List<String> includeMembers, List<String> excludeMembers) {
        this(null, buildFilter(includeMembers), buildFilter(excludeMembers));
    }

    /**
     * Creates a filter.
     *
     * @param memberNameExpression member name expression. Can be null if not used. Member name expression has the following format:
     *                             #reg_exp_pattern | exact _member_name. Where:
     *                             <li> reg_exp-pattern - any valid regular expression pattern
     *                             <li> exact_member_name - this filter matches if member name equals to exact_member_name pattern
     * @param includeMembers       members to include. Can be null if not used
     * @param excludeMembers       members to exclude. Can be null if not used
     */
    public MemberNameFilter(String memberNameExpression, List<MemberNameFilter> includeMembers, List<MemberNameFilter> excludeMembers) {
        this.memberNameExpression = memberNameExpression;

        if (memberNameExpression != null)
            pattern = Strings.createFilterPattern(memberNameExpression, true);
        else
            pattern = null;

        this.includeMembers = Immutables.wrap(includeMembers);
        this.excludeMembers = Immutables.wrap(excludeMembers);
    }

    public String getMemberNameExpression() {
        return memberNameExpression;
    }

    public List<MemberNameFilter> getIncludeMembers() {
        return includeMembers;
    }

    public List<MemberNameFilter> getExcludeMembers() {
        return excludeMembers;
    }

    /**
     * Matches specified member name against this filter.
     *
     * @param memberName member name to match
     * @return true if member name matches the filter
     */
    public boolean matchMember(String memberName) {
        Assert.notNull(memberName);

        boolean res = matchMemberName(memberName);

        if (!res && includeMembers != null && !includeMembers.isEmpty()) {
            for (MemberNameFilter memberFilter : includeMembers) {
                if (memberFilter.matchMember(memberName)) {
                    res = true;
                    break;
                }
            }
        }

        if (res && excludeMembers != null && !excludeMembers.isEmpty()) {
            for (MemberNameFilter memberFilter : excludeMembers) {
                if (memberFilter.matchMember(memberName)) {
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
        if (!(o instanceof MemberNameFilter))
            return false;

        MemberNameFilter memberFilter = (MemberNameFilter) o;
        return Objects.equals(memberNameExpression, memberFilter.memberNameExpression) && Objects.equals(includeMembers, memberFilter.includeMembers) &&
                Objects.equals(excludeMembers, memberFilter.excludeMembers);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(memberNameExpression, includeMembers, excludeMembers);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        boolean appended = false;

        if (memberNameExpression != null) {
            builder.append("member: ");
            builder.append(memberNameExpression);
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

    private boolean matchMemberName(String memberName) {
        if (pattern != null)
            return pattern.matcher(memberName).matches();
        else
            return false;
    }

    private static List<MemberNameFilter> buildFilter(List<String> members) {
        if (members == null)
            return null;

        List<MemberNameFilter> filter = new ArrayList<MemberNameFilter>(members.size());
        for (String member : members)
            filter.add(new MemberNameFilter(member));

        return filter;
    }
}
