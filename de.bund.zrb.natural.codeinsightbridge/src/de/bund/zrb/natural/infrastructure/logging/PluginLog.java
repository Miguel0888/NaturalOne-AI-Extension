package de.bund.zrb.natural.infrastructure.logging;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * Provide a minimal logging facade.
 */
public final class PluginLog {

    private static final String PLUGIN_ID = "de.bund.zrb.natural.codeinsightbridge";

    private PluginLog() {
        // Prevent instantiation
    }

    public static void info(String message) {
        log(Status.INFO, message, null);
    }

    public static void error(String message, Throwable throwable) {
        log(Status.ERROR, message, throwable);
    }

    private static void log(int severity, String message, Throwable throwable) {
        Bundle bundle = Platform.getBundle(PLUGIN_ID);
        ILog log = Platform.getLog(bundle);
        Status status = new Status(severity, PLUGIN_ID, message, throwable);
        log.log(status);
    }
}
