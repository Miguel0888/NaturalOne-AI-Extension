package de.bund.zrb.natural.ui.chat.internal.ollama;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Stores Ollama connection settings in Eclipse instance preferences.
 */
public final class OllamaConfigStore {

    // Keep aligned with ChatView preference node usage.
    private static final String PREF_NODE = "de.bund.zrb.natural.codeinsightbridge.ui";

    private static final String PREF_OLLAMA_BASE_URL = "chat.ollama.baseUrl";
    private static final String PREF_OLLAMA_MODEL = "chat.ollama.model";

    // Timeouts in milliseconds
    private static final String PREF_OLLAMA_CONNECT_TIMEOUT_MS = "chat.ollama.connectTimeoutMs";
    private static final String PREF_OLLAMA_READ_TIMEOUT_MS = "chat.ollama.readTimeoutMs";

    public String loadBaseUrl() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);
        String v = prefs.get(PREF_OLLAMA_BASE_URL, "http://localhost:11434");
        v = v == null ? "" : v.trim();
        return v.isEmpty() ? "http://localhost:11434" : v;
    }

    public String loadModel() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);
        String v = prefs.get(PREF_OLLAMA_MODEL, "llama3.1");
        v = v == null ? "" : v.trim();
        return v.isEmpty() ? "llama3.1" : v;
    }

    public int loadConnectTimeoutMs() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);
        return readInt(prefs, PREF_OLLAMA_CONNECT_TIMEOUT_MS, 2000, 0, 300000);
    }

    public int loadReadTimeoutMs() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(PREF_NODE);
        return readInt(prefs, PREF_OLLAMA_READ_TIMEOUT_MS, 60000, 0, 300000);
    }

    private static int readInt(IEclipsePreferences prefs, String key, int def, int min, int max) {
        if (prefs == null) {
            return def;
        }
        try {
            String raw = prefs.get(key, null);
            if (raw == null) {
                return def;
            }
            raw = raw.trim();
            if (raw.isEmpty()) {
                return def;
            }
            int v = Integer.parseInt(raw);
            if (v < min) {
                return min;
            }
            if (v > max) {
                return max;
            }
            return v;
        } catch (Exception ex) {
            return def;
        }
    }
}
