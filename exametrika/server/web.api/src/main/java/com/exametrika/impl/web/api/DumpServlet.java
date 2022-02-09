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
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Files;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.server.web.BaseServlet;
import com.exametrika.impl.server.web.ExaDbPrincipal;

/**
 * The {@link DumpServlet} is a dump servlet.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class DumpServlet extends BaseServlet {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ExaDbPrincipal principal = getPrincipal(request, response);
        if (principal == null)
            return;

        JsonObject query = JsonSerializers.read(request.getReader(), false);

        final IDatabase[] database = new IDatabase[1];
        principal.getSession().transactionSync(new Operation() {
            @Override
            public void run(ITransaction transaction) {
                database[0] = transaction.getDatabase();
            }
        });

        File dumpPath = new File(System.getProperty("com.exametrika.workPath"), "dump");
        dumpPath.mkdirs();
        Files.emptyDir(dumpPath);

        database[0].getOperations().dump(dumpPath.getPath(), new DumpContext(
                IDumpContext.DUMP_ORPHANED | IDumpContext.DUMP_TIMES | IDumpContext.DUMP_ID, query), null);
    }
}
