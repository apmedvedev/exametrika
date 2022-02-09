/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.meters;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Strings;
import com.exametrika.spi.aggregator.common.meters.IStandardExpressionContext;

/**
 * The {@link StandardExpressionContext} is a standard context for expressions.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StandardExpressionContext implements IStandardExpressionContext {
    private static final MessageDigest digest = getMessageDigest();
    private static final Charset charset = Charset.forName("UTF8");
    private volatile HashMap<String, Pattern> filterPatterns = new HashMap<String, Pattern>();

    @Override
    public String hide(Object value) {
        if (value == null)
            return "";
        else
            return "##" + Strings.digestToString(digest.digest(value.toString().getBytes(charset)));
    }

    @Override
    public Object truncate(Object value, int length, boolean ellipsis) {
        if (value == null)
            return "";

        if (value instanceof IJsonCollection)
            return JsonUtils.truncate((IJsonCollection) value, length, ellipsis);
        else
            return Strings.truncate(value.toString(), length, ellipsis);
    }

    @Override
    public JsonObjectBuilder json(JsonObject value) {
        return new JsonObjectBuilder(value);
    }

    @Override
    public String getCurrentThread() {
        return Thread.currentThread().getName();
    }

    @Override
    public long getWallTime() {
        return System.nanoTime();
    }

    @Override
    public int count(Iterable list) {
        int count = 0;
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            it.next();
            count++;
        }

        return count;
    }

    @Override
    public boolean filter(String pattern, String value) {
        if (value == null)
            value = "";

        Pattern filterPattern = filterPatterns.get(pattern);
        if (filterPattern == null)
            filterPattern = addFilterPattern(pattern);

        return filterPattern.matcher(value).matches();
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return Exceptions.wrapAndThrow(e);
        }
    }

    private synchronized Pattern addFilterPattern(String pattern) {
        Pattern filterPattern = filterPatterns.get(pattern);
        if (filterPattern != null)
            return filterPattern;

        HashMap<String, Pattern> filterPatterns = (HashMap<String, Pattern>) this.filterPatterns.clone();
        filterPattern = Strings.createFilterPattern(pattern, false);
        filterPatterns.put(pattern, filterPattern);

        this.filterPatterns = filterPatterns;

        return filterPattern;
    }
}