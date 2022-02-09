/**
 * Copyright 2015 Andrey Medvedev. All rights reserved.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class HostStarterGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: <input_template> <output_file> <start_id> <entry_count> [<debug>]");
            return;
        }

        BufferedReader reader = new BufferedReader(new FileReader(new File(args[0])));
        StringBuilder builder = new StringBuilder();
        while (true) {
            String line = reader.readLine();
            if (line == null || line.startsWith("$JAVA "))
                break;

            builder.append(line);
            builder.append('\n');
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[1])));
        writer.write(builder.toString());

        int start = Integer.parseInt(args[2]);
        int count = Integer.parseInt(args[3]);

        for (int i = start; i < start + count; i++) {
            String debug = "";
            if (args.length >= 5 && args[4].equals("debug"))
                debug = "-Xdebug -Xrunjdwp:transport=dt_socket,address=" + Integer.toString(8000 + i) + ",server=y,suspend=n ";

            writer.write("$JAVA " + debug + "-Dcom.exametrika.home=$EXA_HOME -Dcom.exametrika.hostName=host" + Integer.toString(i)
                    + " -cp $EXA_HOME/lib/com.exametrika.boot-1.0.0.jar com.exametrika.impl.boot.Bootstrap $EXA_HOME/conf/exametrika-host.conf &\n");
        }

        writer.close();
    }
}
