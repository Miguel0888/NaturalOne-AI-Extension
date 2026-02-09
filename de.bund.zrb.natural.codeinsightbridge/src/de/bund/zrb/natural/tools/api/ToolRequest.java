package de.bund.zrb.natural.tools.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolRequest {

    private final String toolId;
    private final Map<String, Object> arguments;
    private final ToolOrigin origin;
    private final String callId;

    public ToolRequest(String toolId, Map<String, Object> arguments, ToolOrigin origin, String callId) {
        this.toolId = toolId;
        this.arguments = arguments == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(arguments));
        this.origin = origin == null ? ToolOrigin.AI : origin;
        this.callId = callId;
    }

    public String getToolId() {
        return toolId;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public ToolOrigin getOrigin() {
        return origin;
    }

    public String getCallId() {
        return callId;
    }
}

