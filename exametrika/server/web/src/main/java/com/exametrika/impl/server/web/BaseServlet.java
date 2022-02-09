/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.server.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The {@link BaseServlet} is a base servlet.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class BaseServlet extends HttpServlet {
    protected ExaDbPrincipal getPrincipal(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ExaDbPrincipal principal = (ExaDbPrincipal) request.getUserPrincipal();
        if (!principal.getSession().isOpened()) {
            request.getSession().invalidate();
            response.sendRedirect("/");
            return null;
        }

        return principal;
    }
}
