/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package jdbc;

import java.io.FileReader;
import java.io.FileWriter;

import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;

public class Diff {
    public static void main(String[] args) throws Throwable {
        JsonObject first = JsonSerializers.read(new FileReader(args[0]), false);
        JsonObject second = JsonSerializers.read(new FileReader(args[1]), false);
        JsonDiff diff = new JsonDiff();
        JsonObject res = diff.diff(first, second);
        FileWriter writer = new FileWriter(args[2]);
        JsonSerializers.write(writer, res, true);
        writer.close();
    }
}
