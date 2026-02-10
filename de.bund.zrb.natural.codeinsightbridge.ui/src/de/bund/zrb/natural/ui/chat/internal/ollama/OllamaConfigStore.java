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
}

