/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.bridge;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exametrika.spi.metrics.jvm.boot.IHttpServletBridge;


/**
 * The {@link HttpServletBridge} represents an implementation of {@link IHttpServletBridge}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HttpServletBridge implements IHttpServletBridge {
    @Override
    public boolean supports(Object request) {
        return request instanceof HttpServletRequest || request instanceof HttpServletResponse;
    }

    @Override
    public String getContextName(Object httpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) httpServletRequest;
        return request.getServletContext().getServletContextName();
    }

    @Override
    public String getRequestURI(Object httpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) httpServletRequest;
        return request.getRequestURI();
    }

    @Override
    public String getQueryString(Object httpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) httpServletRequest;
        return request.getQueryString();
    }

    @Override
    public int getContentLength(Object httpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) httpServletRequest;
        return request.getContentLength();
    }

    @Override
    public String getTag(Object httpServletRequest) {
        HttpServletRequest request = (HttpServletRequest) httpServletRequest;
        return request.getHeader(EXA_TRACE_TAG);
    }

    @Override
    public void setRequestTime(Object httpServletResponse, long time) {
        HttpServletResponse response = (HttpServletResponse) httpServletResponse;
        response.setHeader(EXA_REQUEST_TIME, Long.toString(time));
    }
}
