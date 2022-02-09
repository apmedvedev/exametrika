/**
 * Copyright 2016 Andrey Medvedev. All rights reserved.
 */
package mapping;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RequestInequalityEstimator {
    private Map<String, Map<Long, Long>> requests = new HashMap<String, Map<Long, Long>>();
    private Set<Long> threads = new HashSet<Long>();
    private long lastTime = System.currentTimeMillis();

    public synchronized void addRequest(String name) {
        Map<Long, Long> requestThreads = requests.get(name);
        if (requestThreads == null) {
            requestThreads = new HashMap<Long, Long>();
            requests.put(name, requestThreads);
        }

        Long thread = Thread.currentThread().getId();
        Long count = requestThreads.get(thread);
        if (count == null)
            count = 0l;
        count++;
        requestThreads.put(thread, count);
        threads.add(thread);

        estimate();
    }

    private void estimate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTime < 300000)
            return;

        lastTime = currentTime;

        StringBuilder builder = new StringBuilder();
        builder.append("{{{{{{{{{{{{\n");
        for (Long thread : threads) {
            for (Map<Long, Long> requestThreads : requests.values()) {
                if (!requestThreads.containsKey(thread))
                    requestThreads.put(thread, 0L);
            }
        }

        for (Map<Long, Long> requestThreads : requests.values()) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            long total = 0, c = 0;
            for (long count : requestThreads.values()) {
                total += count;
                min = Math.min(min, count);
                max = Math.max(max, count);
                c++;
            }

            double avg = total / c;
            builder.append(MessageFormat.format("min: {0}, max: {1}, avg: {2}, histo: {3}", min, max, avg, requestThreads.values()) + "\n");
        }
        builder.append("}}}}}}}}}}}}}\n");

        System.out.println(builder.toString());
        requests.clear();
        threads.clear();
    }
}
