package log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLogClass1 {
    private static final Logger logger = LoggerFactory.getLogger(TestLogClass1.class);

    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 1000; i++) {
            run();

            Thread.sleep(1000);
        }
    }

    public static void run() {
        logger.trace("Test log trace record.");
        logger.debug("Test log debug record.");
        logger.info("Test log info record.");
        logger.warn("Test log warning record.");
        test(0);
    }

    private static void test(int level) {
        if (level == 10)
            logger.error("Test log error record.", new RuntimeException("Test exception."));
        else
            test(level + 1);
    }
}