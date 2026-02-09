package de.bund.zrb.natural.tools.core;

public final class ToolUserDecision {

    private final boolean allowOnce;
    private final ToolPolicyDecision rememberAs;

    private ToolUserDecision(boolean allowOnce, ToolPolicyDecision rememberAs) {
        this.allowOnce = allowOnce;
        this.rememberAs = rememberAs;
    }

    public static ToolUserDecision allowOnce() {
        return new ToolUserDecision(true, null);
    }

    public static ToolUserDecision denyOnce() {
        return new ToolUserDecision(false, null);
    }

    public static ToolUserDecision allowAndRememberAlways() {
        return new ToolUserDecision(true, ToolPolicyDecision.ALWAYS);
    }

    public static ToolUserDecision denyAndRememberNever() {
        return new ToolUserDecision(false, ToolPolicyDecision.NEVER);
    }

    public boolean isAllowOnce() {
        return allowOnce;
    }

    public ToolPolicyDecision getRememberAs() {
        return rememberAs;
    }
}

