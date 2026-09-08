package de.nebrel.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The client's single logger. */
public final class NebrelClientLog {

    private static final Logger LOGGER = LoggerFactory.getLogger("Nebrel Client");

    private NebrelClientLog() {
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message, Throwable error) {
        if (error == null) {
            LOGGER.warn(message);
        } else {
            LOGGER.warn(message, error);
        }
    }

    public static void error(String message, Throwable error) {
        if (error == null) {
            LOGGER.error(message);
        } else {
            LOGGER.error(message, error);
        }
    }
}
