package de.bund.zrb.natural.tools.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolResult {

    private final ToolStatus status;
    private final String humanSummary;
    private final Map<String, Object> payload;
    private final String diagnostics;

    private ToolResult(ToolStatus status, String humanSummary, Map<String, Object> payload, String diagnostics) {
        this.status = status;
        this.humanSummary = humanSummary;
        this.payload = payload == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(payload));
        this.diagnostics = diagnostics;
    }

    public static ToolResult ok(String summary, Map<String, Object> payload) {
        return new ToolResult(ToolStatus.OK, summary, payload, null);
    }

    public static ToolResult denied(String summary) {
        return new ToolResult(ToolStatus.DENIED, summary, null, null);
    }

    public static ToolResult error(String summary, String diagnostics) {
        return new ToolResult(ToolStatus.ERROR, summary, null, diagnostics);
    }

    public ToolStatus getStatus() {
        return status;
    }

    public String getHumanSummary() {
        return humanSummary;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public String getDiagnostics() {
        return diagnostics;
    }
}

