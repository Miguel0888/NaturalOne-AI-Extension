package de.bund.zrb.natural.infrastructure.logging;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

public final class PluginLog {

    private static final String PLUGIN_ID = "de.bund.zrb.natural.codeinsightbridge";

    private PluginLog() {
        // Prevent instantiation
    }

    public static void info(String message) {
        log(IStatus.INFO, message, null);
    }

    public static void error(String message, Throwable t) {
        log(IStatus.ERROR, message, t);
    }

    private static void log(int severity, String message, Throwable t) {
        ILog log = Platform.getLog(Platform.getBundle(PLUGIN_ID));
        Status status = new Status(severity, PLUGIN_ID, message, t);
        log.log(status);
    }
}
