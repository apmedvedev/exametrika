/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.web.api;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.common.utils.Files;
import com.exametrika.impl.server.web.BaseServlet;
import com.exametrika.impl.server.web.ExaDbPrincipal;

/**
 * The {@link SnapshotServlet} is a snapshot servlet.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SnapshotServlet extends BaseServlet {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ExaDbPrincipal principal = getPrincipal(request, response);
        if (principal == null)
            return;

        final IDatabase[] database = new IDatabase[1];
        principal.getSession().transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                database[0] = transaction.getDatabase();
            }
        });

        File snapshotPath = new File(System.getProperty("com.exametrika.workPath"), "snapshot");
        snapshotPath.mkdirs();
        Files.emptyDir(snapshotPath);

        database[0].getOperations().snapshot(snapshotPath.getPath(), null);
    }
}
